package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
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
import com.example.restaurantbookingapp.ui.theme.BackgroundGray
import com.example.restaurantbookingapp.ui.theme.TableAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeTanScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        // --- HEADER TOPBAR CỦA MÀN HÌNH LỄ TÂN ---
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Lễ Tân", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
                .background(BackgroundGray)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(text = "Danh sách đặt bàn hôm nay", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = "Kiểm tra và sắp xếp chỗ ngồi cho khách", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            // --- THÈ MẪU THÔNG TIN KHÁCH ĐẶT TRƯỚC 1 ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Anh Nguyễn Văn A", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                .background(TableAvailable.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "18:30", color = TableAvailable, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Số điện thoại: 0901234567", fontSize = 14.sp, color = Color.DarkGray)
                    Text(text = "Số lượng khách: 4 người (Bàn VIP)", fontSize = 14.sp, color = Color.Gray)
                }
            }

            // --- THẺ MẪU THÔNG TIN KHÁCH ĐẶT TRƯỚC 2 ---
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Chị Lê Thị B", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Box(
                            modifier = Modifier
                                // ĐÃ SỬA: Thay thế Color.Orange bằng mã Hex Color(0xFFFFA500) để tránh lỗi Unresolved reference
                                .background(Color(0xFFFFA500).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // ĐÃ SỬA: Đồng bộ mã màu cam Hex cố định cho Text hiển thị giờ
                            Text(text = "20:00", color = Color(0xFFFFA500), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Số điện thoại: 0988888888", fontSize = 14.sp, color = Color.DarkGray)
                    Text(text = "Số lượng khách: 2 người (Bàn đôi)", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}