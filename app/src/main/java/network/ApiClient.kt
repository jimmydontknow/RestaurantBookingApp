package com.example.restaurantbookingapp.network

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

object ApiConfig {
    // 10.0.2.2 là địa chỉ host machine khi chạy trên Android Emulator
    const val BASE_URL = "http://10.0.2.2:3001"

    fun endpoint(path: String): String {
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$BASE_URL$cleanPath"
    }
}

object ApiClient {

    // ----------------------------------------------------------------
    // Public HTTP methods
    // ----------------------------------------------------------------

    fun get(path: String): ApiResult<String> =
        request(path, "GET")

    fun post(path: String, body: JSONObject): ApiResult<String> =
        request(path, "POST", body.toString())

    fun put(path: String, body: JSONObject): ApiResult<String> =
        request(path, "PUT", body.toString())

    fun delete(path: String): ApiResult<String> =
        request(path, "DELETE")

    fun deleteWithBody(path: String, body: JSONObject): ApiResult<String> =
        request(path, "DELETE", body.toString())

    // ----------------------------------------------------------------
    // Internal request builder
    // ----------------------------------------------------------------

    private fun request(
        path: String,
        method: String,
        body: String? = null
    ): ApiResult<String> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(ApiConfig.endpoint(path)).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout    = 15_000
                setRequestProperty("Accept", "application/json")

                // ── Đính kèm JWT token vào mọi request (trừ auth endpoints) ──
                val isAuthPath = path.startsWith("/api/auth/") || path == "/api/health"
                if (!isAuthPath) {
                    val token = TokenManager.getToken()
                    if (!token.isNullOrBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }

                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }

            val code = connection.responseCode

            // ── 401: Token hết hạn hoặc không hợp lệ → xóa phiên ──
            if (code == 401) {
                TokenManager.clearSession()
                val errorBody = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val serverMsg = parseServerMessage(errorBody, "Phiên đăng nhập hết hạn")
                return ApiResult.Error(serverMsg, 401)
            }

            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code in 200..299) {
                ApiResult.Success(response)
            } else {
                ApiResult.Error(parseServerMessage(response, "Lỗi server $code"), code)
            }
        } catch (_: SocketTimeoutException) {
            ApiResult.Error("Kết nối quá lâu, vui lòng kiểm tra server")
        } catch (_: IOException) {
            ApiResult.Error("Không kết nối được server. Kiểm tra mạng hoặc backend port 3001")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Lỗi không xác định")
        } finally {
            connection?.disconnect()
        }
    }

    // ----------------------------------------------------------------
    // Helper: trích xuất message từ JSON response của server
    // ----------------------------------------------------------------

    private fun parseServerMessage(response: String, fallback: String): String {
        return runCatching {
            val json = JSONObject(response)
            json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { fallback }
        }.getOrDefault(fallback)
    }
}

