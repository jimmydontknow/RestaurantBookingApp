package com.example.restaurantbookingapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.restaurantbookingapp.screens.*
import com.example.restaurantbookingapp.ui.theme.RestaurantBookingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RestaurantBookingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootNavigation()
                }
            }
        }
    }
}

// --- ĐIỀU HƯỚNG GỐC: Login → Admin hoặc Customer ---
@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("admin_main") {
            AdminMainScreen(rootNavController = navController)
        }
        composable("customer_main") {
            CustomerMainScreen(rootNavController = navController)
        }
        composable("employee_main") {
            EmployeeMainScreen(rootNavController = navController)
        }
    }
}

// --- MÀN HÌNH ADMIN (có BottomBar đầy đủ) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(rootNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Chỉ hiện BottomBar khi ở tab chính
    val showBottomBar = currentRoute in listOf("dashboard", "LeTan", "sodo", "invoices", "them")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {

                    // 1. TRANG CHỦ
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        },
                        label = { Text("Trang chủ") },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )

                    // 2. LỄ TÂN
                    NavigationBarItem(
                        selected = currentRoute == "LeTan",
                        onClick = { navController.navigate("LeTan") },
                        label = { Text("Lễ tân") },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )

                    // 3. SƠ ĐỒ BÀN
                    NavigationBarItem(
                        selected = currentRoute == "sodo",
                        onClick = { navController.navigate("sodo") },
                        label = { Text("Sơ đồ") },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                    )

                    // 4. THANH TOÁN
                    NavigationBarItem(
                        selected = currentRoute == "invoices",
                        onClick = { navController.navigate("invoices") },
                        label = { Text("Thanh toán") },
                        icon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                    )

                    // 5. THÊM (cài đặt / đổi vai trò)
                    NavigationBarItem(
                        selected = currentRoute == "them",
                        onClick = { navController.navigate("them") },
                        label = { Text("Thêm") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") { DashboardScreen(navController = navController) }
            composable("LeTan") { BookingsListScreen(navController = navController, canManageBookings = true, canOrderFood = true) }
            composable("sodo") { SoDoBanScreen(navController = navController) }
            composable("invoices") { InvoiceListScreen(navController = navController) }
            composable("them") { ThemScreen(navController = navController) }
            composable("Profile") { ProfileScreen(navController = navController) }
            composable("CreateBooking") { CreateBookingScreen(navController = navController) }
            composable("create_booking") { CreateBookingScreen(navController = navController) }
            composable("EditBooking/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                EditBookingScreen(navController = navController, bookingId = bookingId)
            }
            composable("Payment/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                PaymentScreen(navController = navController, bookingId = bookingId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeMainScreen(rootNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(
        "employee_bookings",
        "employee_tables",
        "employee_invoices",
        "employee_lookup"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "employee_bookings",
                        onClick = { navController.navigate("employee_bookings") },
                        label = { Text("Đơn") },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "employee_tables",
                        onClick = { navController.navigate("employee_tables") },
                        label = { Text("Sơ đồ") },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "employee_invoices",
                        onClick = { navController.navigate("employee_invoices") },
                        label = { Text("Hóa đơn") },
                        icon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "employee_lookup",
                        onClick = { navController.navigate("employee_lookup") },
                        label = { Text("Tra khách") },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "employee_bookings",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("employee_bookings") {
                BookingsListScreen(
                    navController = navController,
                    canManageBookings = false,
                    canOrderFood = true
                )
            }
            composable("employee_tables") { SoDoBanScreen(navController) }
            composable("employee_invoices") { InvoiceListScreen(navController) }
            composable("employee_lookup") { StaffCustomerLookupScreen() }
            composable("Payment/{bookingId}") { backStackEntry ->
                PaymentScreen(
                    navController,
                    backStackEntry.arguments?.getString("bookingId") ?: ""
                )
            }
            composable("menu/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString("bookingId")
                    ?.toIntOrNull()
                    ?: 0
                if (bookingId > 0) {
                    CustomerMenuScreen(navController, bookingId)
                }
            }
        }
    }
}
