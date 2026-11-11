package com.spendwise.app.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.GeneralSecurityException

/**
 * Lưu thông tin phiên (uid, email, thời điểm đồng bộ) bằng EncryptedSharedPreferences:
 * key mã hóa AES256-SIV, value mã hóa AES256-GCM, khóa chính nằm trong Android Keystore.
 * Mở file XML trong /data/data/... chỉ thấy chuỗi đã mã hóa.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    private val _uid = MutableStateFlow(prefs.getString(KEY_UID, null))
    val uid: StateFlow<String?> = _uid.asStateFlow()

    val email: String? get() = prefs.getString(KEY_EMAIL, null)

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    fun save(uid: String, email: String) {
        prefs.edit().putString(KEY_UID, uid).putString(KEY_EMAIL, email).apply()
        _uid.value = uid
    }

    fun clear() {
        prefs.edit().clear().apply()
        _uid.value = null
    }

    private companion object {
        const val PREFS_NAME = "secure_session"
        const val KEY_UID = "uid"
        const val KEY_EMAIL = "email"
        const val KEY_LAST_SYNC = "last_sync_at"

        fun createPrefs(context: Context): SharedPreferences = try {
            open(context)
        } catch (e: GeneralSecurityException) {
            // Khóa Keystore hỏng (vd: khôi phục từ bản sao lưu máy khác) → xóa file cũ, tạo lại
            context.deleteSharedPreferences(PREFS_NAME)
            open(context)
        } catch (e: java.io.IOException) {
            context.deleteSharedPreferences(PREFS_NAME)
            open(context)
        }

        fun open(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
