plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.finanzas_independientes_app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.finanzas_independientes_app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:9090/\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://businesscontrol.azurewebsites.net/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Retrofit y Gson para la red
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    // OkHttp: cliente, logging e interceptores (BOM alinea versiones)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    // Almacenamiento seguro de tokens (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)
    // Librerías para ViewModel y viewModelScope
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Hilt: dependency injection (KSP compiler for speed over kapt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Charts: weekly bars, monthly trend line, category pie (Fase 5)
    implementation(libs.mpandroidchart)
    // Room: offline-first local cache (Fase 7)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Splash Screen API: fast, themed cold-start (Fase 7)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.kotlinx.coroutines.test)
}