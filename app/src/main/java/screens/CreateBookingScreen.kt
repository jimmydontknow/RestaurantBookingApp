package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookingScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // --- TRẠNG THÁI NHẬP LIỆU ---
    var guestName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var depositAmount by remember { mutableStateOf("200000") }
    var note by remember { mutableStateOf("") }

    // --- TRẠNG THÁI CHỌN KHU ---
    var selectedZone by remember { mutableStateOf("A") }
    var expandedZoneDropdown by remember { mutableStateOf(false) }

    // --- TRẠNG THÁI BOTTOM SHEET CHỌN BÀN ---
    var showTablePicker by remember { mutableStateOf(false) }

    // --- DANH SÁCH BÀN TỪ API ---
    val allTables = remember { mutableStateListOf<TableData>() }
    var isLoadingTables by remember { mutableStateOf(false) }

    // --- BÀN ĐÃ CHỌN (có thể chọn nhiều để gộp) ---
    val selectedTables = remember { mutableStateListOf<TableData>() }

    // Lọc bàn theo khu đang chọn, chỉ hiện bàn còn trống hoặc chưa đặt
    val tablesInZone = allTables.filter { it.zone == selectedZone }

    // Ngày giờ
    var bookingDate by remember { mutableStateOf("") }
    var bookingTime by remember { mutableStateOf("") }
    var expandedTime by remember { mutableStateOf(false) }
    val timeSlots = listOf("10:00","11:00","12:00","13:00","17:00","18:00","19:00","20:00","21:00")

    // Tóm tắt bàn đã chọn để hiển thị trên form
    val tableSummaryDisplay = when {
        selectedTables.isEmpty() -> "Chưa chọn bàn"
        selectedTables.size == 1 -> "1 Bàn đơn ${selectedTables[0].tableNumber} (Khu $selectedZone)"
        else -> "Gộp ${selectedTables.size} bàn: ${selectedTables.joinToString(", ") { it.tableNumber }} (Khu $selectedZone)"
    }

    // Chuỗi gửi lên server
    val tableSummaryForServer = when {
        selectedTables.isEmpty() -> ""
        selectedTables.size == 1 -> "1 Bàn đơn ${selectedTables[0].tableNumber} Khu $selectedZone"
        else -> "Gộp ${selectedTables.size} bàn ${selectedTables.joinToString("+") { it.tableNumber }} Khu $selectedZone"
    }

    // --- HÀM TẢI DANH SÁCH BÀN TỪ API ---
    suspend fun loadTables() {
        isLoadingTables = true
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3001/api/tables")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("tables")
                    val fetched = mutableListOf<TableData>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val tableNumber = item.optString("tableNumber", item.optString("TableNumber", ""))
                        val rawStatus = item.optString("status", item.optString("CurrentStatus", "available"))
                        val normalizedStatus = when {
                            rawStatus.equals("occupied", true) || rawStatus.equals("Đang dùng", true) -> "occupied"
                            rawStatus.equals("booked", true) || rawStatus.equals("Đã đặt", true) -> "booked"
                            else -> "available"
                        }

                        fetched.add(
                            TableData(
                                id = item.optString("id", item.optString("TableID", "")),
                                tableNumber = tableNumber,
                                tableName = "BÀN $tableNumber",
                                status = normalizedStatus,
                                zone = item.optString("zone", if (tableNumber.startsWith("A")) "A" else "B"),
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        allTables.clear()
                        allTables.addAll(fetched)
                        isLoadingTables = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoadingTables = false }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoadingTables = false }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // --- HÀM CẬP NHẬT TRẠNG THÁI BÀN ĐÃ CHỌN LÊN SERVER ---
    suspend fun markTablesAsBooked(tableIds: List<String>) {
        withContext(Dispatchers.IO) {
            tableIds.forEach { tableId ->
                var conn: HttpURLConnection? = null
                try {
                    val url = URL("http://10.0.2.2:3001/api/tables/status")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "PUT"
                    conn.setRequestProperty("Content-Type", "application/json; utf-8")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true

                    val body = JSONObject().apply {
                        put("id", tableId)
                        put("status", "booked") // Trạng thái mới: "booked" = đã đặt
                    }.toString()

                    conn.outputStream.use { os -> os.write(body.toByteArray(charset("utf-8"))) }
                    conn.responseCode // Kích hoạt request
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }
// NGÀY ĐẶT BÀN
    OutlinedTextField(
        value = bookingDate,
        onValueChange = { bookingDate = it },
        label = { Text("Ngày đặt bàn (YYYY-MM-DD)") },
        placeholder = { Text("VD: 2025-12-31") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF007AFF), unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
    )

// GIỜ ĐẶT BÀN
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = bookingTime,
            onValueChange = {},
            readOnly = true,
            label = { Text("Giờ đặt bàn") },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedTime = true }) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
        )
        DropdownMenu(expanded = expandedTime, onDismissRequest = { expandedTime = false }) {
            timeSlots.forEach { time ->
                DropdownMenuItem(text = { Text(time) }, onClick = { bookingTime = time; expandedTime = false })
            }
        }
    }
    // --- HÀM TẠO ĐẶT BÀN ---
    suspend fun createBooking(): Boolean {
        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3001/api/bookings")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; utf-8")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.doOutput = true

                val jsonInputString = JSONObject().apply {
                    put("guestName", guestName)
                    put("phoneNumber", phoneNumber)
                    put("tableNumber", tableSummaryForServer)
                    put("depositAmount", depositAmount.toIntOrNull() ?: 0)
                    put("note", note)
                    put("bookingDate", bookingDate)   // ĐÃ THÊM
                    put("bookingTime", bookingTime)   // ĐÃ THÊM
                }.toString()

                conn.outputStream.use { os ->
                    os.write(jsonInputString.toByteArray(charset("utf-8")))
                }

                val code = conn.responseCode
                code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_CREATED
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                conn?.disconnect()
            }
        }
    }

    // Tải bàn khi mở màn hình
    LaunchedEffect(Unit) { loadTables() }

    // =========================================================================
    // BOTTOM SHEET CHỌN BÀN
    // =========================================================================
    if (showTablePicker) {
        ModalBottomSheet(
            onDismissRequest = { showTablePicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header bottom sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chọn bàn — Khu $selectedZone",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showTablePicker = false }) {
                        Text("Xong", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Số bàn đã chọn
                Text(
                    text = if (selectedTables.isEmpty()) "Chưa chọn bàn nào"
                    else "Đã chọn ${selectedTables.size} bàn: ${selectedTables.joinToString(", ") { it.tableNumber }}",
                    fontSize = 13.sp,
                    color = if (selectedTables.isEmpty()) Color.Gray else Color(0xFF007AFF),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Chú thích màu
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem(color = Color(0xFFE2F0D9), label = "Trống")
                    LegendItem(color = Color(0xFF007AFF).copy(alpha = 0.15f), label = "Đang chọn")
                    LegendItem(color = Color(0xFFFFE5E5), label = "Không có sẵn")
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingTables) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF007AFF))
                    }
                } else {
                    // Lưới bàn 4 cột
                    val gridHeight = (kotlin.math.ceil(tablesInZone.size / 4.0).toInt() * 90).dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().height(gridHeight),
                        userScrollEnabled = false
                    ) {
                        items(tablesInZone) { table ->
                            val isSelected = selectedTables.any { it.id == table.id }
                            // Bàn không chọn được nếu đang occupied hoặc booked
                            val isUnavailable = table.status != "available"

                            val bgColor = when {
                                isSelected -> Color(0xFF007AFF).copy(alpha = 0.15f)
                                isUnavailable -> Color(0xFFFFE5E5)
                                else -> Color(0xFFE2F0D9)
                            }
                            val borderColor = when {
                                isSelected -> Color(0xFF007AFF)
                                isUnavailable -> Color(0xFFFFCDD2)
                                else -> Color(0xFFB7D8A8)
                            }
                            val labelColor = when {
                                isSelected -> Color(0xFF007AFF)
                                isUnavailable -> Color(0xFFE53935)
                                else -> Color(0xFF385723)
                            }
                            val statusText = when {
                                isSelected -> "Đang chọn"
                                table.status == "occupied" -> "Đang dùng"
                                table.status == "booked" -> "Đã đặt"
                                table.status != "available" -> "Không có sẵn"
                                else -> "Trống"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(bgColor, RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isUnavailable) {
                                        // Toggle chọn/bỏ chọn bàn
                                        if (isSelected) {
                                            selectedTables.removeIf { it.id == table.id }
                                        } else {
                                            selectedTables.add(table)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        table.tableNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = labelColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Nút xác nhận chọn bàn
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showTablePicker = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    enabled = selectedTables.isNotEmpty()
                ) {
                    Text(
                        if (selectedTables.isEmpty()) "Chưa chọn bàn nào"
                        else "Xác nhận ${selectedTables.size} bàn đã chọn",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // =========================================================================
    // FORM TẠO ĐẶT BÀN
    // =========================================================================
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Tạo đặt bàn phân khu", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.Black)
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
                .background(Color(0xFFF8F9FA))
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tên khách hàng
            OutlinedTextField(
                value = guestName,
                onValueChange = { guestName = it },
                label = { Text("Tên khách hàng") },
                placeholder = { Text("Nhập tên khách hàng...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // 2. Số điện thoại
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Số điện thoại") },
                placeholder = { Text("Nhập số điện thoại...") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // 3. CHỌN KHU VỰC
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (selectedZone == "A") "Khu A — Phòng lạnh (20 bàn, tối đa 10 người/bàn)"
                    else "Khu B — Ngoài trời (10 bàn, tối đa 20 người/bàn)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Chọn khu vực") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown, "Dropdown",
                            Modifier.clickable { expandedZoneDropdown = true }
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
                    expanded = expandedZoneDropdown,
                    onDismissRequest = { expandedZoneDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Khu A — Phòng lạnh (20 bàn)") },
                        onClick = {
                            selectedZone = "A"
                            selectedTables.clear() // Reset bàn đã chọn khi đổi khu
                            expandedZoneDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Khu B — Ngoài trời (10 bàn)") },
                        onClick = {
                            selectedZone = "B"
                            selectedTables.clear()
                            expandedZoneDropdown = false
                        }
                    )
                }
            }

            // 4. NÚT MỞ SƠ ĐỒ CHỌN BÀN
            OutlinedTextField(
                value = tableSummaryDisplay,
                onValueChange = {},
                readOnly = true,
                label = { Text("Bàn đã chọn") },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown, "Chọn bàn",
                        Modifier.clickable {
                            coroutineScope.launch { loadTables() }
                            showTablePicker = true
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch { loadTables() }
                        showTablePicker = true
                    },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF007AFF)
                )
            )

            // Nút mở sơ đồ bàn rõ ràng hơn
            OutlinedButton(
                onClick = {
                    coroutineScope.launch { loadTables() }
                    showTablePicker = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF007AFF))
            ) {
                Text(
                    text = "Mở sơ đồ bàn Khu $selectedZone để chọn",
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // 5. Tiền cọc
            OutlinedTextField(
                value = depositAmount,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tiền đặt cọc cố định (VND)") },
                supportingText = { Text("Tiền cọc sẽ được trừ khi thanh toán hóa đơn.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // 6. Ghi chú
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Yêu cầu đặc biệt") },
                placeholder = { Text("Yêu cầu set up món trước...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // NÚT XÁC NHẬN TẠO ĐẶT BÀN
            Button(
                onClick = {
                    when {
                        guestName.trim().isEmpty() ->
                            Toast.makeText(context, "Vui lòng nhập tên khách hàng!", Toast.LENGTH_SHORT).show()
                        phoneNumber.trim().isEmpty() ->
                            Toast.makeText(context, "Vui lòng nhập số điện thoại!", Toast.LENGTH_SHORT).show()
                        selectedTables.isEmpty() ->
                            Toast.makeText(context, "Vui lòng chọn ít nhất 1 bàn!", Toast.LENGTH_SHORT).show()
                        else -> {
                            coroutineScope.launch {
                                // Bước 1: Tạo booking
                                val success = createBooking()
                                if (success) {
                                    // Bước 2: Cập nhật trạng thái các bàn đã chọn → "booked"
                                    markTablesAsBooked(selectedTables.map { it.id })
                                    Toast.makeText(
                                        context,
                                        "Đặt bàn thành công! ${selectedTables.size} bàn đã được cập nhật trạng thái.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Lỗi tạo đặt bàn, thử lại!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTables.isEmpty()) Color.Gray else Color(0xFF007AFF)
                )
            ) {
                Text("Xác nhận tạo đặt bàn", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// Composable chú thích màu nhỏ
@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}