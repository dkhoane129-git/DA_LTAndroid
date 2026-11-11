# ===== SpendWise – R8 / ProGuard =====

# Giữ thông tin generic + annotation để Retrofit/Gson đọc được kiểu Response<Map<String, T>>
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exceptions

# DTO được Gson ánh xạ bằng reflection theo tên field → không được đổi tên
-keep class com.spendwise.app.network.dto.** { *; }

# Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit interface (Retrofit đã kèm consumer rules, giữ thêm cho chắc với R8 full mode)
-keep,allowobfuscation interface com.spendwise.app.network.** { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Ẩn log debug trong bản release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
