package com.example.restaurantbookingapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Model dữ liệu bàn ăn vật lý hiển thị trên sơ đồ lưới
data class TableMapItem(
    val id: String,
    val name: String,     // Ví dụ: "BÀN 01"
    val status: String,   // "Bàn trống", "Đang dùng", "Đang đặt"
    val type: String      // "đơn" hoặc "ghép"
)

@Composable
fun TableMapScreen(navController: NavController) {
    val tableList = remember { mutableStateListOf<TableMapItem>() }

    // Đồng bộ gọi API lấy dữ liệu thực tế từ hệ thống Backend
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:3000/api/tables-layout")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("tables")
                    val fetchedTables = mutableListOf<TableMapItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        fetchedTables.add(
                            TableMapItem(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                status = obj.getString("status"),
                                type = obj.getString("type") // Bóc tách trường phân loại "đơn" / "ghép"
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        tableList.clear()
                        tableList.addAll(fetchedTables)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // Khối tiêu đề trên cùng (Giữ nguyên cấu trúc ảnh mẫu)
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Trạng thái phòng máy lạnh", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = "Xanh lá: Bàn trống | Xanh dương: Đang có khách", fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Hiệu suất sử dụng: ${tableList.count { it.status != "Bàn trống" }} / 12 bàn đang ăn", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color(0xFF007AFF), trackColor = Color(0xFFE5E5EA))
        }

        // Danh sách hiển thị sơ đồ dạng lưới 2 cột chuẩn thiết kế UI của bạn
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tableList) { table ->
                TableCard(tableItem = table)
            }
        }
    }
}

// --- THÀNH PHẦN CUSTOM TABLE CARD: ĐÃ NÂNG CẤP THÊM NHÃN GÓC TRÊN BÊN PHẢI ---
@Composable
fun TableCard(tableItem: TableMapItem) {
    // Xác định hệ màu nền và màu chữ theo trạng thái hoạt động thực tế (Xanh dương cho "Đang dùng")
    val isOccupied = tableItem.status == "Đang dùng" || tableItem.status == "Đang đặt"
    val containerBgColor = if (isOccupied) Color(0xFFE5F1FF) else Color(0xFFE4F9E7)
    val textMainColor = if (isOccupied) Color(0xFF0056B3) else Color(0xFF28A745)

    // Xác định màu sắc nhãn phân loại: "ghép" sẽ có nhãn màu cam cảnh báo rực rỡ, "đơn" màu xám nhẹ
    val isMerged = tableItem.type == "ghép"
    val badgeBgColor = if (isMerged) Color(0xFFFF9500) else Color(0xFF8E8E93)

    // Sử dụng Box làm FrameLayout bao bọc để hỗ trợ đặt linh kiện nhãn xếp chồng góc phải
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(color = containerBgColor, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // 1. Nhãn định vị phân loại (Nằm ở Góc trên bên phải - Alignment.TopEnd)
        Surface(
            modifier = Modifier.align(Alignment.TopEnd),
            color = badgeBgColor,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = tableItem.type, // Hiển thị chữ "đơn" hoặc "ghép"
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // 2. Nội dung thông tin chính của bàn (Căn giữa không gian ô chứa)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tableItem.name, // Ví dụ: "BÀN 01"
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tableItem.status, // Ví dụ: "Đang dùng"
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textMainColor
            )
        }
    }
}