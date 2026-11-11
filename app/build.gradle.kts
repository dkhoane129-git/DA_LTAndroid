import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

/**
 * Milestone 5 – ẩn khóa / URL khỏi mã nguồn công khai:
 * đọc từ secrets.properties (đã .gitignore), hoặc biến môi trường khi build CI.
 * Mẫu: secrets.properties.example
 */
val secrets = Properties().apply {
    val file = rootProject.file("secrets.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(key: String, default: String = ""): String =
    secrets.getProperty(key) ?: System.getenv(key) ?: default

/** Thông tin keystore ký bản release (keystore.properties – đã .gitignore). */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.spendwise.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spendwise.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.1.0"

        buildConfigField("String", "EXCHANGE_BASE_URL", "\"${secret("EXCHANGE_BASE_URL", "https://open.er-api.com/")}\"")
        buildConfigField("String", "FIREBASE_DB_URL", "\"${secret("FIREBASE_DB_URL")}\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: thu gọn + làm rối mã, loại bỏ resource không dùng
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Máy không tải được android-all qua mạng: ./gradlew test -ProbolectricOfflineDir=<thư mục chứa jar>
            project.findProperty("robolectricOfflineDir")?.let { dir ->
                it.systemProperty("robolectric.offline", "true")
                it.systemProperty("robolectric.dependency.dir", dir.toString())
            }
        }
    }

    // Milestone 2: bật View Binding, bỏ hoàn toàn findViewById()
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    // ViewModel + LiveData / StateFlow
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Navigation Component (BottomNavigationView + Auth flow)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Room Database (Milestone 3)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp (Milestone 4)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Milestone 5
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.glide)
    implementation(libs.androidx.security.crypto)
    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.test.core)
}
