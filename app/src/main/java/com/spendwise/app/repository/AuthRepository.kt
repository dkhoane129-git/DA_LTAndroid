package com.spendwise.app.repository

import android.net.Uri
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.spendwise.app.database.dao.UserDao
import com.spendwise.app.database.entity.UserEntity
import com.spendwise.app.database.entity.toModel
import com.spendwise.app.model.User
import com.spendwise.app.network.TokenProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Milestone 5: Đăng ký / Đăng nhập bằng Firebase Authentication (Email/Password).
 * Hồ sơ vẫn được lưu Room (UserEntity) để hiển thị khi offline;
 * uid + email lưu trong SessionManager (EncryptedSharedPreferences).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val session: SessionManager
) : TokenProvider {

    val currentUser: Flow<User?> = session.uid.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else userDao.observeUser(uid).map { it?.toModel() }
    }

    val currentUid: String? get() = session.uid.value

    /** Firebase tự lưu phiên trên máy nên vẫn trả về true khi offline. */
    fun isLoggedIn(): Boolean = auth.currentUser != null && session.uid.value != null

    suspend fun login(email: String, password: String): Result<User> = runAuth {
        val fbUser = auth.signInWithEmailAndPassword(email, password).await().user!!
        saveLocal(fbUser)
    }

    suspend fun register(fullName: String, email: String, password: String): Result<User> = runAuth {
        val fbUser = auth.createUserWithEmailAndPassword(email, password).await().user!!
        fbUser.updateProfile(userProfileChangeRequest {
            displayName = fullName
            photoUri = Uri.parse(avatarUrl(fullName))
        }).await()
        saveLocal(fbUser, fullName)
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runAuth {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun updateProfile(fullName: String, phone: String) {
        val uid = session.uid.value ?: return
        userDao.updateProfile(uid, fullName, phone, avatarUrl(fullName))
        // Cập nhật tên trên Firebase; offline thì bỏ qua, dữ liệu local vẫn đúng
        runCatching {
            auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = fullName })?.await()
        }
    }

    fun logout() {
        auth.signOut()
        session.clear()
    }

    /** ID token gửi kèm REST API của Realtime Database (?auth=...). */
    override suspend fun idToken(): String? =
        runCatching { auth.currentUser?.getIdToken(false)?.await()?.token }.getOrNull()

    private suspend fun saveLocal(fbUser: FirebaseUser, nameOverride: String? = null): User {
        val existing = userDao.getUser(fbUser.uid)
        val name = nameOverride ?: existing?.fullName ?: fbUser.displayName
            ?: fbUser.email.orEmpty().substringBefore("@")
        val entity = UserEntity(
            uid = fbUser.uid,
            fullName = name,
            email = fbUser.email.orEmpty(),
            phone = existing?.phone.orEmpty(),
            avatarUrl = avatarUrl(name)
        )
        userDao.upsert(entity)
        session.save(fbUser.uid, entity.email)
        return entity.toModel()
    }

    /** Ảnh đại diện tạo từ tên (ui-avatars.com), tải bằng Glide. */
    private fun avatarUrl(name: String): String =
        "https://ui-avatars.com/api/?size=256&bold=true&background=2E7D5B&color=fff&name=" +
            Uri.encode(name.ifBlank { "U" })

    /** Đổi exception của Firebase thành thông báo tiếng Việt. */
    private inline fun <T> runAuth(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        val message = when (e) {
            is FirebaseNetworkException -> "Không có kết nối mạng"
            is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu (tối thiểu 6 ký tự)"
            is FirebaseAuthUserCollisionException -> "Email đã được sử dụng"
            is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại hoặc đã bị khóa"
            is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu không đúng"
            else -> e.localizedMessage ?: "Đã có lỗi xảy ra"
        }
        Result.failure(Exception(message, e))
    }
}
