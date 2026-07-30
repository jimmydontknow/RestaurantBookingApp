package com.example.restaurantbookingapp.screens

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
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

data class PaymentFoodItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

data class PaymentSummary(
    val bookingId: Int,
    val bookingCode: String,
    val guestName: String,
    val guestPhone: String,
    val tableSummary: String,
    val items: List<PaymentFoodItem>,
    val foodSubtotal: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val depositAmount: Double,
    val amountDue: Double,
    val invoiceId: String = "",
    val paidAt: String = ""
)

internal fun parsePaymentSummary(obj: JSONObject): PaymentSummary {
    val itemArray = obj.optJSONArray("items")
    val foods = mutableListOf<PaymentFoodItem>()
    if (itemArray != null) {
        for (index in 0 until itemArray.length()) {
            val item = itemArray.getJSONObject(index)
            foods.add(
                PaymentFoodItem(
                    name = item.optString("foodName", "Món ăn"),
                    quantity = item.optInt("quantity", 0),
                    unitPrice = item.optDouble("unitPrice", 0.0),
                    lineTotal = item.optDouble("lineTotal", 0.0)
                )
            )
        }
    }
    return PaymentSummary(
        bookingId = obj.optInt("bookingId", 0),
        bookingCode = obj.optString("bookingCode"),
        guestName = obj.optString("guestName"),
        guestPhone = obj.optString("guestPhone"),
        tableSummary = obj.optString("tableSummary"),
        items = foods,
        foodSubtotal = obj.optDouble("foodSubtotal", 0.0),
        discountPercent = obj.optDouble("discountPercent", 0.0),
        discountAmount = obj.optDouble("discountAmount", 0.0),
        depositAmount = obj.optDouble("depositAmount", 200000.0),
        amountDue = obj.optDouble("amountDue", 0.0),
        invoiceId = obj.optString("invoiceId"),
        paidAt = obj.optString("paidAt")
    )
}

private fun htmlEscape(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")

internal fun printPaymentInvoice(
    context: Context,
    summary: PaymentSummary,
    paymentMethod: String
) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val itemRows = summary.items.joinToString("") { item ->
        """
        <tr>
          <td>${htmlEscape(item.name)}</td>
          <td class="center">${item.quantity}</td>
          <td class="money">${formatter.format(item.unitPrice)}</td>
          <td class="money">${formatter.format(item.lineTotal)}</td>
        </tr>
        """.trimIndent()
    }
    val html = """
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8">
          <style>
            body { font-family: sans-serif; color: #111; padding: 24px; }
            h1, h2, p { text-align: center; margin: 4px 0; }
            h1 { font-size: 22px; }
            h2 { font-size: 16px; }
            .meta { margin: 22px 0; line-height: 1.7; }
            table { width: 100%; border-collapse: collapse; margin-top: 18px; }
            th, td { border-bottom: 1px solid #ddd; padding: 9px 5px; }
            th { text-align: left; }
            .center { text-align: center; }
            .money { text-align: right; white-space: nowrap; }
            .summary { margin-top: 18px; margin-left: auto; width: 62%; }
            .summary div { display: flex; justify-content: space-between; padding: 5px 0; }
            .total { font-size: 18px; font-weight: bold; border-top: 2px solid #111; margin-top: 8px; padding-top: 10px !important; }
            .thanks { margin-top: 32px; }
          </style>
        </head>
        <body>
          <h1>EAT WHEN HUNGRY RESTAURANT</h1>
          <h2>HÓA ĐƠN THANH TOÁN</h2>
          <p>${if (summary.invoiceId.isBlank()) "Bản xem trước" else "Mã hóa đơn: #${htmlEscape(summary.invoiceId)}"}</p>
          <div class="meta">
            <div><b>Mã đặt bàn:</b> ${htmlEscape(summary.bookingCode)}</div>
            <div><b>Khách hàng:</b> ${htmlEscape(summary.guestName)}</div>
            <div><b>Số điện thoại:</b> ${htmlEscape(summary.guestPhone)}</div>
            <div><b>Bàn / Khu vực:</b> ${htmlEscape(summary.tableSummary)}</div>
            <div><b>Thanh toán:</b> ${if (paymentMethod == "cash") "Tiền mặt" else "Chuyển khoản"}</div>
          </div>
          <table>
            <thead><tr><th>Món ăn</th><th class="center">SL</th><th class="money">Đơn giá</th><th class="money">Thành tiền</th></tr></thead>
            <tbody>$itemRows</tbody>
          </table>
          <div class="summary">
            <div><span>Tiền món:</span><b>${formatter.format(summary.foodSubtotal)}</b></div>
            <div><span>Giảm hội viên (${summary.discountPercent.toInt()}%):</span><b>-${formatter.format(summary.discountAmount)}</b></div>
            <div><span>Tiền cọc:</span><b>-${formatter.format(summary.depositAmount)}</b></div>
            <div class="total"><span>Còn thanh toán:</span><span>${formatter.format(summary.amountDue)}</span></div>
          </div>
          <p class="thanks">Cảm ơn quý khách và hẹn gặp lại!</p>
        </body>
        </html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val adapter = view.createPrintDocumentAdapter(
                "HoaDon_${summary.bookingCode}"
            )
            printManager.print(
                "Hóa đơn ${summary.bookingCode}",
                adapter,
                PrintAttributes.Builder().build()
            )
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController, bookingId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    }

    var summary by remember { mutableStateOf<PaymentSummary?>(null) }
    var selectedPayment by remember { mutableStateOf("cash") }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) }

    suspend fun loadSummary() = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL(
                "http://10.0.2.2:3001/api/bookings/$bookingId/payment-summary"
            ).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val code = conn.responseCode
            val response = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code == HttpURLConnection.HTTP_OK) {
                val loaded = parsePaymentSummary(
                    JSONObject(response).getJSONObject("summary")
                )
                withContext(Dispatchers.Main) {
                    summary = loaded
                    errorMessage = ""
                    isLoading = false
                }
            } else {
                val message = runCatching {
                    JSONObject(response).optString("message")
                }.getOrDefault("Không tải được hóa đơn")
                withContext(Dispatchers.Main) {
                    errorMessage = message
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                errorMessage = "Không kết nối được máy chủ: ${e.localizedMessage}"
                isLoading = false
            }
        } finally {
            conn?.disconnect()
        }
    }

    fun processPayment() {
        val currentSummary = summary ?: return
        isProcessing = true
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = URL("http://10.0.2.2:3001/api/invoices")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.connectTimeout = 7000
                conn.readTimeout = 7000
                conn.doOutput = true
                val body = JSONObject().apply {
                    put("bookingId", currentSummary.bookingId)
                    put("paymentMethod", selectedPayment)
                    put("note", note)
                }.toString()
                conn.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                val response = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                if (
                    code == HttpURLConnection.HTTP_CREATED ||
                    code == HttpURLConnection.HTTP_OK
                ) {
                    val paidSummary = parsePaymentSummary(
                        JSONObject(response).getJSONObject("invoice")
                    )
                    withContext(Dispatchers.Main) {
                        summary = paidSummary
                        isPaid = true
                        isProcessing = false
                        val alreadyPaid = JSONObject(response)
                            .optBoolean("alreadyPaid", false)
                        Toast.makeText(
                            context,
                            if (alreadyPaid)
                                "Đơn đã thanh toán, trạng thái đã được đồng bộ."
                            else
                                "Thanh toán thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val message = runCatching {
                        JSONObject(response).optString(
                            "message",
                            "Lỗi thanh toán: $code"
                        )
                    }.getOrDefault("Lỗi thanh toán: $code")
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    Toast.makeText(
                        context,
                        "Lỗi thanh toán: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    LaunchedEffect(bookingId) {
        loadSummary()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thanh toán",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            summary == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(errorMessage, color = Color.Red)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        isLoading = true
                        coroutineScope.launch { loadSummary() }
                    }) {
                        Text("Tải lại")
                    }
                }
            }

            else -> {
                val bill = summary!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFF8F9FA)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text(
                                    "EAT WHEN HUNGRY RESTAURANT",
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    if (isPaid) "HÓA ĐƠN ĐÃ THANH TOÁN" else "HÓA ĐƠN THANH TOÁN",
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(16.dp))
                                InvoiceRow("Mã đặt bàn", bill.bookingCode)
                                InvoiceRow("Khách hàng", bill.guestName)
                                InvoiceRow("Số điện thoại", bill.guestPhone)
                                InvoiceRow("Bàn / Khu vực", bill.tableSummary)
                            }
                        }
                    }

                    item {
                        Text("Món đã gọi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (bill.items.isEmpty()) {
                        item {
                            Text("Chưa có món ăn nào.", color = Color.Gray)
                        }
                    } else {
                        items(bill.items) { food ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(food.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${food.quantity} x ${currencyFormatter.format(food.unitPrice)}",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        currencyFormatter.format(food.lineTotal),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                InvoiceRow("Tiền món ăn", currencyFormatter.format(bill.foodSubtotal))
                                Spacer(Modifier.height(8.dp))
                                InvoiceRow(
                                    "Giảm khách thân thiết (${bill.discountPercent.toInt()}%)",
                                    "-${currencyFormatter.format(bill.discountAmount)}"
                                )
                                Spacer(Modifier.height(8.dp))
                                InvoiceRow(
                                    "Đã trừ tiền cọc",
                                    "-${currencyFormatter.format(bill.depositAmount)}"
                                )
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("CÒN THANH TOÁN", fontWeight = FontWeight.Bold)
                                    Text(
                                        currencyFormatter.format(bill.amountDue),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34C759)
                                    )
                                }
                            }
                        }
                    }

                    if (!isPaid) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Phương thức thanh toán", fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedPayment == "cash",
                                            onClick = { selectedPayment = "cash" }
                                        )
                                        Text("Tiền mặt")
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedPayment == "transfer",
                                            onClick = { selectedPayment = "transfer" }
                                        )
                                        Text("Chuyển khoản")
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Ghi chú hóa đơn") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }

                        item {
                            Button(
                                onClick = { processPayment() },
                                enabled = !isProcessing,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF34C759)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                printPaymentInvoice(context, bill, selectedPayment)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isPaid) "In hóa đơn" else "In bản xem trước")
                        }
                    }

                    item {
                        Spacer(Modifier.height(70.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

