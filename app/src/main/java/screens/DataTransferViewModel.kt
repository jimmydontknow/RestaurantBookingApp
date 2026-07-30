package com.example.restaurantbookingapp.screens

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class DataTransferViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    var isProcessing by mutableStateOf(savedStateHandle["isProcessing"] ?: false)
        private set
    var progress by mutableIntStateOf(savedStateHandle["progress"] ?: 0)
        private set
    var statusText by mutableStateOf(savedStateHandle["statusText"] ?: "Sẵn sàng")
        private set
    var lastFilePath by mutableStateOf(savedStateHandle["lastFilePath"] ?: "")
        private set

    init {
        if (isProcessing) {
            updateState(
                processing = false,
                newProgress = 0,
                message = "Tác vụ trước đã bị gián đoạn. Bạn có thể chạy lại."
            )
        }
    }

    fun exportData(context: Context) {
        if (isProcessing) return
        updateState(true, 0, "Đang chuẩn bị xuất dữ liệu...")

        executor.execute {
            runCatching {
                val sections = listOf(
                    "bookings" to "/api/bookings",
                    "tables" to "/api/tables",
                    "menu" to "/api/menu",
                    "invoices" to "/api/invoices"
                )
                val data = JSONObject()

                sections.forEachIndexed { index, (key, endpoint) ->
                    postProgress(
                        10 + index * 20,
                        "Đang tải ${sectionLabel(key)}..."
                    )
                    data.put(key, getJson(endpoint))
                }

                postProgress(90, "Đang ghi tệp JSON...")
                val backup = JSONObject().apply {
                    put("formatVersion", 1)
                    put(
                        "exportedAt",
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                    )
                    put("data", data)
                }

                val directory = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "restaurant-backups"
                ).apply { mkdirs() }
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                val output = File(directory, "restaurant-backup-$timestamp.json")
                output.writeText(backup.toString(2), Charsets.UTF_8)
                output
            }.onSuccess { output ->
                mainHandler.post {
                    lastFilePath = output.absolutePath
                    savedStateHandle["lastFilePath"] = lastFilePath
                    updateState(false, 100, "Xuất dữ liệu thành công")
                }
            }.onFailure { error ->
                mainHandler.post {
                    updateState(false, 0, "Xuất dữ liệu thất bại: ${error.message}")
                }
            }
        }
    }

    fun importReferenceData(context: Context) {
        if (isProcessing) return
        updateState(true, 0, "Đang tìm bản sao gần nhất...")

        executor.execute {
            runCatching {
                val directory = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                    "restaurant-backups"
                )
                val input = directory.listFiles()
                    ?.filter { it.isFile && it.name.startsWith("restaurant-backup-") }
                    ?.maxByOrNull { it.lastModified() }
                    ?: error("Chưa có tệp sao lưu")

                postProgress(25, "Đang kiểm tra cấu trúc JSON...")
                val backup = JSONObject(input.readText(Charsets.UTF_8))
                require(backup.optInt("formatVersion") == 1) {
                    "Phiên bản bản sao không được hỗ trợ"
                }

                val data = backup.getJSONObject("data")
                val payload = JSONObject().apply {
                    put("tables", data.getJSONObject("tables").getJSONArray("tables"))
                    put("menuItems", data.getJSONObject("menu").getJSONArray("items"))
                }

                postProgress(60, "Đang khôi phục bàn và thực đơn...")
                postJson("/api/admin/import-reference-data", payload)
                input
            }.onSuccess { input ->
                mainHandler.post {
                    lastFilePath = input.absolutePath
                    savedStateHandle["lastFilePath"] = lastFilePath
                    updateState(false, 100, "Khôi phục dữ liệu tham chiếu thành công")
                }
            }.onFailure { error ->
                mainHandler.post {
                    updateState(false, 0, "Khôi phục thất bại: ${error.message}")
                }
            }
        }
    }

    private fun getJson(endpoint: String): JSONObject {
        val connection = openConnection(endpoint, "GET")
        return connection.useResponse { code, body ->
            check(code in 200..299) { "Máy chủ trả về lỗi $code" }
            JSONObject(body)
        }
    }

    private fun postJson(endpoint: String, payload: JSONObject): JSONObject {
        val connection = openConnection(endpoint, "POST").apply {
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        }
        return connection.useResponse { code, body ->
            check(code in 200..299) {
                JSONObject(body.ifBlank { "{}" }).optString("message", "Máy chủ trả về lỗi $code")
            }
            JSONObject(body)
        }
    }

    private fun openConnection(endpoint: String, method: String) =
        (URL("$BASE_URL$endpoint").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
        }

    private inline fun <T> HttpURLConnection.useResponse(
        block: (Int, String) -> T
    ): T = try {
        val code = responseCode
        val stream = if (code in 200..299) inputStream else errorStream
        block(code, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
    } finally {
        disconnect()
    }

    private fun postProgress(value: Int, message: String) {
        mainHandler.post { updateState(true, value, message) }
    }

    private fun updateState(processing: Boolean, newProgress: Int, message: String) {
        isProcessing = processing
        progress = newProgress
        statusText = message
        savedStateHandle["isProcessing"] = processing
        savedStateHandle["progress"] = newProgress
        savedStateHandle["statusText"] = message
    }

    private fun sectionLabel(key: String) = when (key) {
        "bookings" -> "đơn đặt bàn"
        "tables" -> "sơ đồ bàn"
        "menu" -> "thực đơn"
        else -> "hóa đơn"
    }

    override fun onCleared() {
        executor.shutdownNow()
        super.onCleared()
    }

    private companion object {
        const val BASE_URL = "http://10.0.2.2:3001"
    }
}

