plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aalaya.pg"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aalaya.pg"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Android Emulator's special host alias for the development
            // machine. A literal 127.0.0.1 would point at the emulator itself.
            buildConfigField("String", "API_BASE_URL", "\"https://counties-routes-reviewed-confidentiality.trycloudflare.com/api/v1/\"")
            buildConfigField("String", "API_ROOT_URL", "\"https://counties-routes-reviewed-confidentiality.trycloudflare.com/\"")
            // Google sign-in stays hidden until a Google Cloud OAuth client
            // exists. The backend refuses /auth/google without GOOGLE_CLIENT_ID
            // ("GOOGLE_CLIENT_ID missing; /auth/google disabled" at boot), and
            // offering a button that always fails is worse than not offering it.
            // Flip to true once both the Web and Android client IDs are issued.
            buildConfigField("boolean", "GOOGLE_SIGN_IN_ENABLED", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Point this at your deployed backend before shipping.
            buildConfigField("String", "API_BASE_URL", "\"https://api.aalaya.in/api/v1/\"")
            buildConfigField("String", "API_ROOT_URL", "\"https://api.aalaya.in/\"")
            buildConfigField("boolean", "GOOGLE_SIGN_IN_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time (DateFmt) on minSdk 24 requires core-library desugaring —
        // without it the app crashes on Android 7.x devices.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.fonts.google)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)
    implementation(libs.browser)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.datastore.preferences)
    // coil-compose intentionally omitted: image loading is done via
    // plain OkHttp + BitmapFactory — coil was pulled in speculatively.

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}
