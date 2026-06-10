package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StaffCustomerLookupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<JSONObject?>(null) }

    fun lookup() {
        if (phone.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        scope.launch(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                val encoded = URLEncoder.encode(phone.trim(), "UTF-8")
                conn = URL("http://10.0.2.2:3001/api/customers/lookup?phone=$encoded")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                withContext(Dispatchers.Main) {
                    result = if (code == HttpURLConnection.HTTP_OK) JSONObject(body) else null
                    isLoading = false
                    if (code != HttpURLConnection.HTTP_OK) {
                        Toast.makeText(context, "Không tra cứu được khách hàng", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Lỗi: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                conn?.disconnect()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tra cứu khách hàng", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Kiểm tra khách vãng lai hoặc khách thân thiết bằng số điện thoại.", color = Color.Gray)
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter(Char::isDigit) },
            label = { Text("Số điện thoại") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = ::lookup,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            else Text("Tra cứu")
        }

        result?.let { data ->
            val found = data.optBoolean("found", false)
            val visits = data.optInt("visitCount", 0)
            val spent = data.optDouble("totalSpent", 0.0)
            val discount = data.optDouble("discountPercent", 0.0)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (found) "Khách thân thiết" else "Khách vãng lai",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (found) Color(0xFF34C759) else Color(0xFFFF9500)
                    )
                    Text("Số lần đã thanh toán: $visits")
                    Text("Tổng chi tiêu trước đây: ${formatter.format(spent)}")
                    Text("Mức giảm hiện tại: ${discount.toInt()}%", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
