package com.example.restaurantbookingapp.screens

import android.widget.Toast
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- ĐỊNH NGHĨA HỆ MÀU ĐỒNG BỘ ĐỒ ÁN NHÀ HÀNG (Tránh lỗi Unresolved reference) ---
val StatusGreen = Color(0xFF34C759)  // Bàn trống
val StatusBlue = Color(0xFF007AFF)   // Đang ăn / Đang đặt
val StatusOrange = Color(0xFFFF9500) // Đang dọn dẹp
val BackgroundGray = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) { // Nhận navController để giải quyết dứt điểm lỗi build tại MainActivity
    val context = LocalContext.current
    val scrollState = rememberScrollState() // Thỏa mãn điều kiện dùng ScrollView bọc toàn bộ màn hình

    Scaffold(
        topBar = {
            // [HEADER COMPONENT]: Không có nút back, chứa nút Menu, Tiêu đề và Avatar
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { Toast.makeText(context, "Mở Menu hệ thống", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Default.Menu, contentDescription = "MenuButton") // MenuButton
                    }
                    Text(
                        text = "Tổng quan Nhà Hàng", // ScreenTitle
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    // AvatarButton chứa chữ cái đầu của tài khoản Admin
                    Surface(
                        modifier = Modifier.size(36.dp).clickable { navController.navigate("Profile") },
                        color = StatusBlue.copy(alpha = 0.15f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "A", color = StatusBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp) // AvatarButton: "A"
                        }
                    }
                }
            }
        },
        containerColor = BackgroundGray
    ) { innerPadding ->

        // [SCROLLVIEW ROOT]: Cho phép cuộn dọc mượt mà toàn màn hình khi bàn phím xuất hiện
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {

            // --- TitleSection: Tiêu đề động cập nhật thời gian thực ---
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Tổng quan trạng thái", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black) //
            Text(text = "Dữ liệu cập nhật theo thời gian thực", fontSize = 13.sp, color = Color.Gray) //
            Spacer(modifier = Modifier.height(16.dp))

            // --- StatsGrid (Lưới ô thống kê cấu trúc 2x2 quy đổi từ Flexbox) ---
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(title = "Bàn trống", count = "14 bàn", color = StatusGreen) //
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(title = "Đang ăn", count = "6 bàn", color = StatusBlue) //
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(title = "Đang đặt", count = "5 bàn", color = StatusBlue) //
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(title = "Đang dọn dẹp", count = "2 bàn", color = StatusOrange) //
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- TodaySection: Hoạt động hôm nay và doanh thu nhà hàng ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(8.dp), color = StatusGreen, shape = CircleShape) {} // DotIndicator
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Hoạt động hôm nay", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black) //
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Khách đến: 28 | Khách đi: 22", fontSize = 14.sp, color = Color.DarkGray) //
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Doanh thu: 5,600,000 đ", // Định dạng tiền tệ chuẩn trực quan
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- AlertSection: Thông báo khẩn cấp cần nhân viên xử lý gấp ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBE6)), // Màu nền vàng nhạt cảnh báo
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = StatusOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Cần chú ý khẩn cấp", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StatusOrange) //
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // AlertBadge thông báo chi tiết
                    Text(
                        text = "⚠️ 3 bàn vừa trả khách cần nhân viên dọn dẹp.", //
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Điều phối ngay →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusBlue,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Đã chuyển tới Sơ Đồ Bàn!", Toast.LENGTH_SHORT).show()
                            navController.navigate("SoDoBan") // Thực thi LinkText chuyển trang
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // Đệm chân tránh bị che khuất bởi BottomTabBar
        }
    }
}

// --- CUSTOM MINI COMPONENT: STATCARD (Thẻ ô vuông hiển thị trạng thái bàn ăn) ---
@Composable
fun StatCard(title: String, count: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(modifier = Modifier.size(20.dp, 16.dp), color = color, shape = RoundedCornerShape(4.dp)) {} // Ô màu đại diện trạng thái
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 13.sp, color = Color.Gray) //
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) //
        }
    }
}