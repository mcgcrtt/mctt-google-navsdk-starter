plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)

    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}
val navSdkVersion by extra("7.8.0")

android {
    namespace = "com.example.navsdkcodelab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.navsdkcodelab"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // 👇 將原本的 KotlinJvmProjectExtension 改為 KotlinAndroidProjectExtension
    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
    }

    // 👇 2. 這是解決 16KB RELRO alignment 的核心關鍵設定
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // 強制要求 AGP 使用現代的無壓縮/分頁對齊方式打包原生庫
            useLegacyPackaging = false
        }
        // 確保編譯器在處理 Navigation SDK 的 .so 檔案時不做錯誤剪裁
        @Suppress("DEPRECATION")
        packagingOptions {
            doNotStrip("*/lib*.so")
        }
    }


}

dependencies {

    // Include the Google Navigation SDK.
    // 1. 強制改用 implementation 引入，並補上完整的群組庫名稱
    implementation("com.google.android.libraries.navigation:navigation:${navSdkVersion}")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 請改成這行（補上 :_nio 尾綴，並將版本換成 2.1.5）：
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
}

secrets {
    // Optionally specify a different file name containing your secrets.
    // The plugin defaults to "local.properties"
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"
}
