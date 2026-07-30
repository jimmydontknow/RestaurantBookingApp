package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StaffCustomerLookupScreen(
    lookupViewModel: StaffCustomerLookupViewModel = viewModel()
) {
    val context = LocalContext.current
    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
    }

    LaunchedEffect(lookupViewModel.message) {
        lookupViewModel.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            lookupViewModel.consumeMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tra cứu khách hàng", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tìm khách vãng lai hoặc khách thân thiết bằng tên hoặc số điện thoại.",
            color = Color.Gray
        )
        OutlinedTextField(
            value = lookupViewModel.query,
            onValueChange = lookupViewModel::updateQuery,
            label = { Text("Tên hoặc số điện thoại") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = lookupViewModel::lookup,
            enabled = !lookupViewModel.isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (lookupViewModel.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Tra cứu")
            }
        }

        if (lookupViewModel.hasSearched && lookupViewModel.results.isEmpty() && !lookupViewModel.isLoading) {
            Text(
                "Không tìm thấy lịch sử thanh toán phù hợp.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        lookupViewModel.results.forEach { customer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        customer.name.ifBlank { "Khách thân thiết" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF208A48)
                    )
                    Text("Số điện thoại: ${customer.phone.ifBlank { "Chưa có" }}")
                    Text("Số lần đã thanh toán: ${customer.visitCount}")
                    Text("Tổng chi tiêu trước đây: ${formatter.format(customer.totalSpent)}")
                    Text(
                        "Mức giảm hiện tại: ${customer.discountPercent.toInt()}%",
                        fontWeight = FontWeight.Bold
                    )
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    Text(
                        "Lịch sử hóa đơn (${customer.invoices.size})",
                        fontWeight = FontWeight.Bold
                    )
                    if (customer.invoices.isEmpty()) {
                        Text(
                            "Chưa có hóa đơn đã thanh toán cho khách này.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        customer.invoices.forEach { invoice ->
                            CustomerInvoiceHistoryItem(
                                invoice = invoice,
                                formatter = formatter
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerInvoiceHistoryItem(
    invoice: CustomerInvoiceResult,
    formatter: NumberFormat
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8F9FA),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = invoice.bookingCode.ifBlank { "HD${invoice.id}" },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D6EFD)
                )
                Text(
                    text = formatter.format(invoice.totalAmount),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF208A48)
                )
            }
            Text("Bàn: ${invoice.tableSummary.ifBlank { "Chưa có" }}")
            Text("Ngày thanh toán: ${invoice.paidAt.toDisplayDate()}")
            Text(
                "Tiền món: ${formatter.format(invoice.foodSubtotal)} | " +
                    "Giảm: ${formatter.format(invoice.discountAmount)} | " +
                    "Cọc: ${formatter.format(invoice.depositAmount)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (invoice.note.isNotBlank()) {
                Text(
                    "Ghi chú: ${invoice.note}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun String.toDisplayDate(): String {
    return ifBlank { "Chưa có" }
        .replace("T", " ")
        .replace("Z", "")
        .take(19)
}
