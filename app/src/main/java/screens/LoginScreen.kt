package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.restaurantbookingapp.network.ApiClient
import com.example.restaurantbookingapp.network.ApiResult
import com.example.restaurantbookingapp.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// ======================================================================
// LoginScreen — Xác thực qua backend (bcrypt + JWT)
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    val context       = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedRole   by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading      by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf("") }

    // Form fields
    var fullName     by remember { mutableStateOf("") }
    var phoneNumber  by remember { mutableStateOf("") }
    var username     by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    fun resetForm() {
        fullName    = ""
        phoneNumber = ""
        username    = ""
        password    = ""
        errorMessage = ""
    }

    // ------------------------------------------------------------------
    // Điều hướng theo vai trò sau khi đăng nhập / đăng ký thành công
    // ------------------------------------------------------------------
    fun navigateByRole(role: String) {
        val destination = when (role.lowercase()) {
            "admin", "manager", "receptionist" -> "admin_main"
            "employee", "staff" -> "employee_main"
            "customer" -> "customer_main"
            else -> "login"
        }
        if (destination != "login") {
            navController.navigate(destination) {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    fun userPhone(user: JSONObject): String =
        user.optString("phoneNumber")
            .ifBlank { user.optString("phone") }

    // ------------------------------------------------------------------
    // ĐĂNG KÝ — POST /api/auth/register
    // ------------------------------------------------------------------
    fun registerAccount() {
        if (fullName.isBlank() || phoneNumber.isBlank() ||
            username.isBlank() || password.isBlank()
        ) {
            errorMessage = "Vui lòng nhập đầy đủ thông tin!"
            return
        }
        if (password.length < 6) {
            errorMessage = "Mật khẩu phải có ít nhất 6 ký tự!"
            return
        }

        isLoading    = true
        errorMessage = ""

        coroutineScope.launch {
            val body = JSONObject().apply {
                put("username", username.trim())
                put("password", password)
                put("role",     selectedRole)
                put("fullName", fullName.trim())
                put("phone",    phoneNumber.trim())
            }

            val result = withContext(Dispatchers.IO) {
                ApiClient.post("/api/auth/register", body)
            }

            isLoading = false

            when (result) {
                is ApiResult.Success -> {
                    val json  = JSONObject(result.data)
                    val token = json.optString("token")
                    val user  = json.optJSONObject("user")

                    if (token.isNotBlank() && user != null) {
                        // Lưu phiên vào EncryptedSharedPreferences
                        TokenManager.saveSession(
                            token    = token,
                            userId   = user.optString("id"),
                            username = user.optString("username"),
                            role     = user.optString("role"),
                            fullName = user.optString("fullName"),
                            phone    = userPhone(user)
                        )
                        Toast.makeText(context, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                        navigateByRole(user.optString("role"))
                    } else {
                        errorMessage = "Lỗi: Không nhận được token từ server"
                    }
                }
                is ApiResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // ĐĂNG NHẬP — POST /api/auth/login
    // ------------------------------------------------------------------
    fun loginAccount() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Vui lòng nhập tài khoản và mật khẩu!"
            return
        }

        isLoading    = true
        errorMessage = ""

        coroutineScope.launch {
            val body = JSONObject().apply {
                put("username", username.trim())
                put("password", password)
                put("role",     selectedRole)
            }

            val result = withContext(Dispatchers.IO) {
                ApiClient.post("/api/auth/login", body)
            }

            isLoading = false

            when (result) {
                is ApiResult.Success -> {
                    val json  = JSONObject(result.data)
                    val token = json.optString("token")
                    val user  = json.optJSONObject("user")

                    if (token.isNotBlank() && user != null) {
                        // Lưu phiên vào EncryptedSharedPreferences
                        TokenManager.saveSession(
                            token    = token,
                            userId   = user.optString("id"),
                            username = user.optString("username"),
                            role     = user.optString("role"),
                            fullName = user.optString("fullName"),
                            phone    = userPhone(user)
                        )
                        navigateByRole(user.optString("role"))
                    } else {
                        errorMessage = "Lỗi: Không nhận được token từ server"
                    }
                }
                is ApiResult.Error -> {
                    errorMessage = result.message
                }
            }
        }
    }

    // ====================================================================
    // UI — CHỌN VAI TRÒ
    // ====================================================================

    if (selectedRole.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "🍽️", fontSize = 64.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "EAT WHEN HUNGRY RESTAURANT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E40AF),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Chào mừng bạn đến với hệ thống đặt bàn",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(text = "Bạn là ai?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Quản lý / Lễ tân
            Button(
                onClick = { selectedRole = "admin"; resetForm() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👔  Quản lý / Lễ tân", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Toàn quyền quản lý hệ thống", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Nhân viên
            Button(
                onClick = { selectedRole = "employee"; resetForm() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nhân viên", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Bàn, món ăn, thanh toán và tra cứu khách", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Khách hàng
            Button(
                onClick = { selectedRole = "customer"; resetForm() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👤  Khách hàng", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Đặt bàn và xem đơn của bạn", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        return
    }

    // ====================================================================
    // UI — ĐĂNG NHẬP / ĐĂNG KÝ
    // ====================================================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedRole) {
                            "customer" -> "Khách hàng"
                            "employee" -> "Nhân viên"
                            else       -> "Quản lý / Lễ tân"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        selectedRole   = ""
                        isRegisterMode = false
                        resetForm()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRegisterMode) "Đăng ký tài khoản" else "Đăng nhập",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Xác thực qua máy chủ — mật khẩu được mã hóa an toàn",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Trường bổ sung cho đăng ký ──
            if (isRegisterMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ tên") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Tên đăng nhập ──
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Tên đăng nhập") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Mật khẩu (có nút hiện/ẩn) ──
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Ẩn mật khẩu" else "Hiện mật khẩu"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Thông báo lỗi từ server ──
            if (errorMessage.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Nút đăng nhập / đăng ký ──
            Button(
                onClick = {
                    if (isRegisterMode) registerAccount() else loginAccount()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (selectedRole) {
                        "customer" -> Color(0xFF34C759)
                        "employee" -> Color(0xFF007AFF)
                        else       -> Color(0xFF1E40AF)
                    }
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        if (isRegisterMode) "Đăng ký" else "Đăng nhập",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Chuyển giữa đăng nhập / đăng ký ──
            Text(
                text = if (isRegisterMode) "Đã có tài khoản? Đăng nhập"
                       else "Chưa có tài khoản? Đăng ký",
                color = Color(0xFF007AFF),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    isRegisterMode = !isRegisterMode
                    password = ""
                    errorMessage = ""
                }
            )
        }
    }
}
