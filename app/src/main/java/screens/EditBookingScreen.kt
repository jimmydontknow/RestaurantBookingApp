package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookingScreen(navController: NavController, bookingId: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // --- TRẠNG THÁI CÁC Ô NHẬP LIỆU ---
    var guestName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var tableNumber by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("200000") }
    var note by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("pending") }
    var isLoading by remember { mutableStateOf(true) }

    // Danh sách trạng thái có thể chọn
    val statusOptions = listOf(
        "pending" to "Chờ xếp bàn",
        "checked_in" to "Đang phục vụ",
        "checked_out" to "Đã rời đi"
    )
    var expandedDropdown by remember { mutableStateOf(false) }

    // --- TẢI THÔNG TIN BOOKING HIỆN TẠI TỪ API ---
    LaunchedEffect(bookingId) {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3001/api/bookings")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("bookings")

                    // Tìm đúng booking theo ID để điền sẵn dữ liệu vào form
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        if (item.getString("id") == bookingId) {
                            val tableSummary = item.getString("tableSummary") // Ví dụ: "Bàn 2"
                            val tableNum = tableSummary.replace("Bàn ", "").trim()
                            withContext(Dispatchers.Main) {
                                guestName = item.getString("guestName")
                                phoneNumber = item.getString("guestPhone")
                                tableNumber = tableNum
                                depositAmount = "200000"
                                selectedStatus = item.getString("status")
                                isLoading = false
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi tải dữ liệu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Chỉnh sửa đặt bàn", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                },
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                // 1. Tên khách hàng
                OutlinedTextField(
                    value = guestName, onValueChange = { guestName = it },
                    label = { Text("Tên khách hàng") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )

                // 2. Số điện thoại
                OutlinedTextField(
                    value = phoneNumber, onValueChange = { phoneNumber = it },
                    label = { Text("Số điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )

                // 3. Số bàn (chỉ nhận ký tự số)
                OutlinedTextField(
                    value = tableNumber,
                    onValueChange = { if (it.all { c -> c.isDigit() }) tableNumber = it },
                    label = { Text("Số bàn") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )

                // 4. Tiền cọc
                OutlinedTextField(
                    value = depositAmount, onValueChange = {}, readOnly = true,
                    label = { Text("Tiền đặt cọc cố định (VND)") },
                    supportingText = { Text("Tiền cọc sẽ được trừ khi thanh toán hóa đơn.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )

                // 5. Dropdown chọn trạng thái
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = statusOptions.first { it.first == selectedStatus }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trạng thái") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        statusOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedStatus = value
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // 6. Ghi chú
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Ghi chú bổ sung") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NÚT LƯU CHỈNH SỬA
                Button(
                    onClick = { /* code cũ giữ nguyên */ },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

// ĐÃ THÊM: NÚT HỦY ĐẶT BÀN - Chuyển trạng thái sang "cancelled" và quay về
                var showCancelDialog by remember { mutableStateOf(false) }

                Button(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                ) {
                    Text("Hủy đặt bàn", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

// ĐÃ THÊM: DIALOG XÁC NHẬN HỦY - tránh bấm nhầm
                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        title = { Text("Xác nhận hủy đặt bàn", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        text = { Text("Bạn có chắc muốn hủy đặt bàn của khách \"$guestName\" không?", fontSize = 14.sp) },
                        confirmButton = {
                            TextButton(onClick = {
                                showCancelDialog = false
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        var conn: HttpURLConnection? = null
                                        try {
                                            // Gọi PUT cập nhật trạng thái sang "cancelled"
                                            val url = URL("http://10.0.2.2:3001/api/bookings/$bookingId")
                                            conn = url.openConnection() as HttpURLConnection
                                            conn.requestMethod = "PUT"
                                            conn.setRequestProperty("Content-Type", "application/json; utf-8")
                                            conn.connectTimeout = 5000
                                            conn.readTimeout = 5000
                                            conn.doOutput = true

                                            val body = JSONObject().apply {
                                                put("guestName", guestName)
                                                put("phoneNumber", phoneNumber)
                                                put("tableNumber", tableNumber)
                                                put("depositAmount", depositAmount.toIntOrNull() ?: 0)
                                                put("note", note)
                                                put("status", "cancelled") // Đánh dấu đã hủy
                                            }.toString()

                                            conn.outputStream.use { os ->
                                                os.write(body.toByteArray(charset("utf-8")))
                                            }

                                            val responseCode = conn.responseCode
                                            withContext(Dispatchers.Main) {
                                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                                    Toast.makeText(context, "Đã hủy đặt bàn của $guestName!", Toast.LENGTH_SHORT).show()
                                                    navController.popBackStack()
                                                } else {
                                                    Toast.makeText(context, "Lỗi hủy: mã $responseCode", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        } finally {
                                            conn?.disconnect()
                                        }
                                    }
                                }
                            }) {
                                Text("Xác nhận hủy", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) {
                                Text("Giữ lại", color = Color.Gray)
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                    Text("Lưu thay đổi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
