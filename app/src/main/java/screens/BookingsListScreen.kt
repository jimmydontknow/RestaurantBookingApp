package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

data class BookingData(
    val id: String,
    val bookingCode: String,
    val guestName: String,
    val guestPhone: String,
    val tableSummary: String,
    val totalAmount: Double,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookingsListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }

    // Danh sách lịch đặt bàn động từ SQL Server
    val rawBookings = remember { mutableStateListOf<BookingData>() }

    // ĐÃ THÊM: Trạng thái cho popup menu nhấn giữ và dialog xác nhận xóa
    // Khai báo ĐÚNG CHỖ trong BookingsListScreen để dùng được ở cả items và dialog
    var selectedBooking by remember { mutableStateOf<BookingData?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Gọi API lấy danh sách KHÁCH ĐẶT BÀN từ Node.js
    val fetchBookingsFromApi = suspend {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                // ĐÃ SỬA: Gọi đúng đường dẫn lấy danh sách khách đặt bàn
                val url = URL("http://10.0.2.2:3000/api/bookings")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(responseStr)
                    val jsonArray = jsonObject.getJSONArray("bookings")

                    val fetchedList = mutableListOf<BookingData>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        fetchedList.add(
                            BookingData(
                                id = item.getString("id"),
                                bookingCode = item.getString("bookingCode"),
                                guestName = item.getString("guestName"),
                                guestPhone = item.getString("guestPhone"),
                                tableSummary = item.getString("tableSummary"),
                                totalAmount = item.getDouble("totalAmount"),
                                status = item.getString("status")
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        rawBookings.clear()
                        rawBookings.addAll(fetchedList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi nạp dữ liệu lễ tân: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // Tự động tải danh sách khách khi mở màn hình
    LaunchedEffect(Unit) {
        fetchBookingsFromApi()
    }

    val filteredBookings = rawBookings.filter { booking ->
        val matchesSearch = booking.bookingCode.contains(searchQuery, ignoreCase = true) ||
                booking.guestName.contains(searchQuery, ignoreCase = true) ||
                booking.guestPhone.contains(searchQuery, ignoreCase = true)

        val mappedStatus = when (selectedFilter) {
            "Chờ xếp bàn" -> "pending"
            "Đang phục vụ" -> "checked_in"
            "Đã rời đi" -> "checked_out"
            "Đã hủy" -> "cancelled"
            else -> "Tất cả"
        }

        val matchesFilter = when (mappedStatus) {
            "Tất cả" -> booking.status != "cancelled" // Ẩn "Đã hủy" khỏi tab Tất cả
            else -> booking.status == mappedStatus
        }

        matchesSearch && matchesFilter
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        ScreenHeader(
            title = "Danh sách đặt bàn",
            rightAction = {
                Button(
                    onClick = { navController.navigate("CreateBooking") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Thêm đặt bàn", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    fetchBookingsFromApi()
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SearchBox(
                        value = searchQuery,
                        placeholder = "Tìm theo mã, tên khách, số điện thoại...",
                        onTextChange = { searchQuery = it },
                        onClear = { searchQuery = "" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalFilterChips(
                        activeFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (filteredBookings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Không tìm thấy lịch đặt bàn nào.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(items = filteredBookings, key = { it.id }) { bookingItem ->
                        BookingCard(
                            booking = bookingItem,
                            onPress = {
                                navController.navigate("EditBooking/${bookingItem.id}")
                            },
                            // ĐÃ THÊM: Nhấn giữ → lưu booking được chọn và hiện popup menu
                            onLongPress = {
                                selectedBooking = bookingItem
                                showOptionsMenu = true
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    if (filteredBookings.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    isLoadingMore = true
                                    fetchBookingsFromApi()
                                    isLoadingMore = false
                                }
                            }) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(text = "Tải lại dữ liệu...", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // ĐÃ THÊM: POPUP MENU khi nhấn giữ card - hiện 2 lựa chọn Sửa và Xóa
    if (showOptionsMenu && selectedBooking != null) {
        AlertDialog(
            onDismissRequest = { showOptionsMenu = false },
            title = {
                Text("${selectedBooking!!.bookingCode} - ${selectedBooking!!.guestName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = { Text("Bạn muốn thực hiện thao tác gì?", fontSize = 14.sp) },
            confirmButton = {
                // Nút Chỉnh sửa → điều hướng sang EditBookingScreen
                TextButton(onClick = {
                    showOptionsMenu = false
                    navController.navigate("EditBooking/${selectedBooking!!.id}")
                }) {
                    Text("Chỉnh sửa", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                // Nút Xóa → mở dialog xác nhận xóa
                TextButton(onClick = {
                    showOptionsMenu = false
                    showDeleteDialog = true
                }) {
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        )
    }

    // ĐÃ THÊM: DIALOG XÁC NHẬN XÓA - tránh xóa nhầm
    if (showDeleteDialog && selectedBooking != null) {
        val bookingToDelete = selectedBooking!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Bạn có chắc muốn xóa đặt bàn của khách \"${bookingToDelete.guestName}\" không? Hành động này không thể hoàn tác.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            var conn: HttpURLConnection? = null
                            try {
                                // Gọi DELETE /api/bookings/:id lên server
                                val url = URL("http://10.0.2.2:3000/api/bookings/${bookingToDelete.id}")
                                conn = url.openConnection() as HttpURLConnection
                                conn.requestMethod = "DELETE"
                                conn.connectTimeout = 5000
                                conn.readTimeout = 5000

                                val responseCode = conn.responseCode
                                withContext(Dispatchers.Main) {
                                    if (responseCode == HttpURLConnection.HTTP_OK) {
                                        // Xóa khỏi danh sách UI ngay lập tức
                                        rawBookings.removeIf { it.id == bookingToDelete.id }
                                        Toast.makeText(context, "Đã xóa đặt bàn của ${bookingToDelete.guestName}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Lỗi xóa: mã $responseCode", Toast.LENGTH_SHORT).show()
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
                    Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun ScreenHeader(title: String, rightAction: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            rightAction()
        }
    }
}

@Composable
fun SearchBox(value: String, placeholder: String, onTextChange: (String) -> Unit, onClear: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder, fontSize = 14.sp, color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.clickable { onClear() })
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
    )
}

@Composable
fun HorizontalFilterChips(activeFilter: String, onSelectFilter: (String) -> Unit) {
    val filters = listOf("Tất cả", "Chờ xếp bàn", "Đang phục vụ", "Đã rời đi", "Đã hủy")
    val scrollState = rememberScrollState()
    // KHÔNG khai báo selectedBooking/showOptionsMenu/showDeleteDialog ở đây
    // Các biến đó đã được khai báo đúng chỗ trong BookingsListScreen phía trên
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { name ->
            val isActive = activeFilter == name
            Surface(
                modifier = Modifier.clickable { onSelectFilter(name) },
                color = if (isActive) Color(0xFF007AFF) else Color(0xFFE5E5EA),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = name,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (isActive) Color.White else Color.DarkGray,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookingCard(booking: BookingData, onPress: () -> Unit, onLongPress: () -> Unit) {
    val vietnameseLocale = Locale("vi", "VN")
    val currencyFormatter = NumberFormat.getCurrencyInstance(vietnameseLocale)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // ĐÃ THÊM: combinedClickable để xử lý cả nhấn thường lẫn nhấn giữ
            .combinedClickable(
                onClick = { onPress() },
                onLongClick = { onLongPress() }
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Mã: " + booking.bookingCode, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                StatusBadge(statusType = booking.status)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "Guest Icon", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = booking.guestName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = "Table Icon", tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = booking.tableSummary, fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Tổng tiền / Tiền cọc giữ bàn", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormatter.format(booking.totalAmount),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759)
                    )
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Arrow Right", tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun StatusBadge(statusType: String) {
    val (backgroundColor, textColor, textLabel) = when (statusType) {
        "checked_in" -> Triple(Color(0xFFE4F9E7), Color(0xFF34C759), "Đang phục vụ")
        "pending" -> Triple(Color(0xFFFFF1E6), Color(0xFFFF9500), "Chờ xếp bàn")
        "cancelled" -> Triple(Color(0xFFFFE5E5), Color(0xFFFF3B30), "Đã hủy") // ĐÃ THÊM
        else -> Triple(Color(0xFFE5F1FF), Color(0xFF007AFF), "Đã rời đi")
    }
    Surface(color = backgroundColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = textLabel,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}