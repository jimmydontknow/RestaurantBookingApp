package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restaurantbookingapp.ui.theme.BackgroundGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        // --- HEADER TOPBAR CỦA MÀN HÌNH XEM THÊM ---
        topBar = {
            TopAppBar(
                title = { Text("Tính năng mở rộng", fontWeight = FontWeight.Bold) },
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
            Text(text = "Hệ thống bổ sung", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = "Cài đặt tài khoản và thông tin phần mềm", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(20.dp))

            // --- THÀNH PHẦN DANH SÁCH MENU LỰA CHỌN ---
            MenuItemRow(title = "Cấu hình nhà hàng", icon = Icons.Default.Settings) {
                Toast.makeText(context, "Mở Cài đặt hệ thống", Toast.LENGTH_SHORT).show()
            }
            MenuItemRow(title = "Quản lý nhân sự", icon = Icons.Default.AccountCircle) {
                Toast.makeText(context, "Mở Danh sách nhân viên", Toast.LENGTH_SHORT).show()
            }
            MenuItemRow(title = "Thông tin đồ án", icon = Icons.Default.Info) {
                Toast.makeText(context, "Đồ án Quản Lý Đặt Bàn Nhà Hàng v1.0", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// --- HÀM COMPONENT PHỤ VẼ TỪNG DÒNG MENU CHO ĐẸP VÀ GỌN CODE ---
@Composable
fun MenuItemRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}