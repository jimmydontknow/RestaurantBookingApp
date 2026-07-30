package com.example.restaurantbookingapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.restaurantbookingapp.network.ApiConfig
import com.example.restaurantbookingapp.network.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TableData(
    val id: String,
    val tableNumber: String,
    val tableName: String,
    val status: String,
    val zone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoDoBanScreen(navController: NavController) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val tableList = remember {
        mutableStateListOf<TableData>()
    }

    val scrollState = rememberScrollState()

    var showDialog by remember {
        mutableStateOf(false)
    }

    var selectedTableForEdit by remember {
        mutableStateOf<TableData?>(null)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    fun tableOrder(table: TableData): Int =
        table.tableNumber.drop(1).toIntOrNull() ?: Int.MAX_VALUE

    val zoneATables =
        tableList.filter { it.zone == "A" }.sortedBy(::tableOrder)

    val zoneBTables =
        tableList.filter { it.zone == "B" }.sortedBy(::tableOrder)

    val occupiedCount =
        tableList.count {
            it.status == "occupied"
        }

    val bookedCount =
        tableList.count {
            it.status == "booked"
        }

    val totalTables =
        tableList.size

    // ====================================================
    // FETCH TABLES
    // ====================================================

    suspend fun fetchTables() {

        withContext(Dispatchers.IO) {

            var conn: HttpURLConnection? = null

            try {

                val url =
                    URL(ApiConfig.endpoint("/api/tables"))

                conn =
                    url.openConnection()
                            as HttpURLConnection

                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val token = TokenManager.getToken()
                if (!token.isNullOrBlank()) {
                    conn.setRequestProperty("Authorization", "Bearer $token")
                }

                if (
                    conn.responseCode ==
                    HttpURLConnection.HTTP_OK
                ) {

                    val responseStr =
                        conn.inputStream
                            .bufferedReader()
                            .use { it.readText() }

                    val jsonArray =
                        JSONObject(responseStr)
                            .getJSONArray("tables")

                    val fetched =
                        mutableListOf<TableData>()

                    for (i in 0 until jsonArray.length()) {

                        val obj =
                            jsonArray.getJSONObject(i)

                        val tableNum =
                            obj.optString(
                                "tableNumber",
                                obj.optString("TableNumber", "")
                            )

                        val rawStatus =
                            obj.optString(
                                "status",
                                obj.optString("CurrentStatus", "available")
                            )

                        val normalizedStatus = when {
                            rawStatus.equals("occupied", true) || rawStatus.equals("Đang dùng", true) -> "occupied"
                            rawStatus.equals("booked", true) || rawStatus.equals("Đã đặt", true) -> "booked"
                            else -> "available"
                        }

                        val tableNumUpper = tableNum.trim().uppercase()
                        val parsedZone = obj.optString("zone", "").uppercase()
                        val finalZone = when {
                            parsedZone == "A" || parsedZone == "B" -> parsedZone
                            tableNumUpper.startsWith("A") -> "A"
                            tableNumUpper.startsWith("B") -> "B"
                            else -> {
                                val num = tableNumUpper.filter { it.isDigit() }.toIntOrNull() ?: 1
                                if (num <= 6) "A" else "B"
                            }
                        }

                        fetched.add(
                            TableData(
                                id = obj.optString("id", obj.optString("TableID", "")),
                                tableNumber = tableNum,
                                tableName = if (tableNumUpper.startsWith("BÀN")) tableNumUpper else "BÀN $tableNum",
                                status = normalizedStatus,
                                zone = finalZone
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {

                        tableList.clear()
                        tableList.addAll(fetched)

                        isLoading = false
                    }
                }

            } catch (e: Exception) {

                e.printStackTrace()

                withContext(Dispatchers.Main) {

                    isLoading = false

                    Toast.makeText(
                        context,
                        "Lỗi tải sơ đồ bàn!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            finally {

                conn?.disconnect()
            }
        }
    }

    // ====================================================
    // UPDATE TABLE STATUS
    // ====================================================

    fun updateTableStatus(
        tableId: String,
        newStatus: String,
        index: Int,
        updatedTable: TableData
    ) {

        coroutineScope.launch {

            withContext(Dispatchers.IO) {

                var conn: HttpURLConnection? = null

                try {

                    val url =
                        URL(ApiConfig.endpoint("/api/tables/status"))

                    conn =
                        url.openConnection()
                                as HttpURLConnection

                    conn.requestMethod = "PUT"

                    conn.setRequestProperty(
                        "Content-Type",
                        "application/json; utf-8"
                    )

                    val token = TokenManager.getToken()
                    if (!token.isNullOrBlank()) {
                        conn.setRequestProperty("Authorization", "Bearer $token")
                    }

                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true

                    val body =
                        JSONObject().apply {

                            put("id", tableId)

                            put(
                                "status",
                                newStatus
                            )

                        }.toString()

                    conn.outputStream.use {

                        it.write(
                            body.toByteArray(
                                Charsets.UTF_8
                            )
                        )
                    }

                    val code =
                        conn.responseCode

                    withContext(Dispatchers.Main) {

                        if (
                            code ==
                            HttpURLConnection.HTTP_OK
                        ) {

                            tableList[index] =
                                updatedTable

                            Toast.makeText(
                                context,
                                "Đã cập nhật bàn!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } catch (e: Exception) {

                    e.printStackTrace()
                }

                finally {

                    conn?.disconnect()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            fetchTables()
            delay(5000)
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Sơ đồ bàn ăn",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {

                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sơ đồ bàn ăn nhà hàng",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text =
                    "Đang dùng: $occupiedCount | Đã đặt: $bookedCount | Tổng: $totalTables bàn",
                fontSize = 13.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),

                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            else {

                // =========================
                // KHU A
                // =========================

                Text(
                    text =
                        "Khu A (${zoneATables.size} bàn)",

                    fontWeight = FontWeight.Bold,

                    color = Color(0xFF1D4ED8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val aRows =
                    kotlin.math.ceil(
                        zoneATables.size / 3.0
                    ).toInt()

                val aHeight =
                    (aRows * 110).dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(aHeight),

                    userScrollEnabled = false,

                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(zoneATables) { table ->

                        TableCard(
                            table = table,
                            onClick = {

                                selectedTableForEdit =
                                    table

                                showDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // =========================
                // KHU B
                // =========================

                Text(
                    text =
                        "Khu B (${zoneBTables.size} bàn)",

                    fontWeight = FontWeight.Bold,

                    color = Color(0xFF15803D)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val bRows =
                    kotlin.math.ceil(
                        zoneBTables.size / 3.0
                    ).toInt()

                val bHeight =
                    (bRows * 110).dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bHeight),

                    userScrollEnabled = false,

                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(zoneBTables) { table ->

                        TableCard(
                            table = table,
                            onClick = {

                                selectedTableForEdit =
                                    table

                                showDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // ====================================================
    // DIALOG
    // ====================================================

    if (
        showDialog &&
        selectedTableForEdit != null
    ) {

        val currentTable =
            selectedTableForEdit!!

        val isAvailable =
            currentTable.status == "available"

        val nextStatus =
            if (isAvailable)
                "occupied"
            else
                "available"

        val nextLabel =
            if (isAvailable)
                "Đang dùng"
            else
                "Trống"

        AlertDialog(

            onDismissRequest = {
                showDialog = false
            },

            title = {

                Text(
                    "Đổi trạng thái ${currentTable.tableName}"
                )
            },

            text = {

                Text(
                    "Chuyển sang [$nextLabel]?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        val index =
                            tableList.indexOfFirst {
                                it.id == currentTable.id
                            }

                        if (index != -1) {

                            val updated =
                                currentTable.copy(
                                    status = nextStatus
                                )

                            updateTableStatus(
                                currentTable.id,
                                nextStatus,
                                index,
                                updated
                            )
                        }

                        showDialog = false
                    }
                ) {

                    Text("Cập nhật")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {

                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun TableCard(
    table: TableData,
    onClick: () -> Unit
) {

    val isBooked =
        table.status == "booked"

    val bgColor =
        when {

            table.status == "occupied" ->
                Color(0xFFD9E1F2)

            isBooked ->
                Color(0xFFFFF3CD)

            else ->
                Color(0xFFE2F0D9)
        }

    val textColor =
        when {

            table.status == "occupied" ->
                Color(0xFF1F4E79)

            isBooked ->
                Color(0xFF856404)

            else ->
                Color(0xFF385723)
        }

    val statusLabel =
        when {

            table.status == "occupied" ->
                "Đang dùng"

            isBooked ->
                "Đã đặt"

            else ->
                "Trống"
        }

    Box(

        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                bgColor,
                RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            }
    ) {

        Column(

            modifier = Modifier.fillMaxSize(),

            verticalArrangement =
                Arrangement.Center,

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = table.tableName,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusLabel,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isBooked) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(
                        Color(0xFFFF9500),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
            ) {

                Text(
                    text = "Đã đặt",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}