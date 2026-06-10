package com.example.restaurantbookingapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

val StatusGreen = Color(0xFF34C759)
val StatusBlue = Color(0xFF007AFF)
val StatusOrange = Color(0xFFFF9500)
val BackgroundGray = Color(0xFFF2F2F7)

// Data class chứa thống kê dashboard
data class DashboardStats(
    val pendingBookings: Int = 0,
    val checkedInBookings: Int = 0,
    val checkedOutBookings: Int = 0,
    val availableTables: Int = 0,
    val occupiedTables: Int = 0,
    val bookedTables: Int = 0,
    val totalTables: Int = 0,
    val revenue: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Trạng thái dữ liệu động từ API
    var stats by remember { mutableStateOf(DashboardStats()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- HÀM TẢI THỐNG KÊ TỪ API ---
    suspend fun loadStats() {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3001/api/dashboard/stats")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val b = json.getJSONObject("bookings")
                    val t = json.getJSONObject("tables")

                    withContext(Dispatchers.Main) {
                        stats = DashboardStats(
                            pendingBookings = b.optInt("pending", 0),
                            checkedInBookings = b.optInt("checkedIn", 0),
                            checkedOutBookings = b.optInt("checkedOut", 0),
                            availableTables = t.optInt("available", 0),
                            occupiedTables = t.optInt("occupied", 0),
                            bookedTables = t.optInt("booked", 0),
                            totalTables = t.optInt("total", 0),
                            revenue = b.optDouble("revenue", 0.0)
                        )
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // Tải dữ liệu khi mở màn hình
    LaunchedEffect(Unit) { loadStats() }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    Scaffold(
        topBar = {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    Text(text = "Tổng quan Nhà Hàng", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Surface(
                        modifier = Modifier.size(36.dp).clickable { navController.navigate("Profile") },
                        color = StatusBlue.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "A", color = StatusBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        containerColor = BackgroundGray
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Tổng quan trạng thái", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = "Dữ liệu thực tế từ hệ thống", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StatusBlue)
                }
            } else {
                // 4 StatCard động
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(
                                title = "Bàn trống",
                                count = "${stats.availableTables} bàn",
                                color = StatusGreen
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(
                                title = "Đang ăn",
                                count = "${stats.occupiedTables} bàn",
                                color = StatusBlue
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(
                                title = "Chờ xếp bàn",
                                count = "${stats.pendingBookings} đơn",
                                color = StatusOrange
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            StatCard(
                                title = "Đang phục vụ",
                                count = "${stats.checkedInBookings} đơn",
                                color = StatusGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Card doanh thu hôm nay (động)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(8.dp), color = StatusGreen, shape = CircleShape) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Hoạt động hôm nay", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Đã phục vụ: ${stats.checkedOutBookings} đơn | Đang phục vụ: ${stats.checkedInBookings} đơn",
                            fontSize = 14.sp, color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Doanh thu: ${currencyFormatter.format(stats.revenue)}",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StatusGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card cảnh báo nếu có đơn đang chờ
                if (stats.pendingBookings > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBE6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Cần xử lý", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Có ${stats.pendingBookings} đơn đang chờ xếp bàn.",
                                fontSize = 13.sp, color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Điều phối ngay →",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusBlue,
                                modifier = Modifier.clickable { navController.navigate("LeTan") }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun StatCard(title: String, count: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, shape = CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = count, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}