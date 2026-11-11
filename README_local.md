# SpendWise – Quản lý chi tiêu cá nhân

Đồ án môn **Lập trình ứng dụng di động** (Android – Kotlin).

## Cấu trúc package (Milestone 2)

```
com.spendwise.app
├── model/        Data class: Transaction, Category, User, MonthSummary, CategoryStat, TransactionItem
├── repository/   Nguồn dữ liệu (MockData, AuthRepository, TransactionRepository, CategoryRepository)
├── viewmodel/    ViewModel + StateFlow / LiveData cho từng màn hình
├── adapter/      RecyclerView Adapter (ListAdapter + DiffUtil + ViewBinding)
├── ui/
│   ├── splash/       SplashActivity
│   ├── auth/         AuthActivity, LoginFragment, RegisterFragment, ForgotPasswordFragment
│   ├── main/         MainActivity (BottomNavigationView + Navigation Component)
│   ├── home/         HomeFragment
│   ├── transactions/ TransactionsFragment, TransactionDetailActivity, AddEditTransactionActivity
│   ├── statistics/   StatisticsFragment
│   ├── profile/      ProfileFragment, EditProfileActivity, CategoryActivity
│   └── common/       TransactionListFragment, CategoryPickerBottomSheet, DatePickerDialogFragment, ConfirmDialogFragment
└── util/         Formatters, Validator, collectWithLifecycle()
```

## Yêu cầu kỹ thuật Milestone 2

| Yêu cầu | Cách đáp ứng |
|---|---|
| View Binding | `buildFeatures { viewBinding = true }` trong `app/build.gradle.kts`. Toàn bộ Activity, Fragment, ViewHolder, Dialog dùng `XxxBinding.inflate(...)`. Không còn `findViewById()` (kiểm tra: `grep -r findViewById app/src` → rỗng). |
| Cấu trúc package | `ui`, `viewmodel`, `model`, `repository`, `adapter` (+ `util`). |
| ViewModel + LiveData/StateFlow | Mock data chuyển hết về `repository/MockData.kt`. Mọi logic lọc, tính tổng, validate form nằm trong ViewModel. Dữ liệu bọc trong `StateFlow` (đa số màn) và `LiveData` (Profile, EditProfile). Bộ lọc / tháng thống kê lưu thêm trong `SavedStateHandle`. |
| Xoay màn hình | ViewModel giữ trạng thái; UI collect bằng `repeatOnLifecycle(STARTED)` nên tự cập nhật lại sau khi Activity được tạo lại. |

Unit test cho ViewModel: `./gradlew testDebugUnitTest`.

## Milestone 3 – Room Database

```
database/
├── AppDatabase.kt        RoomDatabase duy nhất, Singleton (@Volatile + synchronized)
├── Converters.kt         TransactionType <-> String
├── entity/  UserEntity, TransactionEntity, CategoryEntity, BudgetEntity
└── dao/     UserDao, TransactionDao, CategoryDao, BudgetDao
```

| Phân hệ | Entity / DAO | Repository |
|---|---|---|
| Auth / Hồ sơ | `UserEntity` / `UserDao` | `AuthRepository` + `SessionManager` |
| Giao dịch | `TransactionEntity` / `TransactionDao` | `TransactionRepository` |
| Thống kê | `BudgetEntity` / `BudgetDao` (ngân sách tháng) | `BudgetRepository` |
| Danh mục | `CategoryEntity` / `CategoryDao` | `CategoryRepository` |

- Ghi (Insert / Update / Delete / Upsert): `suspend fun`.
- Đọc: trả về `Flow<List<T>>` (hoặc `Flow<T?>`), ViewModel `stateIn` → UI tự cập nhật khi bảng thay đổi.
- `AppContainer` (tạo trong `SpendWiseApp`) giữ các repository; ViewModel nhận qua `viewModelFactory`.
- Danh mục mặc định được nạp khi cài app lần đầu; Tài khoản → "Tạo dữ liệu mẫu" để thêm 16 giao dịch mẫu.
- Schema xuất ra `app/schemas/` để theo dõi thay đổi CSDL.

**Kiểm thử Offline:** bật Airplane Mode trên AVD → mở app: phiên đăng nhập, giao dịch, thống kê, ngân sách vẫn đọc/ghi bình thường vì tất cả nằm trong Room.

Test DAO (Robolectric, chạy trên JVM): `./gradlew testDebugUnitTest`.

## Milestone 4 – Retrofit + Coroutines + Resource State

- `AndroidManifest.xml`: `INTERNET`, `ACCESS_NETWORK_STATE`.
- `network/`: `ApiClient` (1 OkHttpClient dùng chung), `ExchangeRateApi`, `CloudSyncApi`, DTO, `safeApiCall` (chạy trên `Dispatchers.IO`).
- `util/Resource.kt`: `sealed class Resource { Loading, Success(data), Error(message, code, data) }`.
- Layout dùng chung `view_state.xml` + `ViewStateBinding.render()`: Loading → ProgressBar, Error → banner + nút Thử lại.

| Phân hệ | API | Màn hình |
|---|---|---|
| Tỷ giá ngoại tệ | `@GET v6/latest/{base}` – open.er-api.com (không cần key) | Tài khoản → Tỷ giá ngoại tệ (`CurrencyActivity`) – đổi VND sang 12 ngoại tệ, cache Room (`ExchangeRateEntity`, DB v2 AutoMigration) để offline vẫn xem được |
| Đồng bộ đám mây | `@GET` / `@POST` / `@PUT` / `@DELETE` – Firebase Realtime Database REST (`users/{uid}/transactions.json?auth=`) | Tài khoản → Đồng bộ đám mây (`CloudSyncActivity`) – Tải lên / Tải về / chạm để xóa bản trên mây |

Xử lý lỗi (`network/SafeApiCall.kt`): mất mạng / timeout, HTTP 400, 401, 403, 404, 429, 5xx → thông báo tiếng Việt.
Ở Milestone 4 tài khoản vẫn là mock, chưa có ID token → server trả **401** (minh họa trạng thái Error). Milestone 5 thay bằng Firebase Auth.

Test: `SafeApiCallTest` (MockWebServer) kiểm Success, 400/401/500, mất mạng và đúng định dạng request Firebase REST.

## Milestone 5 – Firebase, Glide, Hiệu năng, Bảo mật, Xuất bản

| Hạng mục | Cách làm |
|---|---|
| **Firebase Authentication** | `AuthRepository` dùng `FirebaseAuth` (Email/Password): đăng ký (kèm `displayName`), đăng nhập, quên mật khẩu, đăng xuất. Lỗi Firebase được đổi sang tiếng Việt. Firebase lưu phiên trên máy nên mở app offline vẫn vào được. ID token (`getIdToken`) dùng cho API đồng bộ → Realtime Database chỉ cho user đọc/ghi `users/{uid}` của chính mình. |
| **Glide** | Cờ 12 quốc gia trong danh sách tỷ giá (`CurrencyAdapter`, CardView + RecyclerView) và avatar ở tab Tài khoản. Dùng `override()` đúng kích thước, `placeholder/error`, `circleCrop()`, `clear()` trong `onViewRecycled`. Glide gắn với vòng đời Fragment/View nên tự hủy request. |
| **Hiệu năng & rò rỉ bộ nhớ** | `debugImplementation leakcanary-android`. Đã sửa: `StatisticsFragment` giữ `CategoryStatAdapter` làm field → adapter giữ RecyclerView cũ sau `onDestroyView` (leak). Nay tạo adapter theo vòng đời View và gán `adapter = null` khi hủy. Mọi Fragment đều `_binding = null` trong `onDestroyView`; collect Flow bằng `repeatOnLifecycle`; ListAdapter + DiffUtil; `setHasFixedSize`; Room/Retrofit chạy ngoài main thread. |
| **Bảo mật & mã hóa** | `SessionManager` dùng **EncryptedSharedPreferences** (MasterKey AES256-GCM trong Android Keystore; key AES256-SIV, value AES256-GCM). `allowBackup=false`. URL/khóa đọc từ `secrets.properties` → `BuildConfig` (không nằm trong code). `google-services.json`, `secrets.properties`, `keystore.properties`, `*.jks` đều nằm trong `.gitignore`. Log OkHttp che `?auth=` và tắt hẳn ở bản release; R8 xóa `Log.d/v`. |
| **Đóng gói xuất bản** | Keystore `keystore/spendwise-release.jks` (RSA 2048, 10000 ngày). `release`: `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-rules.pro` giữ DTO cho Gson/Retrofit. `./gradlew assembleRelease bundleRelease` → APK ~3,4 MB (debug ~11 MB) và AAB. |

## Đồng bộ tự động nhiều thiết bị (v1.1.0)

Đăng nhập cùng tài khoản trên máy khác → dữ liệu tự có. `SyncManager` gọi `SyncRepository.fullSync()` khi:
đăng nhập / mở app, app quay lại foreground (`ProcessLifecycleOwner`), sau mỗi lần thêm / sửa / xóa giao dịch, và khi có mạng trở lại (`NetworkCallback`).
Các yêu cầu được gộp (debounce 1,5 s), chỉ 1 lần đồng bộ chạy tại một thời điểm (`Mutex`).

`fullSync()`: gửi các lệnh xóa đang chờ (`PendingDeleteEntity`, DB v3) → `@GET` kéo về (thêm mới, cập nhật bản cloud mới hơn, xóa bản đã bị xóa ở máy khác) → `@POST` bản chưa có / `@PUT` bản local mới hơn. Xung đột: `updatedAt` lớn hơn thắng.
Trạng thái hiện dưới lời chào ở Home (chạm để đồng bộ ngay). Test: `MultiDeviceSyncTest` mô phỏng 2 máy.

## Phân công (Vertical Slicing)

| Phân hệ | Màn hình / lớp | Thành viên |
|---|---|---|
| Auth | Splash, Auth, Login, Register, ForgotPassword + ViewModel · `UserEntity/UserDao` · Firebase Auth · EncryptedSharedPreferences | _(điền tên)_ |
| Giao dịch | Home, Transactions, Detail, AddEdit, TransactionListFragment, CategoryPicker · `TransactionEntity/TransactionDao` | _(điền tên)_ |
| Thống kê | Statistics, DatePickerDialogFragment, CategoryStatAdapter · `BudgetEntity/BudgetDao` | _(điền tên)_ |
| Tài khoản | Profile, EditProfile, Category, ConfirmDialogFragment · `CategoryEntity/CategoryDao` · Tỷ giá (API) · Đồng bộ đám mây (API) | _(điền tên)_ |

## Chạy thử

1. Đặt `app/google-services.json` (tải từ Firebase Console, package `com.spendwise.app`).
2. Tạo `secrets.properties` ở thư mục gốc theo mẫu `secrets.properties.example`.
3. (Tuỳ chọn, để build Release) tạo `keystore.properties` theo mẫu `keystore.properties.example`.
4. Mở bằng Android Studio → Run. Đăng ký tài khoản mới ngay trong app (Firebase Auth).
