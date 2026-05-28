package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsListScreen(navController: NavController) {
    val bookingList = remember { mutableStateListOf<BookingItem>() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Trạng thái dialog tùy chọn (nhấn giữ card)
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedBookingForAction by remember { mutableStateOf<BookingItem?>(null) }

    // --- HÀM LẤY DANH SÁCH ĐẶT BÀN ---
    fun fetchBookings() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:3000/api/bookings")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("bookings")
                    val fetched = mutableListOf<BookingItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        fetched.add(
                            BookingItem(
                                id = obj.optString("id"),
                                bookingCode = obj.optString("bookingCode"),
                                guestName = obj.optString("guestName"),
                                guestPhone = obj.optString("guestPhone"),
                                tableSummary = obj.optString("tableSummary"),
                                totalAmount = obj.optDouble("totalAmount", 0.0),
                                status = obj.optString("status", "pending"),

                                // THÊM
                                bookingDate = obj.optString("bookingDate", ""),
                                bookingTime = obj.optString("bookingTime", "")
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        bookingList.clear()
                        bookingList.addAll(fetched)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- HÀM CẬP NHẬT TRẠNG THÁI ĐƠN + BÀN LIÊN QUAN ---
    fun updateBookingStatus(booking: BookingItem, newStatus: String) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    // Bước 1: Cập nhật trạng thái booking
                    val url = URL("http://10.0.2.2:3000/api/bookings/status")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "PUT"
                    conn.setRequestProperty("Content-Type", "application/json; utf-8")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true

                    val body = JSONObject().apply {
                        put("id", booking.id.toIntOrNull() ?: booking.id)
                        put("status", newStatus)
                    }.toString()
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    conn.disconnect()

                    if (code == HttpURLConnection.HTTP_OK) {
                        // Bước 2: Cập nhật bàn liên quan nếu cần
                        if (newStatus == "checked_in" || newStatus == "checked_out") {
                            val tableStatus = if (newStatus == "checked_in") "occupied" else "available"

                            // ĐÃ SỬA: Gọi trực tiếp logic cập nhật bàn thay vì gọi hàm suspend riêng
                            try {
                                val urlTables = URL("http://10.0.2.2:3000/api/tables")
                                val connTables = urlTables.openConnection() as HttpURLConnection
                                connTables.requestMethod = "GET"
                                connTables.connectTimeout = 5000
                                connTables.readTimeout = 5000

                                if (connTables.responseCode == HttpURLConnection.HTTP_OK) {
                                    val tablesStr = connTables.inputStream.bufferedReader().use { it.readText() }
                                    val tablesArray = JSONObject(tablesStr).getJSONArray("tables")

                                    // Tìm mã bàn trong chuỗi tableSummary, ví dụ: "A01", "B02"
                                    val tableNumberPattern = Regex("[AB]\\d{2}")
                                    val matchedNumbers = tableNumberPattern.findAll(booking.tableSummary)
                                        .map { it.value }.toSet()

                                    for (i in 0 until tablesArray.length()) {
                                        val t = tablesArray.getJSONObject(i)
                                        val tNum = t.optString("tableNumber", "")
                                        if (matchedNumbers.contains(tNum)) {
                                            val tableId = t.getString("id")
                                            var connUpdate: HttpURLConnection? = null
                                            try {
                                                val urlUpdate = URL("http://10.0.2.2:3000/api/tables/status")
                                                connUpdate = urlUpdate.openConnection() as HttpURLConnection
                                                connUpdate.requestMethod = "PUT"
                                                connUpdate.setRequestProperty("Content-Type", "application/json; utf-8")
                                                connUpdate.connectTimeout = 5000
                                                connUpdate.readTimeout = 5000
                                                connUpdate.doOutput = true
                                                val updateBody = JSONObject().apply {
                                                    put("id", tableId)
                                                    put("status", tableStatus)
                                                }.toString()
                                                connUpdate.outputStream.use { it.write(updateBody.toByteArray(Charsets.UTF_8)) }
                                                connUpdate.responseCode
                                            } finally {
                                                connUpdate?.disconnect()
                                            }
                                        }
                                    }
                                }
                                connTables.disconnect()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            fetchBookings()
                            val msg = when (newStatus) {
                                "checked_in" -> "Đã xếp bàn! Bàn chuyển sang Đang dùng."
                                "checked_out" -> "Khách đã rời. Bàn chuyển về Trống."
                                "cancelled" -> "Đã hủy đặt bàn."
                                else -> "Cập nhật thành công!"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }

    // --- HÀM CẬP NHẬT BÀN LIÊN QUAN DỰA VÀO tableSummary ---
    // tableSummary ví dụ: "1 Bàn đơn A01 Khu A" hoặc "Gộp 2 bàn A01+A02 Khu A"
    suspend fun updateRelatedTables(tableSummary: String, newTableStatus: String) {
        withContext(Dispatchers.IO) {
            try {
                // Lấy danh sách bàn để tìm ID theo tableNumber
                val urlTables = URL("http://10.0.2.2:3000/api/tables")
                val connTables = urlTables.openConnection() as HttpURLConnection
                connTables.requestMethod = "GET"
                connTables.connectTimeout = 5000
                connTables.readTimeout = 5000

                if (connTables.responseCode == HttpURLConnection.HTTP_OK) {
                    val tablesStr = connTables.inputStream.bufferedReader().use { it.readText() }
                    val tablesArray = JSONObject(tablesStr).getJSONArray("tables")

                    // Tìm các tableNumber trong chuỗi tableSummary
                    // Ví dụ: "A01", "A02" từ "Gộp 2 bàn A01+A02 Khu A"
                    val tableNumberPattern = Regex("[AB]\\d{2}")
                    val matchedNumbers = tableNumberPattern.findAll(tableSummary)
                        .map { it.value }.toSet()

                    // Tìm tableId tương ứng và cập nhật từng bàn
                    for (i in 0 until tablesArray.length()) {
                        val t = tablesArray.getJSONObject(i)
                        val tNum = t.optString("tableNumber", "")
                        if (matchedNumbers.contains(tNum)) {
                            val tableId = t.getString("id")
                            // Gọi API cập nhật trạng thái bàn
                            var connUpdate: HttpURLConnection? = null
                            try {
                                val urlUpdate = URL("http://10.0.2.2:3000/api/tables/status")
                                connUpdate = urlUpdate.openConnection() as HttpURLConnection
                                connUpdate.requestMethod = "PUT"
                                connUpdate.setRequestProperty("Content-Type", "application/json; utf-8")
                                connUpdate.connectTimeout = 5000
                                connUpdate.readTimeout = 5000
                                connUpdate.doOutput = true

                                val updateBody = JSONObject().apply {
                                    put("id", tableId)
                                    put("status", newTableStatus)
                                }.toString()
                                connUpdate.outputStream.use { it.write(updateBody.toByteArray(Charsets.UTF_8)) }
                                connUpdate.responseCode
                            } finally {
                                connUpdate?.disconnect()
                            }
                        }
                    }
                }
                connTables.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- HÀM HỦY ĐẶT BÀN ---
    fun cancelBooking(bookingId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3000/api/bookings/status")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doOutput = true
                val body = JSONObject().apply {
                    put("id", bookingId.toIntOrNull() ?: bookingId)
                    put("status", "cancelled")
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                withContext(Dispatchers.Main) {
                    if (code == HttpURLConnection.HTTP_OK) {
                        fetchBookings()
                        Toast.makeText(context, "Đã hủy đặt bàn!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { conn?.disconnect() }
        }
    }
    // Nhấn "Đã hủy" lần 2 → xóa vĩnh viễn toàn bộ đơn đã hủy
    fun deleteAllCancelled() {
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3000/api/bookings/cancelled")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                withContext(Dispatchers.Main) {
                    if (code == HttpURLConnection.HTTP_OK) {
                        fetchBookings()
                        Toast.makeText(context, "Đã xóa tất cả đơn hủy!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { conn?.disconnect() }
        }
    }

    LaunchedEffect(Unit) { fetchBookings() }

    // Lọc danh sách
    val filteredBookings = bookingList.filter {
        val matchSearch = it.guestName.contains(searchQuery, ignoreCase = true) ||
                it.guestPhone.contains(searchQuery) ||
                it.bookingCode.contains(searchQuery, ignoreCase = true)
        val matchStatus = when (selectedFilter) {
            "Chờ xếp bàn" -> it.status == "pending"
            "Đang phục vụ" -> it.status == "checked_in"
            "Đã rời" -> it.status == "checked_out"
            "Đã hủy" -> it.status == "cancelled"
            else -> it.status != "cancelled"
        }
        matchSearch && matchStatus
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_booking") },
                containerColor = Color(0xFF007AFF)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đặt phân khu", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF8F9FA))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Lễ tân & Phân Khu", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Tìm mã, tên khách, SĐT...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Tất cả", "Chờ xếp bàn", "Đang phục vụ", "Đã rời", "Đã hủy").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Button(
                        onClick = { selectedFilter = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5EA)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(text = filter, color = if (isSelected) Color.White else Color.DarkGray, fontSize = 12.sp)
                    }
                }
            }
// Hiện nút "Xóa tất cả đơn hủy" khi đang ở tab Đã hủy
            if (selectedFilter == "Đã hủy" && filteredBookings.isNotEmpty()) {
                TextButton(
                    onClick = { deleteAllCancelled() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xóa tất cả đơn đã hủy", color = Color.Red, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredBookings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (selectedFilter == "Đã hủy") "Không có đặt bàn nào đã hủy."
                                else "Không tìm thấy lịch đặt bàn nào.",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    items(filteredBookings, key = { it.id }) { booking ->
                        BookingCardWithStatus(
                            booking = booking,
                            onLongClick = {
                                selectedBookingForAction = booking
                                showActionDialog = true
                            },
                            onStatusChange = { newStatus ->
                                updateBookingStatus(booking, newStatus)
                            },
                            // ĐÃ THÊM: Điều hướng sang PaymentScreen
                            onPayment = {
                                navController.navigate("Payment/${booking.id}")
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog tùy chọn khi nhấn giữ
    if (showActionDialog && selectedBookingForAction != null) {
        val booking = selectedBookingForAction!!
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("${booking.bookingCode} — ${booking.guestName}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Bạn muốn thực hiện thao tác gì?") },
            confirmButton = {
                TextButton(onClick = {
                    showActionDialog = false
                    navController.navigate("EditBooking/${booking.id}")
                }) {
                    Text("Chỉnh sửa", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showActionDialog = false
                    cancelBooking(booking.id)
                }) {
                    Text("Hủy đặt bàn", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

// =========================================================================
// BOOKING CARD CÓ NÚT CHUYỂN TRẠNG THÁI
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookingCardWithStatus(
    booking: BookingItem,
    onLongClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onPayment: () -> Unit
)
{
    // Màu và nhãn theo trạng thái hiện tại
    val (statusText, statusColor) = when (booking.status) {
        "pending" -> "Chờ xếp bàn" to Color(0xFFFF9500)
        "checked_in" -> "Đang phục vụ" to Color(0xFF28A745)
        "checked_out" -> "Đã rời" to Color(0xFF636366)
        "cancelled" -> "Đã hủy" to Color(0xFF8E8E93)
        else -> "Không xác định" to Color.Gray
    }

    // Nút hành động tiếp theo tùy trạng thái hiện tại
    val nextAction: Pair<String, String>? = when (booking.status) {
        "pending" -> "checked_in" to "Xếp bàn"
        // ĐÃ SỬA: Đổi "Rời bàn" thành "Thanh toán", điều hướng sang PaymentScreen
        "checked_in" -> "payment" to "Thanh toán"
        else -> null
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Dòng 1: Tên khách + mã booking
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = booking.guestName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = booking.bookingCode, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "SĐT: ${booking.guestPhone}", color = Color.Gray, fontSize = 14.sp)
            Text(text = booking.tableSummary, color = Color(0xFFFF9500), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "🕒 ${booking.bookingDate} • ${booking.bookingTime}",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE5E5EA))
            Spacer(modifier = Modifier.height(8.dp))

            // Dòng cuối: Badge trạng thái + Số tiền + Nút hành động
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge trạng thái
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "${booking.totalAmount.toLong()} VND", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // Nút nhỏ chuyển trạng thái tiếp theo
                    if (nextAction != null) {
                        val (nextStatus, nextLabel) = nextAction
                        val btnColor = when (nextStatus) {
                            "checked_in" -> Color(0xFF007AFF)
                            "payment" -> Color(0xFF34C759)
                            else -> Color(0xFF636366)
                        }
                        Button(
                            onClick = {
                                if (nextStatus == "payment") {
                                    // ĐÃ SỬA: Điều hướng sang màn hình thanh toán thay vì đổi trạng thái
                                    onPayment()
                                } else {
                                    onStatusChange(nextStatus)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(text = nextLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}