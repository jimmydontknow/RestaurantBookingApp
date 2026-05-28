package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

import android.content.Context
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMainScreen(rootNavController: NavController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = currentRoute == "customer_booking",
                    onClick = { navController.navigate("customer_booking") },
                    label = { Text("Đặt bàn") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )

                NavigationBarItem(
                    selected = currentRoute == "customer_orders",
                    onClick = { navController.navigate("customer_orders") },
                    label = { Text("Đơn của tôi") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )

                NavigationBarItem(
                    selected = currentRoute == "customer_tables",
                    onClick = { navController.navigate("customer_tables") },
                    label = { Text("Sơ đồ bàn") },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "customer_booking",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("customer_booking") {
                CustomerBookingScreen(
                    onBookingSuccess = { navController.navigate("customer_orders") },
                    onBack = { rootNavController.popBackStack() }
                )
            }

            composable("customer_orders") {
                CustomerOrdersScreen()
            }

            composable("customer_tables") {
                CustomerTableViewScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingScreen(
    onBookingSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("APP_USERS", android.content.Context.MODE_PRIVATE)
    val loggedInName = prefs.getString("current_fullName", "") ?: ""
    val loggedInPhone = prefs.getString("current_phoneNumber", "") ?: ""
    val loggedInUsername = prefs.getString("current_username", "") ?: ""

    var guestName by remember {
        mutableStateOf(loggedInName)
    }

    var phoneNumber by remember {
        mutableStateOf(loggedInPhone)
    }
    var guestCount by remember { mutableStateOf("") }
    var selectedZone by remember { mutableStateOf("A") }
    var bookingDate by remember { mutableStateOf("") }
    var bookingTime by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("500000") }
    var isLoading by remember { mutableStateOf(false) }
    var expandedZone by remember { mutableStateOf(false) }
    var expandedTime by remember { mutableStateOf(false) }

    val timeSlots = listOf(
        "10:00", "10:30", "11:00", "11:30", "12:00", "12:30",
        "13:00", "13:30", "17:00", "17:30", "18:00", "18:30",
        "19:00", "19:30", "20:00", "20:30", "21:00"
    )

    val guestCountInt = guestCount.toIntOrNull() ?: 0
    val maxPerTable = if (selectedZone == "A") 10 else 20
    val tablesNeeded =
        if (guestCountInt > 0) kotlin.math.ceil(guestCountInt.toDouble() / maxPerTable).toInt()
        else 0

    val tableSummary = when {
        tablesNeeded == 0 -> ""
        tablesNeeded == 1 -> "1 Bàn đơn Khu $selectedZone"
        tablesNeeded <= 3 -> "Gộp $tablesNeeded bàn Khu $selectedZone"
        else -> "VƯỢT GIỚI HẠN"
    }

    val isOverLimit = tablesNeeded > 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt bàn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Thông tin đặt bàn", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Vui lòng điền đầy đủ thông tin", fontSize = 13.sp, color = Color.Gray)
            }

            item {
                OutlinedTextField(
                    value = loggedInName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Họ và tên") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = loggedInPhone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = bookingDate,
                    onValueChange = { bookingDate = it },
                    label = { Text("Ngày đặt (YYYY-MM-DD)") },
                    placeholder = { Text("VD: 2025-12-31") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = bookingTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Giờ đặt bàn") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { expandedTime = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )

                    DropdownMenu(
                        expanded = expandedTime,
                        onDismissRequest = { expandedTime = false }
                    ) {
                        timeSlots.forEach { time ->
                            DropdownMenuItem(
                                text = { Text(time) },
                                onClick = {
                                    bookingTime = time
                                    expandedTime = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedZone == "A")
                            "Khu A — Phòng lạnh (tối đa 10 người/bàn)"
                        else
                            "Khu B — Ngoài trời (tối đa 20 người/bàn)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Khu vực") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { expandedZone = true }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )

                    DropdownMenu(
                        expanded = expandedZone,
                        onDismissRequest = { expandedZone = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Khu A — Phòng lạnh") },
                            onClick = {
                                selectedZone = "A"
                                expandedZone = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Khu B — Ngoài trời") },
                            onClick = {
                                selectedZone = "B"
                                expandedZone = false
                            }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = guestCount,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) guestCount = it
                    },
                    label = { Text("Số lượng khách") },
                    isError = isOverLimit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )

                if (tableSummary.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (isOverLimit) Color(0xFFFFE5E5)
                                else Color(0xFFE5F1FF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text =
                                if (isOverLimit) "Vượt giới hạn gộp bàn!"
                                else "Phương án: $tableSummary",
                            modifier = Modifier.padding(12.dp),
                            color =
                                if (isOverLimit) Color.Red
                                else Color(0xFF007AFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = depositAmount,
                    onValueChange = { depositAmount = it },
                    label = { Text("Tiền đặt cọc (VND)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Tiền cọc sẽ được trừ vào tổng hóa đơn khi thanh toán.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Yêu cầu đặc biệt") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Chính sách hủy bàn",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "• Hủy trước 2 tiếng: Hoàn 100% tiền cọc",
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )

                        Text(
                            "• Hủy sau 2 tiếng: Mất tiền cọc",
                            fontSize = 12.sp,
                            color = Color(0xFF856404)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        when {
                            guestName.isEmpty() ->
                                Toast.makeText(context, "Nhập tên!", Toast.LENGTH_SHORT).show()

                            phoneNumber.isEmpty() ->
                                Toast.makeText(context, "Nhập SĐT!", Toast.LENGTH_SHORT).show()

                            bookingDate.isEmpty() ->
                                Toast.makeText(context, "Chọn ngày!", Toast.LENGTH_SHORT).show()

                            bookingTime.isEmpty() ->
                                Toast.makeText(context, "Chọn giờ!", Toast.LENGTH_SHORT).show()

                            tableSummary.isEmpty() ->
                                Toast.makeText(context, "Nhập số khách!", Toast.LENGTH_SHORT).show()

                            isOverLimit ->
                                Toast.makeText(context, "Số khách vượt giới hạn!", Toast.LENGTH_SHORT).show()

                            else -> {
                                isLoading = true

                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        var conn: HttpURLConnection? = null

                                        try {
                                            val url = URL("http://10.0.2.2:3000/api/bookings")
                                            conn = url.openConnection() as HttpURLConnection

                                            conn.requestMethod = "POST"
                                            conn.setRequestProperty(
                                                "Content-Type",
                                                "application/json; utf-8"
                                            )
                                            conn.connectTimeout = 5000
                                            conn.readTimeout = 5000
                                            conn.doOutput = true

                                            val body = JSONObject().apply {
                                                put("customerUsername", loggedInUsername)
                                                put("guestName", loggedInName)
                                                put("phoneNumber", loggedInPhone)
                                                put("tableNumber", tableSummary)
                                                put("depositAmount", depositAmount.toIntOrNull() ?: 0)
                                                put("note", note)
                                                put("bookingDate", bookingDate)
                                                put("bookingTime", bookingTime)
                                            }.toString()

                                            conn.outputStream.use {
                                                it.write(body.toByteArray(Charsets.UTF_8))
                                            }

                                            val code = conn.responseCode

                                            withContext(Dispatchers.Main) {
                                                isLoading = false

                                                if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                                                    Toast.makeText(
                                                        context,
                                                        "Đặt bàn thành công!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    onBookingSuccess()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Lỗi đặt bàn: $code",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }

                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                Toast.makeText(
                                                    context,
                                                    "Lỗi: ${e.localizedMessage}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } finally {
                                            conn?.disconnect()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isOverLimit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            "Xác nhận đặt bàn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("APP_USERS", android.content.Context.MODE_PRIVATE)
    val currentUsername = prefs.getString("current_username", "") ?: ""
    val currentPhone = prefs.getString("current_phoneNumber", "") ?: ""

    val bookingList = remember { mutableStateListOf<BookingItem>() }
    var isLoading by remember { mutableStateOf(false) }

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    fun loadMyBookings() {
        isLoading = true

        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null

            try {
                val url = URL("http://10.0.2.2:3000/api/bookings")
                conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val str = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(str).getJSONArray("bookings")

                    val result = mutableListOf<BookingItem>()

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        val bookingUsername = obj.optString("customerUsername", "")
                        val phoneFromServer = obj.optString(
                            "phoneNumber",
                            obj.optString("guestPhone", "")
                        )

                        if (
                            bookingUsername == currentUsername ||
                            phoneFromServer == currentPhone
                        ) {
                            result.add(
                                BookingItem(
                                    id = obj.optString("id"),
                                    bookingCode = obj.optString("bookingCode"),
                                    guestName = obj.optString("guestName"),
                                    guestPhone = phoneFromServer,
                                    tableSummary = obj.optString(
                                        "tableNumber",
                                        obj.optString("tableSummary", "")
                                    ),
                                    totalAmount = obj.optDouble(
                                        "depositAmount",
                                        obj.optDouble("totalAmount", 0.0)
                                    ),
                                    status = obj.optString("status", "pending"),
                                    bookingDate = obj.optString("bookingDate", ""),
                                    bookingTime = obj.optString("bookingTime", "")
                                )
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        bookingList.clear()
                        bookingList.addAll(result)
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Lỗi tải đơn: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun cancelBooking(booking: BookingItem) {
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null

            try {
                val url = URL("http://10.0.2.2:3000/api/bookings/status")
                conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("id", booking.id.toIntOrNull() ?: booking.id)
                    put("status", "cancelled")
                }.toString()

                conn.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode

                withContext(Dispatchers.Main) {
                    if (code == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(
                            context,
                            "Đã hủy đặt bàn thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadMyBookings()
                    } else {
                        Toast.makeText(
                            context,
                            "Lỗi hủy bàn: $code",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMyBookings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Đơn đặt bàn của tôi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Tự động hiển thị đơn theo tài khoản đang đăng nhập",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else if (bookingList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Bạn chưa có đơn đặt bàn nào.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                lazyItems(bookingList) { booking ->
                    val (statusText, statusColor) = when (booking.status) {
                        "pending" -> "Chờ xếp bàn" to Color(0xFFFF9500)
                        "checked_in" -> "Đang phục vụ" to Color(0xFF28A745)
                        "checked_out" -> "Đã hoàn thành" to Color(0xFF636366)
                        "cancelled" -> "Đã hủy" to Color(0xFF8E8E93)
                        else -> "Không xác định" to Color.Gray
                    }

                    val canCancel = booking.status == "pending"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    booking.guestName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Text(
                                    booking.bookingCode,
                                    color = Color(0xFF007AFF),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                booking.tableSummary,
                                color = Color(0xFFFF9500),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "🕒 ${booking.bookingDate} • ${booking.bookingTime}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            HorizontalDivider(color = Color(0xFFE5E5EA))

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currencyFormatter.format(booking.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34C759)
                                    )

                                    if (canCancel) {
                                        OutlinedButton(
                                            onClick = { cancelBooking(booking) },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color.Red
                                            ),
                                            border = BorderStroke(1.dp, Color.Red),
                                            contentPadding = PaddingValues(
                                                horizontal = 10.dp,
                                                vertical = 4.dp
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Hủy bàn", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerTableViewScreen() {
    val tableList = remember { mutableStateListOf<TableData>() }

    var isLoading by remember { mutableStateOf(true) }

    val zoneATables = tableList.filter { it.zone == "A" }
    val zoneBTables = tableList.filter { it.zone == "B" }
    val availableCount = tableList.count { it.status == "available" }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null

            try {
                val url = URL("http://10.0.2.2:3000/api/tables")
                conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val str = conn.inputStream.bufferedReader().use { it.readText() }
                    val arr = JSONObject(str).getJSONArray("tables")
                    val fetched = mutableListOf<TableData>()

                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        val tNum = item.optString("tableNumber", "")

                        fetched.add(
                            TableData(
                                id = item.getString("id"),
                                tableNumber = tNum,
                                tableName = "BÀN $tNum",
                                status = item.getString("status"),
                                zone = item.optString(
                                    "zone",
                                    if (tNum.startsWith("A")) "A" else "B"
                                )
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        tableList.clear()
                        tableList.addAll(fetched)
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Sơ đồ bàn ăn",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Còn $availableCount bàn trống • Chỉ xem — không thể thay đổi",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF007AFF))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFE2F0D9), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trống", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFD9E1F2), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đang dùng", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFFFF3CD), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đã đặt", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Khu A — Phòng lạnh",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val aRows = kotlin.math.ceil(zoneATables.size / 4.0).toInt()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((aRows * 80).dp),
                    userScrollEnabled = false
                ) {
                    gridItems(zoneATables) { table ->
                        CustomerTableCell(table)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Khu B — Ngoài trời",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val bRows = kotlin.math.ceil(zoneBTables.size / 4.0).toInt()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((bRows * 80).dp),
                    userScrollEnabled = false
                ) {
                    gridItems(zoneBTables) { table ->
                        CustomerTableCell(table)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun CustomerTableCell(table: TableData) {

    val bgColor = when {

        table.status == "occupied" ->
            Color(0xFFD9E1F2)

        table.status != "available" ->
            Color(0xFFFFF3CD)

        else ->
            Color(0xFFE2F0D9)
    }

    val label = when {

        table.status == "occupied" ->
            "Đang dùng"

        table.status != "available" ->
            "Đã đặt"

        else ->
            "Trống"
    }

    val labelColor = when {

        table.status == "occupied" ->
            Color(0xFF1F4E79)

        table.status != "available" ->
            Color(0xFF856404)

        else ->
            Color(0xFF385723)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                bgColor,
                RoundedCornerShape(8.dp)
            ),

        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                table.tableNumber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                label,
                fontSize = 10.sp,
                color = labelColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}