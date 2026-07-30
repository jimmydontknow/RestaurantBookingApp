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
import com.example.restaurantbookingapp.network.ApiConfig
import com.example.restaurantbookingapp.network.TokenManager
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
                val url = URL(ApiConfig.endpoint("/api/tables"))
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val token = TokenManager.getToken()
                if (!token.isNullOrBlank()) {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONObject(responseStr).getJSONArray("tables")
                    val fetchedTables = mutableListOf<TableMapItem>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val normalizedStatus = obj.optString("status", "available")
                        fetchedTables.add(
                            TableMapItem(
                                id = obj.getString("id"),
                                name = "BÀN ${obj.optString("tableNumber")}",
                                status = when (normalizedStatus) {
                                    "occupied" -> "Đang dùng"
                                    "booked" -> "Đang đặt"
                                    else -> "Bàn trống"
                                },
                                type = if (obj.optInt("capacity", 1) > 10) "ghép" else "đơn"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Text(
            text = "Sơ đồ bàn ăn",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "Trạng thái bàn thời gian thực",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Chú thích trạng thái bàn
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusLegendItem(color = Color(0xFF34C759), label = "Bàn trống")
            StatusLegendItem(color = Color(0xFFFF9500), label = "Đang đặt")
            StatusLegendItem(color = Color(0xFFFF3B30), label = "Đang dùng")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sơ đồ lưới danh sách bàn
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tableList) { table ->
                TableCardItem(table = table)
            }
        }
    }
}

@Composable
fun StatusLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = color,
            shape = RoundedCornerShape(3.dp)
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun TableCardItem(table: TableMapItem) {
    val statusColor = when (table.status) {
        "Đang dùng" -> Color(0xFFFF3B30)
        "Đang đặt" -> Color(0xFFFF9500)
        else -> Color(0xFF34C759)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = table.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = table.status,
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
