package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController, bookingId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Dữ liệu booking được load từ API
    var bookingCode by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    var guestPhone by remember { mutableStateOf("") }
    var tableSummary by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }

    // Phương thức thanh toán
    var selectedPayment by remember { mutableStateOf("cash") } // "cash" hoặc "transfer"
    var note by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    // Tải thông tin booking
    LaunchedEffect(bookingId) {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3000/api/bookings")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("bookings")
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        if (obj.optString("id") == bookingId) {
                            withContext(Dispatchers.Main) {
                                bookingCode = obj.optString("bookingCode")
                                guestName = obj.optString("guestName")
                                guestPhone = obj.optString("guestPhone")
                                tableSummary = obj.optString("tableSummary")
                                totalAmount = obj.optDouble("totalAmount", 0.0)
                                isLoading = false
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // Hàm xử lý thanh toán
    fun processPayment() {
        isProcessing = true
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    val url = URL("http://10.0.2.2:3000/api/invoices")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json; utf-8")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true

                    val body = JSONObject().apply {
                        put("bookingId", bookingId)
                        put("bookingCode", bookingCode)
                        put("guestName", guestName)
                        put("guestPhone", guestPhone)
                        put("tableSummary", tableSummary)
                        put("totalAmount", totalAmount)
                        put("paymentMethod", selectedPayment)
                        put("note", note)
                    }.toString()

                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode

                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        if (code == HttpURLConnection.HTTP_CREATED) {
                            Toast.makeText(context, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "Lỗi thanh toán: $code", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thanh toán", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F9FA))
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HÓA ĐƠN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header hóa đơn
                        Text(
                            text = "EAT WHEN HUNGRY RESTAURANT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = "HÓA ĐƠN THANH TOÁN",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE5E5EA))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Thông tin hóa đơn
                        InvoiceRow(label = "Mã đặt bàn", value = bookingCode)
                        Spacer(modifier = Modifier.height(8.dp))
                        InvoiceRow(label = "Khách hàng", value = guestName)
                        Spacer(modifier = Modifier.height(8.dp))
                        InvoiceRow(label = "Số điện thoại", value = guestPhone)
                        Spacer(modifier = Modifier.height(8.dp))
                        InvoiceRow(label = "Bàn / Khu vực", value = tableSummary)

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Tổng tiền nổi bật
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TỔNG THANH TOÁN", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = currencyFormatter.format(totalAmount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                }

                // CHỌN PHƯƠNG THỨC THANH TOÁN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phương thức thanh toán", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Nút Tiền mặt
                        PaymentMethodButton(
                            label = "Tiền mặt",
                            icon = "💵",
                            description = "Khách thanh toán trực tiếp tại quầy",
                            isSelected = selectedPayment == "cash",
                            onClick = { selectedPayment = "cash" }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Nút Chuyển khoản
                        PaymentMethodButton(
                            label = "Chuyển khoản",
                            icon = "🏦",
                            description = "QR Code / Internet Banking",
                            isSelected = selectedPayment == "transfer",
                            onClick = { selectedPayment = "transfer" }
                        )

                        // Hiển thị thông tin QR nếu chọn chuyển khoản
                        if (selectedPayment == "transfer") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE5F1FF)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Thông tin chuyển khoản:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ngân hàng: Vietcombank", fontSize = 13.sp)
                                    Text("STK: 1234567890", fontSize = 13.sp)
                                    Text("Tên TK: NHA HANG RESTAURANT", fontSize = 13.sp)
                                    Text("Nội dung: $bookingCode - $guestName", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // GHI CHÚ
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú hóa đơn") },
                    placeholder = { Text("VD: Khách dùng voucher...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )

                // NÚT XÁC NHẬN THANH TOÁN
                Button(
                    onClick = { processPayment() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Xác nhận thanh toán ${if (selectedPayment == "cash") "tiền mặt" else "chuyển khoản"}",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Composable hàng thông tin hóa đơn
@Composable
fun InvoiceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}

// Composable nút chọn phương thức thanh toán
@Composable
fun PaymentMethodButton(
    label: String,
    icon: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5EA)
    val bgColor = if (isSelected) Color(0xFFE5F1FF) else Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = description, fontSize = 12.sp, color = Color.Gray)
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF))
            )
        }
    }
}