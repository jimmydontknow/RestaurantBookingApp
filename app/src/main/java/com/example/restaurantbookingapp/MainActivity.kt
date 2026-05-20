package com.example.restaurantbookingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// --- ĐỒNG BỘ IMPORT CHÍNH XÁC TỪ THƯ MỤC SCREENS CỦA ĐỒ ÁN ---
import com.example.restaurantbookingapp.screens.DashboardScreen
import com.example.restaurantbookingapp.screens.BookingsListScreen
import com.example.restaurantbookingapp.screens.ProfileScreen
import com.example.restaurantbookingapp.screens.SoDoBanScreen
import com.example.restaurantbookingapp.screens.ThemScreen
import com.example.restaurantbookingapp.ui.theme.RestaurantBookingAppTheme
import com.example.restaurantbookingapp.screens.CreateBookingScreen
// ĐÃ THÊM: Import màn hình chỉnh sửa đặt bàn
import com.example.restaurantbookingapp.screens.EditBookingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RestaurantBookingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // --- BỘ ĐIỀU KHIỂN ĐIỀU HƯỚNG MÀN HÌNH ---
    val navController = rememberNavController()

    // --- LẤY TRẠNG THÁI ROUTE HIỆN TẠI ĐỂ ĐỊNH VỊ NÚT ĐANG CHỌN TRÊN BOTTOM BAR ---
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        // --- THÀNH PHẦN BOTTOM NAVIGATION BAR (Thanh điều hướng cố định phía dưới cùng) ---
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {

                // 1. NÚT ĐIỀU HƯỚNG: TRANG CHỦ (DASHBOARD)
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        if (currentRoute != "dashboard") {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    },
                    label = { Text("Trang chủ") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") }
                )

                // 2. NÚT ĐIỀU HƯỚNG: LỄ TÂN
                NavigationBarItem(
                    selected = currentRoute == "LeTan",
                    onClick = {
                        if (currentRoute != "LeTan") {
                            navController.navigate("LeTan")
                        }
                    },
                    label = { Text("Lễ tân") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Lễ tân") }
                )

                // 3. NÚT ĐIỀU HƯỚNG: SƠ ĐỒ BÀN
                NavigationBarItem(
                    selected = currentRoute == "sodo",
                    onClick = {
                        if (currentRoute != "sodo") {
                            navController.navigate("sodo")
                        }
                    },
                    label = { Text("Sơ đồ") },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sơ đồ") }
                )

                // 4. NÚT ĐIỀU HƯỚNG: XEM THÊM
                NavigationBarItem(
                    selected = currentRoute == "them" || currentRoute == "Profile",
                    onClick = {
                        if (currentRoute != "them") {
                            navController.navigate("them")
                        }
                    },
                    label = { Text("Thêm") },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Thêm") }
                )
                // ĐÃ XÓA: composable("EditBooking/...") bị đặt nhầm vào đây
            }
        }
    ) { innerPadding ->

        // --- BỘ ĐỊNH TUYẾN NAVHOST ---
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            // [Tuyến 1]: Màn hình Tổng quan Dashboard
            composable("dashboard") {
                DashboardScreen(navController = navController)
            }

            // [Tuyến 2]: Màn hình Lễ tân quản lý danh sách đặt bàn
            composable("LeTan") {
                BookingsListScreen(navController = navController)
            }

            // [Tuyến 3]: Màn hình Sơ đồ bàn ăn nhà hàng
            composable("sodo") {
                SoDoBanScreen(navController = navController)
            }

            // [Tuyến 4]: Màn hình Mở rộng (Cài đặt/Xem thêm)
            composable("them") {
                ThemScreen(navController = navController)
            }

            // [Tuyến 5]: Màn hình Hồ sơ cá nhân
            composable("Profile") {
                ProfileScreen(navController = navController)
            }

            // [Tuyến 6]: Màn hình Tạo đặt bàn mới
            composable("CreateBooking") {
                CreateBookingScreen(navController = navController)
            }

            // ĐÃ THÊM ĐÚNG CHỖ - [Tuyến 7]: Màn hình Chỉnh sửa đặt bàn
            // Nhận bookingId từ URL để load đúng dữ liệu cần sửa
            composable("EditBooking/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                EditBookingScreen(navController = navController, bookingId = bookingId)
            }
        }
    }

    // COMPONENT TẠM THỜI ĐÃ ĐƯỢC IMPORT ĐỦ THƯ VIỆN ĐỂ SỬA LỖI ĐỎ LÒM
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CreateBookingPlaceholder(navController: NavController) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Tạo đặt bàn mới",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Màn hình thêm đặt bàn đang được xây dựng...", color = Color.Gray)
            }
        }
    }
}