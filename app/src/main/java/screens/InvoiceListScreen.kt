package com.example.restaurantbookingapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

data class InvoiceItem(
    val id: String,
    val bookingId: String,
    val bookingCode: String,
    val guestName: String,
    val guestPhone: String,
    val tableSummary: String,
    val foodSubtotal: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val depositAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String,
    val paidAt: String,
    val note: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val invoiceList = remember { mutableStateListOf<InvoiceItem>() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var printingInvoiceId by remember { mutableStateOf<String?>(null) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    fun fetchInvoices() {
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null

            try {
                val url = URL("http://10.0.2.2:3001/api/invoices")
                conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("invoices")

                    val fetched = mutableListOf<InvoiceItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        fetched.add(
                            InvoiceItem(
                                id = obj.optString("id"),
                                bookingId = obj.optString("bookingId"),
                                bookingCode = obj.optString("bookingCode"),
                                guestName = obj.optString("guestName"),
                                guestPhone = obj.optString("guestPhone"),
                                tableSummary = obj.optString("tableSummary"),
                                foodSubtotal = obj.optDouble("foodSubtotal", 0.0),
                                discountPercent = obj.optDouble("discountPercent", 0.0),
                                discountAmount = obj.optDouble("discountAmount", 0.0),
                                depositAmount = obj.optDouble("depositAmount", 0.0),
                                totalAmount = obj.optDouble("totalAmount", 0.0),
                                paymentMethod = obj.optString("paymentMethod", "cash"),
                                paidAt = obj.optString("paidAt", ""),
                                note = obj.optString("note", "")
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        invoiceList.clear()
                        invoiceList.addAll(fetched)
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchInvoices()
    }

    val filteredInvoices = invoiceList.filter { invoice ->
        val matchSearch =
            invoice.guestName.contains(searchQuery, ignoreCase = true) ||
                    invoice.bookingCode.contains(searchQuery, ignoreCase = true) ||
                    invoice.guestPhone.contains(searchQuery)

        val invoiceDate = extractInvoiceDate(invoice.paidAt)

        val matchDate =
            selectedDate.isBlank() || invoiceDate == selectedDate.trim()

        matchSearch && matchDate
    }

    val totalRevenue = invoiceList.sumOf { it.totalAmount }

    val selectedDateRevenue = filteredInvoices.sumOf { it.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Lịch sử thanh toán",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Tổng doanh thu tất cả: ${currencyFormatter.format(totalRevenue)}",
                    fontSize = 14.sp,
                    color = Color(0xFF34C759),
                    fontWeight = FontWeight.Bold
                )

                if (selectedDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Doanh thu ngày $selectedDate: ${currencyFormatter.format(selectedDateRevenue)}",
                        fontSize = 14.sp,
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            placeholder = { Text("Tìm mã, tên khách, SĐT...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = selectedDate,
            onValueChange = { selectedDate = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text("Lọc doanh thu theo ngày") },
            placeholder = { Text("VD: 2026-05-27") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        if (selectedDate.isNotBlank()) {
            TextButton(
                onClick = { selectedDate = "" },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Xóa lọc ngày", color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else if (filteredInvoices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Chưa có hóa đơn nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredInvoices, key = { it.id }) { invoice ->
                    InvoiceCard(
                        invoice = invoice,
                        currencyFormatter = currencyFormatter,
                        isPrinting = printingInvoiceId == invoice.id,
                        onViewDetail = {
                            navController.navigate("InvoiceDetail/${invoice.id}")
                        },
                        onPrint = {
                            if (printingInvoiceId == null) {
                                printingInvoiceId = invoice.id
                                coroutineScope.launch {
                                    runCatching {
                                        loadInvoiceDetail(invoice.id)
                                    }.onSuccess { detail ->
                                        printPaymentInvoice(
                                            context,
                                            detail.summary,
                                            detail.paymentMethod
                                        )
                                    }.onFailure { error ->
                                        android.widget.Toast.makeText(
                                            context,
                                            "Không tải được hóa đơn: ${error.message}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    printingInvoiceId = null
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: InvoiceItem,
    currencyFormatter: NumberFormat,
    isPrinting: Boolean,
    onViewDetail: () -> Unit,
    onPrint: () -> Unit
) {
    val (pmLabel, pmColor) =
        if (invoice.paymentMethod == "cash") {
            "Tiền mặt" to Color(0xFF34C759)
        } else {
            "Chuyển khoản" to Color(0xFF007AFF)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = invoice.guestName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = invoice.bookingCode,
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SĐT: ${invoice.guestPhone}",
                color = Color.Gray,
                fontSize = 13.sp
            )

            Text(
                text = invoice.tableSummary,
                color = Color(0xFFFF9500),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "🕒 Thanh toán lúc: ${formatPaidAt(invoice.paidAt)}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            if (invoice.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Ghi chú: ${invoice.note}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (invoice.foodSubtotal > 0 || invoice.depositAmount > 0) {
                Text(
                    text = "Tiền món: ${currencyFormatter.format(invoice.foodSubtotal)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "Giảm hội viên (${invoice.discountPercent.toInt()}%): -${currencyFormatter.format(invoice.discountAmount)}",
                    color = Color(0xFF34C759),
                    fontSize = 12.sp
                )
                Text(
                    text = "Đã trừ cọc: -${currencyFormatter.format(invoice.depositAmount)}",
                    color = Color(0xFFFF9500),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = Color(0xFFE5E5EA))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = pmColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = pmLabel,
                        color = pmColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = currencyFormatter.format(invoice.totalAmount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34C759)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetail,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Chi tiết")
                }

                Button(
                    onClick = onPrint,
                    enabled = !isPrinting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    )
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("In hóa đơn")
                    }
                }
            }
        }
    }
}

fun extractInvoiceDate(paidAt: String): String {
    if (paidAt.isBlank()) return ""

    return when {
        paidAt.contains("T") -> paidAt.substringBefore("T")
        paidAt.contains(" ") -> paidAt.substringBefore(" ")
        paidAt.length >= 10 -> paidAt.take(10)
        else -> paidAt
    }
}

fun formatPaidAt(paidAt: String): String {
    if (paidAt.isBlank()) return "Chưa có thời gian"

    val date = extractInvoiceDate(paidAt)

    val time = when {
        paidAt.contains("T") ->
            paidAt.substringAfter("T")
                .substringBefore(".")
                .substringBefore("Z")
                .take(8)

        paidAt.contains(" ") ->
            paidAt.substringAfter(" ")
                .take(8)

        else -> ""
    }

    return if (time.isNotBlank()) {
        "$date $time"
    } else {
        date
    }
}
