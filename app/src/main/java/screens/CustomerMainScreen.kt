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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import kotlinx.coroutines.delay
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

                NavigationBarItem(
                    selected = false,
                    onClick = {
                        val prefs = rootNavController.context.getSharedPreferences(
                            "APP_USERS",
                            Context.MODE_PRIVATE
                        )
                        prefs.edit()
                            .remove("current_username")
                            .remove("current_fullName")
                            .remove("current_phoneNumber")
                            .remove("current_role")
                            .apply()
                        rootNavController.navigate("login") {
                            popUpTo("customer_main") { inclusive = true }
                        }
                    },
                    label = { Text("Đăng xuất") },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
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
                CustomerOrdersScreen(navController)
            }

            composable("customer_tables") {
                CustomerTableViewScreen()
            }

            composable("menu/{bookingId}") { backStackEntry ->
                val bookingId = backStackEntry.arguments
                    ?.getString("bookingId")
                    ?.toIntOrNull()
                    ?: 0
                if (bookingId > 0) {
                    CustomerMenuScreen(navController, bookingId)
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}


data class CustomerMenuFood(
    val foodId: Int,
    val foodName: String,
    val category: String,
    val price: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingScreen(
    onBookingSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("APP_USERS", android.content.Context.MODE_PRIVATE)
    val savedName = prefs.getString("current_fullName", "") ?: ""
    val savedPhone = prefs.getString("current_phoneNumber", "") ?: ""
    val loggedInUsername = prefs.getString("current_username", "") ?: ""

    var isWalkIn by rememberSaveable { mutableStateOf(savedName.isBlank() && savedPhone.isBlank()) }  //Form đặt bàn khách giữ tên, SĐT, ngày giờ, khu vực, bàn và món đã chọn khi xoay màn hình bằng
    var guestName by rememberSaveable { mutableStateOf(savedName) }
    var phoneNumber by rememberSaveable { mutableStateOf(savedPhone) }
    var memberText by rememberSaveable { mutableStateOf("Nhập SĐT để kiểm tra khách cũ") }
    var discountPercent by rememberSaveable { mutableStateOf(0.0) }
    var guestCount by rememberSaveable { mutableStateOf("") }
    var selectedZone by rememberSaveable { mutableStateOf("A") }
    var bookingDate by rememberSaveable { mutableStateOf("") }
    var bookingTime by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var depositAmount by rememberSaveable { mutableStateOf("200000") }
    var expandedTime by remember { mutableStateOf(false) }
    var expandedZone by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingTables by remember { mutableStateOf(true) }
    var isLoadingMenu by remember { mutableStateOf(true) }

    val allTables = remember { mutableStateListOf<TableData>() }
    val selectedTableIdsSaver = Saver<SnapshotStateList<String>, ArrayList<String>>(
        save = { ArrayList(it) },
        restore = { it.toMutableStateList() }
    )
    val selectedTableIds = rememberSaveable(saver = selectedTableIdsSaver) {
        mutableStateListOf<String>()
    }
    val foods = remember { mutableStateListOf<CustomerMenuFood>() }
    val quantitiesSaver = Saver<SnapshotStateMap<Int, Int>, ArrayList<String>>(
        save = { values ->
            ArrayList(values.map { (foodId, quantity) -> "$foodId:$quantity" })
        },
        restore = { values ->
            mutableStateMapOf<Int, Int>().apply {
                values.forEach { item ->
                    val parts = item.split(":", limit = 2)
                    val foodId = parts.getOrNull(0)?.toIntOrNull()
                    val quantity = parts.getOrNull(1)?.toIntOrNull()
                    if (foodId != null && quantity != null) put(foodId, quantity)
                }
            }
        }
    )
    val quantities = rememberSaveable(saver = quantitiesSaver) {
        mutableStateMapOf<Int, Int>()
    }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val timeSlots = listOf("10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00", "20:30", "21:00")

    val guestCountInt = guestCount.toIntOrNull() ?: 0
    val maxPerTable = if (selectedZone == "A") 10 else 20
    val tablesNeeded = if (guestCountInt > 0) kotlin.math.ceil(guestCountInt.toDouble() / maxPerTable).toInt() else 0
    val isOverLimit = tablesNeeded > 3
    val zoneTables = allTables.filter { it.zone == selectedZone }
    val selectedTables = allTables.filter { it.id in selectedTableIds }
    val selectedFoodCount = quantities.values.sum()
    val menuTotal = foods.sumOf { it.price * (quantities[it.foodId] ?: 0) }
    val discountAmount = menuTotal * discountPercent / 100.0
    val tableSummary = when {
        selectedTables.isEmpty() -> "Chưa chọn bàn"
        selectedTables.size == 1 -> "1 Bàn đơn ${selectedTables.first().tableNumber} Khu $selectedZone"
        else -> "Gộp ${selectedTables.size} bàn ${selectedTables.joinToString("+") { it.tableNumber }} Khu $selectedZone"
    }

    suspend fun lookupMember() = withContext(Dispatchers.IO) {
        if (phoneNumber.isBlank()) return@withContext
        var conn: HttpURLConnection? = null
        try {
            conn = URL("http://10.0.2.2:3001/api/customers/lookup?phone=$phoneNumber").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val obj = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val found = obj.optBoolean("found", false)
                discountPercent = obj.optDouble("discountPercent", 0.0)
                val visits = obj.optInt("visitCount", 0)
                val spent = obj.optDouble("totalSpent", 0.0)
                withContext(Dispatchers.Main) {
                    memberText = if (found) "Khách cũ: $visits lần • đã chi ${currencyFormatter.format(spent)} • giảm ${discountPercent.toInt()}%" else "SĐT chưa có lịch sử, đặt nhanh như khách vãng lai"
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) { memberText = "Chưa kiểm tra được SĐT" }
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun loadTables() = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL("http://10.0.2.2:3001/api/tables").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val arr = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONArray("tables")
                val fetched = mutableListOf<TableData>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val number = obj.optString("tableNumber", obj.optString("TableNumber", ""))
                    val rawStatus = obj.optString("status", obj.optString("CurrentStatus", "available"))
                    val status = when {
                        rawStatus.equals("occupied", true) || rawStatus.equals("Đang dùng", true) -> "occupied"
                        rawStatus.equals("booked", true) || rawStatus.equals("Đã đặt", true) -> "booked"
                        else -> "available"
                    }
                    fetched.add(TableData(obj.optString("id", obj.optString("TableID", "")), number, "BÀN $number", status, obj.optString("zone", if (number.startsWith("A")) "A" else "B")))
                }
                withContext(Dispatchers.Main) {
                    allTables.clear()
                    allTables.addAll(fetched)
                    isLoadingTables = false
                }
            } else withContext(Dispatchers.Main) { isLoadingTables = false }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                isLoadingTables = false
            }
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun loadMenu() = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL("http://10.0.2.2:3001/api/menu").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val arr = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONArray("items")
                val fetched = mutableListOf<CustomerMenuFood>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    fetched.add(CustomerMenuFood(item.optInt("foodId", item.optInt("FoodID")), item.optString("foodName", item.optString("FoodName", "")), item.optString("category", item.optString("Category", "")), item.optDouble("price", item.optDouble("Price", 0.0))))
                }
                withContext(Dispatchers.Main) {
                    foods.clear()
                    foods.addAll(fetched)
                    isLoadingMenu = false
                }
            } else withContext(Dispatchers.Main) { isLoadingMenu = false }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                isLoadingMenu = false
            }
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun markTablesBooked() = withContext(Dispatchers.IO) {
        selectedTables.forEach { table ->
            var conn: HttpURLConnection? = null
            try {
                conn = URL("http://10.0.2.2:3001/api/tables/status").openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true
                val body = JSONObject().apply {
                    put("id", table.id.toIntOrNull() ?: table.id)
                    put("status", "booked")
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
            } finally {
                conn?.disconnect()
            }
        }
    }

    suspend fun submitFoods(bookingId: Int) = withContext(Dispatchers.IO) {
        foods.filter { (quantities[it.foodId] ?: 0) > 0 }.forEach { food ->
            var conn: HttpURLConnection? = null
            try {
                conn = URL("http://10.0.2.2:3001/api/order-items/add").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.doOutput = true
                val body = JSONObject().apply {
                    put("bookingId", bookingId)
                    put("foodId", food.foodId)
                    put("quantity", quantities[food.foodId] ?: 0)
                    put("unitPrice", food.price)
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
            } finally {
                conn?.disconnect()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTables()
        loadMenu()
        if (phoneNumber.isNotBlank()) lookupMember()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt bàn", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color(0xFFF8F9FA)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("Thông tin đặt bàn", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Khách vãng lai có thể đặt nhanh bằng tên và SĐT", fontSize = 13.sp, color = Color.Gray)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isWalkIn, onCheckedChange = { isWalkIn = it })
                    Text("Khách vãng lai / tiện đường")
                }
            }
            item { OutlinedTextField(value = guestName, onValueChange = { guestName = it }, readOnly = !isWalkIn && savedName.isNotBlank(), label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) }
            item {
                OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it.filter { c -> c.isDigit() } }, readOnly = !isWalkIn && savedPhone.isNotBlank(), label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), trailingIcon = {
                    TextButton(onClick = { coroutineScope.launch { lookupMember() } }) { Text("Tìm") }
                })
                Text(memberText, fontSize = 12.sp, color = if (discountPercent > 0) Color(0xFF34C759) else Color.Gray)
            }
            item { OutlinedTextField(value = bookingDate, onValueChange = { bookingDate = it }, label = { Text("Ngày đặt (YYYY-MM-DD)") }, placeholder = { Text("VD: 2026-06-06") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = bookingTime, onValueChange = {}, readOnly = true, label = { Text("Giờ đặt bàn") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedTime = true }) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                    DropdownMenu(expanded = expandedTime, onDismissRequest = { expandedTime = false }) {
                        timeSlots.forEach { time -> DropdownMenuItem(text = { Text(time) }, onClick = { bookingTime = time; expandedTime = false }) }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = if (selectedZone == "A") "Khu A — Phòng lạnh" else "Khu B — Ngoài trời", onValueChange = {}, readOnly = true, label = { Text("Khu vực") }, trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { expandedZone = true }) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                    DropdownMenu(expanded = expandedZone, onDismissRequest = { expandedZone = false }) {
                        DropdownMenuItem(text = { Text("Khu A — Phòng lạnh") }, onClick = { selectedZone = "A"; selectedTableIds.clear(); expandedZone = false })
                        DropdownMenuItem(text = { Text("Khu B — Ngoài trời") }, onClick = { selectedZone = "B"; selectedTableIds.clear(); expandedZone = false })
                    }
                }
            }
            item { OutlinedTextField(value = guestCount, onValueChange = { if (it.all { c -> c.isDigit() }) { guestCount = it; selectedTableIds.clear() } }, label = { Text("Số lượng khách") }, isError = isOverLimit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sơ đồ bàn", fontWeight = FontWeight.Bold)
                        Text("Cần $tablesNeeded bàn • Đã chọn ${selectedTables.size} bàn", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(tableSummary, color = Color(0xFFFF9500), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isLoadingTables) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        else {
                            val rows = kotlin.math.ceil(zoneTables.size / 4.0).toInt().coerceAtLeast(1)
                            LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height((rows * 76).dp), userScrollEnabled = false) {
                                gridItems(zoneTables) { table ->
                                    val isSelected = selectedTables.any { it.id == table.id }
                                    val canPick = table.status == "available"
                                    val bg = when {
                                        isSelected -> Color(0xFF007AFF)
                                        table.status == "occupied" -> Color(0xFFD9E1F2)
                                        table.status != "available" -> Color(0xFFFFF3CD)
                                        else -> Color(0xFFE2F0D9)
                                    }
                                    val fg = if (isSelected) Color.White else Color.Black
                                    Box(modifier = Modifier.fillMaxWidth().height(68.dp).background(bg, RoundedCornerShape(8.dp)).clickable(enabled = canPick && tablesNeeded > 0) {
                                        if (isSelected) selectedTableIds.remove(table.id)
                                        else if (selectedTables.size < tablesNeeded) selectedTableIds.add(table.id)
                                        else Toast.makeText(context, "Đã đủ $tablesNeeded bàn", Toast.LENGTH_SHORT).show()
                                    }, contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(table.tableNumber, color = fg, fontWeight = FontWeight.Bold)
                                            Text(if (isSelected) "Đang chọn" else if (table.status == "available") "Trống" else "Đã đặt", color = fg, fontSize = 10.sp)
                                        }
                                    }
}
                            }
                        }
                    }
                }
            }
            item { OutlinedTextField(value = depositAmount, onValueChange = {}, readOnly = true, label = { Text("Tiền đặt cọc cố định (VND)") }, supportingText = { Text("Tiền cọc sẽ được trừ khi thanh toán hóa đơn.") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Chọn món ăn", fontWeight = FontWeight.Bold)
                        Text("$selectedFoodCount món • ${currencyFormatter.format(menuTotal)}", color = Color.Gray, fontSize = 12.sp)
                        if (discountPercent > 0) Text("Ưu đãi hội viên dự kiến: -${currencyFormatter.format(discountAmount)}", color = Color(0xFF34C759), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isLoadingMenu) CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        else foods.forEach { food ->
                            val quantity = quantities[food.foodId] ?: 0
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(food.foodName, fontWeight = FontWeight.SemiBold)
                                    Text("${food.category} • ${currencyFormatter.format(food.price)}", color = Color.Gray, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(onClick = { if (quantity > 0) quantities[food.foodId] = quantity - 1 }, modifier = Modifier.size(32.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp)) { Text("-") }
                                    Text(quantity.toString(), modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Button(onClick = { quantities[food.foodId] = quantity + 1 }, modifier = Modifier.size(32.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)), shape = RoundedCornerShape(8.dp)) { Text("+", color = Color.White) }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E5EA))
                        }
                    }
                }
            }
            item { OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Yêu cầu đặc biệt") }, minLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) }
            item {
                Button(
                    onClick = {
                        when {
                            guestName.isBlank() -> Toast.makeText(context, "Nhập tên!", Toast.LENGTH_SHORT).show()
                            phoneNumber.isBlank() -> Toast.makeText(context, "Nhập SĐT!", Toast.LENGTH_SHORT).show()
                            bookingDate.isEmpty() -> Toast.makeText(context, "Chọn ngày!", Toast.LENGTH_SHORT).show()
                            bookingTime.isEmpty() -> Toast.makeText(context, "Chọn giờ!", Toast.LENGTH_SHORT).show()
                            guestCountInt <= 0 -> Toast.makeText(context, "Nhập số khách!", Toast.LENGTH_SHORT).show()
                            isOverLimit -> Toast.makeText(context, "Số khách vượt giới hạn!", Toast.LENGTH_SHORT).show()
                            selectedTables.size != tablesNeeded -> Toast.makeText(context, "Vui lòng chọn đúng $tablesNeeded bàn!", Toast.LENGTH_SHORT).show()
                            else -> {
                                isLoading = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    var conn: HttpURLConnection? = null
                                    try {
                                        conn = URL("http://10.0.2.2:3001/api/bookings").openConnection() as HttpURLConnection
                                        conn.requestMethod = "POST"
                                        conn.setRequestProperty("Content-Type", "application/json; utf-8")
                                        conn.doOutput = true
                                        val body = JSONObject().apply {
                                            put("customerUsername", if (isWalkIn) "" else loggedInUsername)
                                            put("guestName", guestName)
                                            put("phoneNumber", phoneNumber)
                                            put("tableNumber", tableSummary)
                                            put("depositAmount", depositAmount.toIntOrNull() ?: 0)
                                            put("note", note)
                                            put("bookingDate", bookingDate)
                                            put("bookingTime", bookingTime)
                                        }.toString()
                                        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                                        val code = conn.responseCode
                                        val response = if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
                                        if (code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_OK) {
                                            val bookingId = JSONObject(response).optInt("bookingId", 0)
                                            if (bookingId > 0) {
                                                submitFoods(bookingId)
                                                markTablesBooked()
                                            }
                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                Toast.makeText(context, "Đặt bàn thành công!", Toast.LENGTH_SHORT).show()
                                                onBookingSuccess()
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                isLoading = false
                                                Toast.makeText(context, "Lỗi đặt bàn: $code", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    } finally {
                                        conn?.disconnect()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isOverLimit,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Xác nhận đặt bàn", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(navController: NavController) {
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
                val url = URL("http://10.0.2.2:3001/api/bookings")
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

    fun deleteBooking(booking: BookingItem) {
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3001/api/bookings/${booking.id}")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                withContext(Dispatchers.Main) {
                    if (code == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(context, "Đã xóa đơn đã hủy!", Toast.LENGTH_SHORT).show()
                        loadMyBookings()
                    } else {
                        Toast.makeText(context, "Lỗi xóa đơn: $code", Toast.LENGTH_LONG).show()
                    }
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
                val url = URL("http://10.0.2.2:3001/api/bookings/status")
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
                    val canDelete = booking.status == "cancelled"

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

                                    if (booking.id.isNotBlank() && booking.status != "cancelled") {
                                        Button(
                                            onClick = { navController.navigate("menu/${booking.id}") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Gọi món", fontSize = 11.sp, color = Color.White)
                                        }
                                    }

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
fun CustomerMenuScreen(navController: NavController, bookingId: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val foods = remember { mutableStateListOf<CustomerMenuFood>() }
    val quantities = remember { mutableStateMapOf<Int, Int>() }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val selectedFoods = foods.filter { (quantities[it.foodId] ?: 0) > 0 }
    val totalAmount = selectedFoods.sumOf { it.price * (quantities[it.foodId] ?: 0) }

    suspend fun loadMenu() = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL("http://10.0.2.2:3001/api/menu").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val arr = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONArray("items")
                val fetched = mutableListOf<CustomerMenuFood>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    fetched.add(CustomerMenuFood(item.optInt("foodId", item.optInt("FoodID")), item.optString("foodName", item.optString("FoodName", "")), item.optString("category", item.optString("Category", "")), item.optDouble("price", item.optDouble("Price", 0.0))))
                }
                conn.disconnect()
                conn = URL("http://10.0.2.2:3001/api/bookings/$bookingId/order-items").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val existing = mutableMapOf<Int, Int>()
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val orderArr = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONArray("items")
                    for (i in 0 until orderArr.length()) {
                        val item = orderArr.getJSONObject(i)
                        existing[item.optInt("foodId", item.optInt("FoodID"))] = item.optInt("quantity", item.optInt("Quantity", 0))
                    }
                }
                withContext(Dispatchers.Main) {
                    foods.clear()
                    foods.addAll(fetched)
                    quantities.clear()
                    quantities.putAll(existing.filterValues { it > 0 })
                    isLoading = false
                }
            } else withContext(Dispatchers.Main) { isLoading = false }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                isLoading = false
                Toast.makeText(context, "Lỗi menu: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } finally {
            conn?.disconnect()
        }
    }

    fun submitOrder() {
        isSubmitting = true
        val selectedOrderItems = selectedFoods.map { food ->
            food to (quantities[food.foodId] ?: 0)
        }
        coroutineScope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            var ok = false
            var successCount = 0
            try {
                conn = URL("http://10.0.2.2:3001/api/bookings/$bookingId/order-items").openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                ok = conn.responseCode == HttpURLConnection.HTTP_OK
            } finally {
                conn?.disconnect()
            }
            if (ok) {
                for ((food, quantity) in selectedOrderItems) {
                    conn = null
                    try {
                        conn = URL("http://10.0.2.2:3001/api/order-items/add").openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/json; utf-8")
                        conn.doOutput = true
                        val body = JSONObject().apply {
                            put("bookingId", bookingId)
                            put("foodId", food.foodId)
                            put("quantity", quantity)
                            put("unitPrice", food.price)
                        }.toString()
                        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                        if (conn.responseCode == HttpURLConnection.HTTP_OK || conn.responseCode == HttpURLConnection.HTTP_CREATED) successCount++
                    } finally {
                        conn?.disconnect()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                isSubmitting = false
                if (ok && successCount == selectedOrderItems.size) {
                    Toast.makeText(context, "Đã lưu món đã gọi", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else Toast.makeText(context, "Một số món chưa lưu được", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) { loadMenu() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chọn món Âu", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Tổng tiền", fontSize = 12.sp, color = Color.Gray)
                        Text(currencyFormatter.format(totalAmount), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF34C759))
                    }
                    Button(onClick = { submitOrder() }, enabled = !isSubmitting, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)), shape = RoundedCornerShape(10.dp)) {
                        Text(if (isSubmitting) "Đang lưu..." else "Lưu món", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            lazyItems(foods) { food ->
                val quantity = quantities[food.foodId] ?: 0
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(food.foodName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(food.category, color = Color.Gray, fontSize = 12.sp)
                            Text(currencyFormatter.format(food.price), color = Color(0xFFFF9500), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { if (quantity > 0) quantities[food.foodId] = quantity - 1 }, modifier = Modifier.size(34.dp), contentPadding = PaddingValues(0.dp), shape = RoundedCornerShape(8.dp)) { Text("-") }
                            Text(quantity.toString(), modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Button(onClick = { quantities[food.foodId] = quantity + 1 }, modifier = Modifier.size(34.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)), shape = RoundedCornerShape(8.dp)) { Text("+", color = Color.White) }
                        }
                    }
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
        while (true) {
            withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null

            try {
                val url = URL("http://10.0.2.2:3001/api/tables")
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
                        val tableNumber = item.optString(
                            "tableNumber",
                            item.optString("TableNumber", "")
                        ).trim()
                        val rawStatus = item.optString(
                            "status",
                            item.optString("CurrentStatus", "available")
                        )
                        val normalizedStatus = when {
                            rawStatus.equals("occupied", true) ||
                                rawStatus.equals("Đang dùng", true) -> "occupied"
                            rawStatus.equals("booked", true) ||
                                rawStatus.equals("Đã đặt", true) -> "booked"
                            else -> "available"
                        }
                        val normalizedZone = item.optString("zone", "")
                            .trim()
                            .uppercase()
                            .takeIf { it == "A" || it == "B" }
                            ?: if (tableNumber.uppercase().startsWith("A")) "A" else "B"

                        fetched.add(
                            TableData(
                                id = item.optString("id", item.optString("TableID", "")),
                                tableNumber = tableNumber,
                                tableName = "BÀN $tableNumber",
                                status = normalizedStatus,
                                zone = normalizedZone
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
            delay(5000)
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
