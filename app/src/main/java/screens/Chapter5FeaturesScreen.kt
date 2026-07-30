package com.example.restaurantbookingapp.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.restaurantbookingapp.network.ApiResult
import com.example.restaurantbookingapp.network.RestaurantRepository
import com.example.restaurantbookingapp.network.RestaurantSnapshot
import com.example.restaurantbookingapp.ui.theme.BackgroundGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chapter5FeaturesScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { RestaurantRepository() }
    val scope = rememberCoroutineScope()

    var isLoading by rememberSaveable { mutableStateOf(false) }
    var networkMessage by rememberSaveable { mutableStateOf("Chưa kiểm tra kết nối") }
    var snapshot by remember { mutableStateOf<RestaurantSnapshot?>(null) }
    var selectedImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var imageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri?.toString()
    }

    LaunchedEffect(selectedImageUri) {
        imageBitmap = selectedImageUri?.let { uriString ->
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
        }
    }

    fun refreshSnapshot() {
        isLoading = true
        networkMessage = "Đang gọi API..."
        scope.launch {
            when (val result = withContext(Dispatchers.IO) { repository.loadRestaurantSnapshot() }) {
                is ApiResult.Success -> {
                    snapshot = result.data
                    networkMessage = "Đã đồng bộ dữ liệu nhà hàng"
                }
                is ApiResult.Error -> {
                    networkMessage = result.message
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tiện ích Chương 5", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
                .background(BackgroundGray)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = "Kết nối mạng",
                icon = Icons.Default.Sync,
                description = networkMessage
            ) {
                Button(onClick = { refreshSnapshot() }, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Kiểm tra API")
                    }
                }
                snapshot?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tổng bàn: ${it.tableCount}")
                    Text("Trống: ${it.availableTables} | Đã đặt: ${it.bookedTables} | Đang dùng: ${it.occupiedTables}")
                    Text("Tổng món trong menu: ${it.menuCount}")
                }
            }

            FeatureCard(
                title = "Đa phương tiện",
                icon = Icons.Default.Image,
                description = "Chọn ảnh món ăn hoặc ảnh không gian nhà hàng để xem trong app"
            ) {
                OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                    Text("Chọn ảnh")
                }
                imageBitmap?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Ảnh đã chọn",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            FeatureCard(
                title = "Liên lạc",
                icon = Icons.Default.Call,
                description = "Gọi điện, nhắn tin, gửi email hoặc chia sẻ thông tin đặt bàn"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { context.safeStart(Intent(Intent.ACTION_DIAL, Uri.parse("tel:0900000000"))) }) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Text(" Gọi")
                    }
                    OutlinedButton(onClick = { context.safeStart(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:0900000000")).putExtra("sms_body", "Tôi muốn đặt bàn tại nhà hàng.")) }) {
                        Text("SMS")
                    }
                    OutlinedButton(onClick = { context.safeStart(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:restaurant@example.com")).putExtra(Intent.EXTRA_SUBJECT, "Yêu cầu đặt bàn")) }) {
                        Icon(Icons.Default.Email, contentDescription = null)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "Nhà hàng hỗ trợ đặt bàn, gọi món và thanh toán hóa đơn.")
                        context.safeStart(Intent.createChooser(sendIntent, "Chia sẻ thông tin nhà hàng"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text(" Chia sẻ")
                }
            }

            FeatureCard(
                title = "Vị trí",
                icon = Icons.Default.Map,
                description = "Mở bản đồ bằng trình duyệt để tránh lỗi Google Maps cũ trên emulator"
            ) {
                Button(
                    onClick = { context.openRestaurantMap() }
                ) {
                    Text("Mở bản đồ")
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    icon: ImageVector,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = description,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                color = Color.Gray
            )
            content()
        }
    }
}

private fun android.content.Context.safeStart(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(this, "Thiết bị không có ứng dụng phù hợp", Toast.LENGTH_SHORT).show()
    }
}

private fun android.content.Context.openRestaurantMap() {
    val query = Uri.encode("nhà hàng gần đây")
    val webUri = Uri.parse("https://www.google.com/search?q=$query+google+maps")
    val browserPackages = listOf(
        "com.android.chrome",
        "com.android.browser",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser"
    )

    browserPackages.forEach { packageName ->
        val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(packageName)
        }
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
            return
        }
    }

    val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    val browserCandidate = packageManager
        .queryIntentActivities(fallbackIntent, 0)
        .firstOrNull { !it.activityInfo.packageName.contains("maps", ignoreCase = true) }

    if (browserCandidate != null) {
        fallbackIntent.setPackage(browserCandidate.activityInfo.packageName)
        safeStart(fallbackIntent)
    } else {
        Toast.makeText(this, "Máy ảo chưa có trình duyệt để mở bản đồ", Toast.LENGTH_LONG).show()
    }
}
