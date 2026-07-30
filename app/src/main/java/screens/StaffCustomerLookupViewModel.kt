package com.example.restaurantbookingapp.screens

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.concurrent.Executors

data class CustomerLookupResult(
    val name: String,
    val phone: String,
    val visitCount: Int,
    val totalSpent: Double,
    val discountPercent: Double,
    val invoices: List<CustomerInvoiceResult> = emptyList()
)

data class CustomerInvoiceResult(
    val id: String,
    val bookingCode: String,
    val tableSummary: String,
    val foodSubtotal: Double,
    val discountAmount: Double,
    val depositAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String,
    val paidAt: String,
    val note: String
)

class StaffCustomerLookupViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var query by mutableStateOf(
        savedStateHandle[KEY_QUERY] ?: preferences.getString(KEY_QUERY, "").orEmpty()
    )
        private set
    var isLoading by mutableStateOf(false)
        private set
    var hasSearched by mutableStateOf(
        savedStateHandle[KEY_HAS_SEARCHED] ?: preferences.getBoolean(KEY_HAS_SEARCHED, false)
    )
        private set
    var results by mutableStateOf(
        parseResults(
            savedStateHandle[KEY_RESULTS]
                ?: preferences.getString(KEY_RESULTS, "").orEmpty()
        )
    )
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun updateQuery(value: String) {
        query = value
        savedStateHandle[KEY_QUERY] = value
        preferences.edit().putString(KEY_QUERY, value).apply()
    }

    fun lookup() {
        val keyword = query.trim()
        if (keyword.isBlank()) {
            message = "Vui lòng nhập tên hoặc số điện thoại"
            return
        }

        isLoading = true
        executor.execute {
            var connection: HttpURLConnection? = null
            runCatching {
                val encoded = URLEncoder.encode(keyword, "UTF-8")
                connection = URL("http://10.0.2.2:3001/api/customers/lookup?q=$encoded")
                    .openConnection() as HttpURLConnection
                connection?.apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val code = connection?.responseCode ?: 500
                val body = (if (code in 200..299) connection?.inputStream else connection?.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    error(runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
                        .ifBlank { "Không tra cứu được khách hàng" })
                }
                body
            }.onSuccess { body ->
                mainHandler.post {
                    val parsed = parseResponse(body)
                    results = parsed
                    hasSearched = true
                    isLoading = false
                    savedStateHandle[KEY_RESULTS] = resultsToJson(parsed)
                    savedStateHandle[KEY_HAS_SEARCHED] = true
                    preferences.edit()
                        .putString(KEY_RESULTS, resultsToJson(parsed))
                        .putBoolean(KEY_HAS_SEARCHED, true)
                        .apply()
                }
            }.onFailure { error ->
                mainHandler.post {
                    isLoading = false
                    message = error.localizedMessage ?: "Không tra cứu được khách hàng"
                }
            }
            connection?.disconnect()
        }
    }

    fun consumeMessage() {
        message = null
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }

    private fun parseResponse(body: String): List<CustomerLookupResult> {
        val root = JSONObject(body)
        val customers = root.optJSONArray("customers") ?: JSONArray()
        val topLevelInvoices = root.optJSONArray("invoices") ?: JSONArray()
        return List(customers.length()) { index ->
            val customer = customers.getJSONObject(index).toResult()
            if (customer.invoices.isEmpty() && customers.length() == 1 && topLevelInvoices.length() > 0) {
                customer.copy(invoices = topLevelInvoices.toInvoiceList())
            } else {
                customer
            }
        }
    }

    private fun JSONObject.toResult() = CustomerLookupResult(
        name = optString("name"),
        phone = optString("phone"),
        visitCount = optInt("visitCount"),
        totalSpent = optDouble("totalSpent"),
        discountPercent = optDouble("discountPercent"),
        invoices = (optJSONArray("invoices") ?: JSONArray()).toInvoiceList()
    )

    private fun JSONArray.toInvoiceList(): List<CustomerInvoiceResult> =
        List(length()) { index ->
            optJSONObject(index)?.toInvoiceResult() ?: CustomerInvoiceResult(
                id = "",
                bookingCode = "",
                tableSummary = "",
                foodSubtotal = 0.0,
                discountAmount = 0.0,
                depositAmount = 0.0,
                totalAmount = 0.0,
                paymentMethod = "",
                paidAt = "",
                note = ""
            )
        }

    private fun JSONObject.toInvoiceResult() = CustomerInvoiceResult(
        id = optString("id").ifBlank { optString("invoiceId") },
        bookingCode = optString("bookingCode"),
        tableSummary = optString("tableSummary"),
        foodSubtotal = optDouble("foodSubtotal"),
        discountAmount = optDouble("discountAmount"),
        depositAmount = optDouble("depositAmount"),
        totalAmount = optDouble("totalAmount").let { total ->
            if (total > 0.0) total else optDouble("amountDue")
        },
        paymentMethod = optString("paymentMethod"),
        paidAt = optString("paidAt"),
        note = optString("note")
    )

    private fun resultsToJson(items: List<CustomerLookupResult>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("name", item.name)
                put("phone", item.phone)
                put("visitCount", item.visitCount)
                put("totalSpent", item.totalSpent)
                put("discountPercent", item.discountPercent)
                put("invoices", JSONArray().apply {
                    item.invoices.forEach { invoice ->
                        put(JSONObject().apply {
                            put("id", invoice.id)
                            put("bookingCode", invoice.bookingCode)
                            put("tableSummary", invoice.tableSummary)
                            put("foodSubtotal", invoice.foodSubtotal)
                            put("discountAmount", invoice.discountAmount)
                            put("depositAmount", invoice.depositAmount)
                            put("totalAmount", invoice.totalAmount)
                            put("paymentMethod", invoice.paymentMethod)
                            put("paidAt", invoice.paidAt)
                            put("note", invoice.note)
                        })
                    }
                })
            })
        }
    }.toString()

    private fun parseResults(raw: String): List<CustomerLookupResult> = runCatching {
        val array = JSONArray(raw)
        List(array.length()) { index -> array.getJSONObject(index).toResult() }
    }.getOrDefault(emptyList())

    companion object {
        private const val PREFERENCES_NAME = "staff_customer_lookup"
        private const val KEY_QUERY = "customer_lookup_query"
        private const val KEY_RESULTS = "customer_lookup_results"
        private const val KEY_HAS_SEARCHED = "customer_lookup_has_searched"
    }
}
