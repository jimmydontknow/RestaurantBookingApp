package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// 0. ĐỊNH NGHĨA LỚP DỮ LIỆU THÔNG TIN BÀN ĂN
data class TableData(
    val id: String,
    val tableName: String,  // Tên bàn hiển thị (Ví dụ: BÀN 01)
    val status: String      // Trạng thái bàn: "available" (Trống), "occupied" (Đang ăn)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoDoBanScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- QUẢN LÝ DANH SÁCH BÀN ĂN BẰNG MUTABLESTATELISTOF ĐỂ CẬP NHẬT GIAO DIỆN TỨC THÌ ---
    // ĐÃ SỬA: Không còn hardcode, dữ liệu được load động từ dbo.Tables qua API
    val tableList = remember { mutableStateListOf<TableData>() }

    // --- TRẠNG THÁI ĐỂ PHỤC VỤ HỘP THOẠI ĐỔI TRẠNG THÁI BÀN ---
    var showDialog by remember { mutableStateOf(false) }
    var selectedTableForEdit by remember { mutableStateOf<TableData?>(null) }

    // ĐÃ THÊM: Trạng thái loading để hiển thị vòng tròn chờ khi đang tải dữ liệu lần đầu
    var isLoading by remember { mutableStateOf(true) }

    // Tính toán số lượng bàn tự động mỗi khi danh sách có sự thay đổi
    val occupiedCount = tableList.count { it.status == "occupied" }
    val totalTables = tableList.size

    // =========================================================================
    // ĐÃ THÊM: HÀM GỌI API LẤY DANH SÁCH BÀN TỪ dbo.Tables (GET /api/tables)
    // Thay thế hoàn toàn dữ liệu hardcode cũ, đảm bảo đồng bộ với SQL Server
    // =========================================================================
    val fetchTables = suspend {
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("http://10.0.2.2:3000/api/tables")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("tables")

                    val fetchedList = mutableListOf<TableData>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        fetchedList.add(
                            TableData(
                                id = item.getString("id"),
                                // padStart(2, '0'): Đảm bảo số bàn luôn hiển thị 2 chữ số (1 → "01", 2 → "02")
                                tableName = "BÀN ${item.getString("tableNumber").padStart(2, '0')}",
                                status = item.getString("status")
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        tableList.clear()
                        tableList.addAll(fetchedList)
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi tải sơ đồ bàn: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    isLoading = false
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    // =========================================================================
    // ĐÃ THÊM: HÀM GỌI API CẬP NHẬT TRẠNG THÁI BÀN LÊN SQL SERVER (PUT /api/tables/status)
    // Trước đây chỉ cập nhật trong RAM (mất khi tắt app), nay lưu thẳng vào database
    // =========================================================================
    val updateTableStatus = { tableId: String, newStatus: String, index: Int, updatedTable: TableData ->
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                var conn: HttpURLConnection? = null
                try {
                    val url = URL("http://10.0.2.2:3000/api/tables/status")
                    conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "PUT"
                    conn.setRequestProperty("Content-Type", "application/json; utf-8")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true

                    val body = JSONObject().apply {
                        put("id", tableId)
                        put("status", newStatus)
                    }.toString()

                    conn.outputStream.use { os ->
                        os.write(body.toByteArray(charset("utf-8")))
                    }

                    val responseCode = conn.responseCode
                    withContext(Dispatchers.Main) {
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            // Cập nhật UI ngay lập tức sau khi server xác nhận lưu thành công
                            tableList[index] = updatedTable
                            Toast.makeText(context, "Đã cập nhật ${updatedTable.tableName}!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Lỗi cập nhật: mã $responseCode", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lỗi kết nối: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    conn?.disconnect()
                }
            }
        }
    }

    // ĐÃ THÊM: Tự động tải danh sách bàn từ API khi mở màn hình (thay vì dùng hardcode)
    LaunchedEffect(Unit) {
        fetchTables()
    }

    Scaffold(
        topBar = {
            // [COMP 1]: HEADER - Có nút quay lại và tiêu đề căn giữa
            TopAppBar(
                title = {
                    Text(
                        text = "Sơ đồ bàn ăn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.background(Color.White)
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // [COMP 2]: ROOM INFO SECTOR - Trạng thái phòng và dòng chú thích màu sắc
            Text(
                text = "Trạng thái phòng máy lạnh",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212529)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Xanh lá: Bàn trống | Xanh dương: Đang có khách",
                fontSize = 13.sp,
                color = Color.Gray
            )

            // Thanh tiến độ hiển thị trực quan tỷ lệ lấp đầy bàn ăn (Tự động chạy lại khi đổi trạng thái bàn)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hiệu suất sử dụng: $occupiedCount / $totalTables bàn đang ăn",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            // ĐÃ THÊM: Kiểm tra totalTables > 0 tránh lỗi chia cho 0 khi chưa load xong dữ liệu
            if (totalTables > 0) {
                LinearProgressIndicator(
                    progress = { occupiedCount.toFloat() / totalTables.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF007AFF),
                    trackColor = Color(0xFFE5E5EA),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // [COMP 3]: GRID LIST TABLES - Hiển thị danh sách bàn dạng lưới 2 cột
            // ĐÃ THÊM: Hiển thị vòng tròn loading khi đang tải dữ liệu lần đầu từ API
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tableList) { table ->
                        val isAvailable = table.status == "available"
                        val cardBackgroundColor = if (isAvailable) Color(0xFFE2F0D9) else Color(0xFFD9E1F2)
                        val statusTextColor = if (isAvailable) Color(0xFF385723) else Color(0xFF1F4E79)
                        val statusTextLabel = if (isAvailable) "Trống" else "Đang ăn"

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clickable {
                                    // ĐÃ THÊM: Lưu lại thông tin bàn được click và mở Dialog chỉnh sửa
                                    selectedTableForEdit = table
                                    showDialog = true
                                },
                            color = cardBackgroundColor,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = table.tableName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = statusTextLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // --- CHỨC NĂNG CHỈNH SỬA TRẠNG THÁI BÀN ĂN (DIALOG XÁC NHẬN) ---
    // =========================================================================
    if (showDialog && selectedTableForEdit != null) {
        val currentTable = selectedTableForEdit!!
        val isCurrentAvailable = currentTable.status == "available"

        // Chuẩn bị văn bản linh hoạt theo trạng thái hiện tại của bàn
        val nextStatusLabel = if (isCurrentAvailable) "Đang ăn" else "Trống"
        val nextStatusValue = if (isCurrentAvailable) "occupied" else "available"

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = "Thay đổi trạng thái ${currentTable.tableName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Bạn có muốn chuyển trạng thái bàn từ [${if (isCurrentAvailable) "Trống" else "Đang ăn"}] sang [$nextStatusLabel] không?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Tìm vị trí của bàn ăn trong List và chuẩn bị bản ghi mới
                        val index = tableList.indexOfFirst { it.id == currentTable.id }
                        if (index != -1) {
                            val updatedTable = currentTable.copy(status = nextStatusValue)
                            // ĐÃ SỬA: Gọi API lưu vào SQL Server thay vì chỉ cập nhật RAM
                            updateTableStatus(currentTable.id, nextStatusValue, index, updatedTable)
                        }
                        showDialog = false // Đóng hộp thoại
                    }
                ) {
                    Text(text = "Cập nhật", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Hủy bỏ", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(14.dp)
        )
    }
}