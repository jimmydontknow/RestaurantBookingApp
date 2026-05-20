package com.example.restaurantbookingapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(
    title: String,
    count: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Thẻ Card bo góc, có bóng đổ nhẹ chiếm 50% độ rộng dòng
    Card(
        modifier = modifier
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Khối màu chỉ thị trạng thái (Trống, Đang ăn...) ở góc
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(backgroundColor, RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Tên trạng thái
            Text(text = title, fontSize = 14.sp, color = Color.Gray)
            // Số lượng bàn hiện tại
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}