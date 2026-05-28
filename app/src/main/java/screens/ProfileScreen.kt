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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController) {
    val scrollState = rememberScrollState() // Đảm bảo hiển thị đúng khi có bàn phím xuất hiện bằng ScrollView

    // SafeAreaView (root container) - Khối nền bao bọc toàn bộ màn hình
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        // 1. Header (Bold, Center, No Back Button)
        ProfileScreenHeader(title = "Hồ sơ cá nhân")

        // 2. ScrollView (Bọc toàn bộ nội dung phía dưới)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState) // Quyết định dùng ScrollView thay FlatList
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 3. ProfileInfoCard (Hiển thị Email & ID)
            ProfileInfoCard(
                email = "admin@restaurant.com",
                id = "1"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. SectionTitle: "Thao tác nhanh"
            ProfileSectionTitle(title = "Thao tác nhanh")

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Tập hợp các thành phần ProfileActionItem riêng biệt tái sử dụng
            ProfileActionItem(
                icon = Icons.Default.Lock,
                iconColor = Color(0xFFFFCC00), // Khóa, vàng
                title = "Đổi mật khẩu",
                description = "Thay đổi mật khẩu bảo mật tài khoản",
                onPress = { navController.navigate("ChangePassword") }
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                icon = Icons.Default.Person,
                iconColor = Color(0xFF34C759), // Người, xanh lá
                title = "Nhân viên",
                description = "Quản lý danh sách và phân quyền nhân sự",
                onPress = { navController.navigate("UserList") }
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                icon = Icons.Default.ShoppingCart,
                iconColor = Color(0xFFFF9500), // Hộp, cam
                title = "Kho vật tư",
                description = "Kiểm kê nguyên liệu, thực phẩm nhà hàng",
                onPress = { navController.navigate("Inventory") }
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                icon = Icons.Default.List,
                iconColor = Color(0xFF007AFF), // Tài liệu, xanh dương
                title = "Hóa đơn",
                description = "Tra cứu lịch sử hóa đơn đặt bàn",
                onPress = { navController.navigate("InvoiceList") }
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFFFF3B30), // Chuông, đỏ
                title = "Thông báo",
                description = "Cấu hình nhận chuông cảnh báo hệ thống",
                onPress = { navController.navigate("NotificationScreen") }
            )
            Spacer(modifier = Modifier.height(10.dp))

            ProfileActionItem(
                icon = Icons.Default.Settings,
                iconColor = Color(0xFF8E8E93), // Bánh răng, xám
                title = "Cài đặt",
                description = "Thiết lập thông số máy in và hệ thống",
                onPress = { navController.navigate("SettingsScreen") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 6. LogoutButton (Full width, màu cam, xử lý tách biệt loading & ConfirmModal)
            ProfileLogoutButton(navController = navController)

            Spacer(modifier = Modifier.height(100.dp)) // Tránh bị BottomTabBar che khuất
        }
    }
}

// =========================================================================
// --- CHI TIẾT CÁC CUSTOM COMPONENTS ĐƯỢC TÁCH RIÊNG THEO ĐÚNG SƠ ĐỒ CÂY ---
// =========================================================================

@Composable
fun ProfileScreenHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun ProfileInfoCard(email: String, id: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                color = Color(0xFF007AFF).copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "A", color = Color(0xFF007AFF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Email: $email", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "ID: $id", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
fun ProfileActionItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    onPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPress() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = description, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "ChevronIcon",
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileLogoutButton(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showConfirmModal by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    Button(
        onClick = { showConfirmModal = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
        shape = RoundedCornerShape(10.dp),
        enabled = !isLoggingOut
    ) {
        if (isLoggingOut) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(text = "Đăng xuất", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showConfirmModal) {
        AlertDialog(
            onDismissRequest = { showConfirmModal = false },
            title = { Text(text = "Xác nhận thoát", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text(text = "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản quản trị nhà hàng không?", fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmModal = false
                        coroutineScope.launch {
                            isLoggingOut = true
                            delay(1000)
                            isLoggingOut = false
                            Toast.makeText(context, "Đã đăng xuất!", Toast.LENGTH_SHORT).show()
                            navController.navigate("Login") { popUpTo(0) }
                        }
                    }
                ) {
                    // SỬA TẠI ĐÂY: Thay thế java.theme lỗi bằng thuộc tính chuẩn fontWeight
                    Text(text = "Đồng ý", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmModal = false }) {
                    Text(text = "Hủy bỏ", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        )
    }
}