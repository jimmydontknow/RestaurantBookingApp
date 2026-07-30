package com.example.restaurantbookingapp.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * TokenManager — Quản lý phiên đăng nhập JWT bằng EncryptedSharedPreferences.
 *
 * Dữ liệu được mã hóa AES-256-GCM (key) + AES-256-SIV (value) trên thiết bị,
 * không thể đọc bằng ADB backup hay file explorer thông thường.
 *
 * Khởi tạo một lần trong Application hoặc MainActivity bằng `TokenManager.init(context)`.
 */
object TokenManager {

    private const val PREFS_FILE = "secure_session"
    private const val KEY_TOKEN        = "jwt_token"
    private const val KEY_USER_ID      = "user_id"
    private const val KEY_USERNAME     = "username"
    private const val KEY_ROLE         = "role"
    private const val KEY_FULL_NAME    = "full_name"
    private const val KEY_PHONE        = "phone"

    private var prefs: SharedPreferences? = null

    // ----------------------------------------------------------------
    // Khởi tạo — gọi 1 lần từ MainActivity.onCreate()
    // ----------------------------------------------------------------
    fun init(context: Context) {
        if (prefs != null) return          // Đã khởi tạo rồi
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ----------------------------------------------------------------
    // Lưu phiên sau khi đăng nhập / đăng ký thành công
    // ----------------------------------------------------------------
    fun saveSession(
        token: String,
        userId: String,
        username: String,
        role: String,
        fullName: String,
        phone: String
    ) {
        requirePrefs().edit()
            .putString(KEY_TOKEN,     token)
            .putString(KEY_USER_ID,   userId)
            .putString(KEY_USERNAME,  username)
            .putString(KEY_ROLE,      role)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_PHONE,     phone)
            .apply()
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------
    fun getToken():    String? = requirePrefs().getString(KEY_TOKEN,     null)
    fun getUserId():   String? = requirePrefs().getString(KEY_USER_ID,   null)
    fun getUsername(): String? = requirePrefs().getString(KEY_USERNAME,  null)
    fun getRole():     String? = requirePrefs().getString(KEY_ROLE,      null)
    fun getFullName(): String? = requirePrefs().getString(KEY_FULL_NAME, null)
    fun getPhone():    String? = requirePrefs().getString(KEY_PHONE,     null)

    /** Trả về true nếu có token được lưu (user đã đăng nhập). */
    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    // ----------------------------------------------------------------
    // Xóa phiên khi đăng xuất hoặc token hết hạn
    // ----------------------------------------------------------------
    fun clearSession() {
        requirePrefs().edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_ROLE)
            .remove(KEY_FULL_NAME)
            .remove(KEY_PHONE)
            .apply()
    }

    // ----------------------------------------------------------------
    private fun requirePrefs(): SharedPreferences {
        return prefs
            ?: error("TokenManager chưa được khởi tạo. Gọi TokenManager.init(context) trong MainActivity.onCreate().")
    }
}
