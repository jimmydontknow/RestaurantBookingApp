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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookingScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- QUẢN LÝ TRẠNG THÁI NHẬP LIỆU ---
    var guestName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var tableNumber by remember { mutableStateOf("") } // Chỉ nhận chuỗi số
    var depositAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // --- LAMBDA CỤC BỘ GỬI DỮ LIỆU ĐẶT BÀN ---
    val createBookingLambda = remember {
        { name: String, phone: String, table: String, deposit: String, noteText: String ->
            suspend {
                withContext(Dispatchers.IO) {
                    var conn: HttpURLConnection? = null
                    try {
                        // Gọi API sử dụng endpoint chuẩn có chữ 's'
                        val url = URL("http://10.0.2.2:3000/api/bookings")
                        conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json; utf-8")
                        conn.setRequestProperty("Accept", "application/json")
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.doOutput = true

                        val jsonInputString = JSONObject().apply {
                            put("guestName", name)
                            put("phoneNumber", phone)
                            put("tableNumber", table) // Gửi số (ví dụ: "1") lên server
                            put("depositAmount", deposit.toIntOrNull() ?: 0)
                            put("note", noteText)
                        }.toString()

                        conn.outputStream.use { os ->
                            val input = jsonInputString.toByteArray(charset("utf-8"))
                            os.write(input, 0, input.size)
                        }

                        val responseCode = conn.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                            Pair(true, "Thành công")
                        } else {
                            val errorStream = conn.errorStream
                            val errorMessage = if (errorStream != null) {
                                BufferedReader(InputStreamReader(errorStream, "utf-8")).use { it.readText() }
                            } else {
                                "Mã lỗi từ Server: $responseCode"
                            }
                            // Trích xuất thông báo lỗi thực tế từ JSON nếu cấu trúc trả về là JSON
                            val cleanMsg = try {
                                JSONObject(errorMessage).getString("message")
                            } catch (e: Exception) {
                                errorMessage
                            }
                            Pair(false, cleanMsg)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Pair(false, "Lỗi kết nối Server: ${e.localizedMessage}")
                    } finally {
                        conn?.disconnect()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Tạo đặt bàn mới", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
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
                label = { Text("Tên khách hàng") }, placeholder = { Text("Nhập tên khách hàng...") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )

            // 2. Số điện thoại
            OutlinedTextField(
                value = phoneNumber, onValueChange = { phoneNumber = it },
                label = { Text("Số điện thoại") }, placeholder = { Text("Nhập số điện thoại...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )

            // 3. SỬA ĐỔI: Ô nhập số bàn (Chỉ điền số)
            OutlinedTextField(
                value = tableNumber,
                onValueChange = { input ->
                    // Bộ lọc chỉ cho phép nhập ký tự số
                    if (input.all { it.isDigit() }) tableNumber = input
                },
                label = { Text("Số bàn (chỉ cần điền số)") },
                placeholder = { Text("Ví dụ: 1 hoặc 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Hiện bàn phím số
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )

            // 4. Số tiền đặt cọc
            OutlinedTextField(
                value = depositAmount, onValueChange = { depositAmount = it },
                label = { Text("Số tiền đặt cọc (VND)") }, placeholder = { Text("Ví dụ: 500000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )

            // 5. Ghi chú bổ sung
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Ghi chú bổ sung") }, placeholder = { Text("Yêu cầu đặc biệt của khách hàng...") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // NÚT HÀNH ĐỘNG XÁC NHẬN
            Button(
                onClick = {
                    if (guestName.trim().isEmpty() || phoneNumber.trim().isEmpty() || tableNumber.trim().isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ Tên, SĐT và Số bàn!", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch {
                            val (isSuccess, serverMessage) = createBookingLambda(
                                guestName, phoneNumber, tableNumber, depositAmount.ifEmpty { "0" }, note
                            ).invoke()

                            if (isSuccess) {
                                Toast.makeText(context, "Đặt bàn thành công cho khách $guestName!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                // Hiện trực tiếp nguyên nhân lỗi cụ thể từ server trả về lên màn hình
                                Toast.makeText(context, "Lỗi: $serverMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("Xác nhận tạo đặt bàn", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}