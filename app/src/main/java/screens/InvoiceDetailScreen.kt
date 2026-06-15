package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

data class InvoiceDetail(
    val summary: PaymentSummary,
    val paymentMethod: String,
    val note: String
)

suspend fun loadInvoiceDetail(invoiceId: String): InvoiceDetail =
    withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL("http://10.0.2.2:3001/api/invoices/$invoiceId")
                .openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 7000
            connection.readTimeout = 7000

            val code = connection.responseCode
            val response = (if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            check(code == HttpURLConnection.HTTP_OK) {
                runCatching { JSONObject(response).optString("message") }
                    .getOrDefault("Lỗi tải hóa đơn: $code")
            }

            val invoice = JSONObject(response).getJSONObject("invoice")
            InvoiceDetail(
                summary = parsePaymentSummary(invoice),
                paymentMethod = invoice.optString("paymentMethod", "cash"),
                note = invoice.optString("note", "")
            )
        } finally {
            connection?.disconnect()
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    navController: NavController,
    invoiceId: String
) {
    val context = LocalContext.current
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    }
    var detail by remember { mutableStateOf<InvoiceDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(invoiceId) {
        runCatching { loadInvoiceDetail(invoiceId) }
            .onSuccess {
                detail = it
                errorMessage = ""
            }
            .onFailure {
                errorMessage = it.message ?: "Không tải được hóa đơn"
            }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết hóa đơn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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

            detail == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage, color = Color.Red)
                }
            }

            else -> {
                val invoice = detail!!
                val bill = invoice.summary
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFF8F9FA)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "HÓA ĐƠN #${bill.invoiceId}",
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(12.dp))
                                InvoiceRow("Mã đặt bàn", bill.bookingCode)
                                InvoiceRow("Khách hàng", bill.guestName)
                                InvoiceRow("Số điện thoại", bill.guestPhone)
                                InvoiceRow("Bàn / Khu vực", bill.tableSummary)
                                InvoiceRow(
                                    "Phương thức",
                                    if (invoice.paymentMethod == "cash") {
                                        "Tiền mặt"
                                    } else {
                                        "Chuyển khoản"
                                    }
                                )
                                InvoiceRow("Thanh toán lúc", formatPaidAt(bill.paidAt))
                            }
                        }
                    }

                    item {
                        Text("Món đã gọi", fontWeight = FontWeight.Bold)
                    }

                    if (bill.items.isEmpty()) {
                        item { Text("Hóa đơn không có món ăn.", color = Color.Gray) }
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
                                            "${food.quantity} x ${formatter.format(food.unitPrice)}",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        formatter.format(food.lineTotal),
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
                            Column(Modifier.padding(16.dp)) {
                                InvoiceRow("Tiền món ăn", formatter.format(bill.foodSubtotal))
                                InvoiceRow(
                                    "Giảm hội viên (${bill.discountPercent.toInt()}%)",
                                    "-${formatter.format(bill.discountAmount)}"
                                )
                                InvoiceRow(
                                    "Đã trừ tiền cọc",
                                    "-${formatter.format(bill.depositAmount)}"
                                )
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TỔNG THANH TOÁN", fontWeight = FontWeight.Bold)
                                    Text(
                                        formatter.format(bill.amountDue),
                                        color = Color(0xFF34C759),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                if (invoice.note.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Ghi chú: ${invoice.note}", color = Color.Gray)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                runCatching {
                                    printPaymentInvoice(
                                        context,
                                        bill,
                                        invoice.paymentMethod
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "Không thể mở trình in: ${it.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("In hóa đơn", fontWeight = FontWeight.Bold)
                        }
                    }

                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
    }
}
