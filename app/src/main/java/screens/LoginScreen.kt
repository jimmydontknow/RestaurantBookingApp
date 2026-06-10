package com.example.restaurantbookingapp.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    val context = LocalContext.current

    var selectedRole by remember {
        mutableStateOf("")
    }

    var isRegisterMode by remember {
        mutableStateOf(false)
    }

    var fullName by remember {
        mutableStateOf("")
    }

    var phoneNumber by remember {
        mutableStateOf("")
    }

    var username by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    fun resetForm() {
        fullName = ""
        phoneNumber = ""
        username = ""
        password = ""
    }

    fun registerAccount() {

        if (
            fullName.isBlank() ||
            phoneNumber.isBlank() ||
            username.isBlank() ||
            password.isBlank()
        ) {

            Toast.makeText(
                context,
                "Vui lòng nhập đầy đủ thông tin!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val prefs =
            context.getSharedPreferences(
                "APP_USERS",
                Context.MODE_PRIVATE
            )

        val key = "${selectedRole}_$username"

        if (prefs.contains("${key}_password")) {

            Toast.makeText(
                context,
                "Tài khoản đã tồn tại!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        prefs.edit()
            .putString("${key}_fullName", fullName)
            .putString("${key}_phoneNumber", phoneNumber)
            .putString("${key}_password", password)
            .putString("${key}_role", selectedRole)

            // AUTO LOGIN
            .putString("current_username", username)
            .putString("current_fullName", fullName)
            .putString("current_phoneNumber", phoneNumber)
            .putString("current_role", selectedRole)

            .apply()

        Toast.makeText(
            context,
            "Đăng ký thành công!",
            Toast.LENGTH_SHORT
        ).show()

        when (selectedRole) {

            "customer" -> {
                navController.navigate("customer_main")
            }

            "admin" -> {
                navController.navigate("admin_main")
            }

            "employee" -> {
                navController.navigate("employee_main")
            }
        }
    }

    fun loginAccount() {

        if (
            username.isBlank() ||
            password.isBlank()
        ) {

            Toast.makeText(
                context,
                "Vui lòng nhập tài khoản và mật khẩu!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val prefs =
            context.getSharedPreferences(
                "APP_USERS",
                Context.MODE_PRIVATE
            )

        val key = "${selectedRole}_$username"

        val savedPassword =
            prefs.getString(
                "${key}_password",
                null
            )

        if (
            savedPassword == null ||
            savedPassword != password
        ) {

            Toast.makeText(
                context,
                "Sai tài khoản hoặc mật khẩu!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val savedName =
            prefs.getString(
                "${key}_fullName",
                username
            ) ?: username

        val savedPhone =
            prefs.getString(
                "${key}_phoneNumber",
                ""
            ) ?: ""

        prefs.edit()
            .putString("current_username", username)
            .putString("current_fullName", savedName)
            .putString("current_phoneNumber", savedPhone)
            .putString("current_role", selectedRole)
            .apply()

        when (selectedRole) {

            "customer" -> {
                navController.navigate("customer_main")
            }

            "admin" -> {
                navController.navigate("admin_main")
            }

            "employee" -> {
                navController.navigate("employee_main")
            }
        }
    }

    // ====================================================
    // CHỌN VAI TRÒ
    // ====================================================

    if (selectedRole.isEmpty()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "🍽️",
                fontSize = 64.sp,
                textAlign = TextAlign.Center
            )

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

            Text(
                text = "Bạn là ai?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedRole = "admin"
                    resetForm()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),

                shape = RoundedCornerShape(12.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E40AF)
                )
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "👔  Quản lý / Lễ tân",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        "Toàn quyền quản lý hệ thống",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedRole = "employee"
                    resetForm()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nhân viên",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Bàn, món ăn, thanh toán và tra cứu khách",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedRole = "customer"
                    resetForm()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),

                shape = RoundedCornerShape(12.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759)
                )
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "👤  Khách hàng",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        "Đặt bàn và xem đơn của bạn",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    // ====================================================
    // LOGIN / REGISTER
    // ====================================================

    else {

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {

                        Text(
                            when (selectedRole) {
                                "customer" -> "Khách hàng"
                                "employee" -> "Nhân viên"
                                else -> "Quản lý / Lễ tân"
                            },

                            fontWeight = FontWeight.Bold
                        )
                    },

                    navigationIcon = {

                        IconButton(
                            onClick = {

                                selectedRole = ""
                                isRegisterMode = false
                                resetForm()
                            }
                        ) {

                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
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
                    .padding(24.dp),

                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text =
                        if (isRegisterMode)
                            "Đăng ký tài khoản"
                        else
                            "Đăng nhập",

                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isRegisterMode) {

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                        },
                        label = {
                            Text("Họ tên")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                        },
                        label = {
                            Text("Số điện thoại")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                    },
                    label = {
                        Text("Tên đăng nhập")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Mật khẩu")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation =
                        PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(

                    onClick = {

                        if (isRegisterMode)
                            registerAccount()
                        else
                            loginAccount()
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape = RoundedCornerShape(12.dp),

                    colors = ButtonDefaults.buttonColors(

                        containerColor =
                            if (selectedRole == "customer")
                                Color(0xFF34C759)
                            else
                                Color(0xFF1E40AF)
                    )

                ) {

                    Text(
                        if (isRegisterMode)
                            "Đăng ký"
                        else
                            "Đăng nhập",

                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(

                    text =
                        if (isRegisterMode)
                            "Đã có tài khoản? Đăng nhập"
                        else
                            "Chưa có tài khoản? Đăng ký",

                    color = Color(0xFF007AFF),

                    fontWeight = FontWeight.Medium,

                    modifier = Modifier.clickable {

                        isRegisterMode = !isRegisterMode
                        password = ""
                    }
                )
            }
        }
    }
}