import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun getSecretProperty(key: String): String {
    val value = localProps.getProperty(key) ?: System.getenv(key) ?: ""
    return value.trim().removeSurrounding("\"")
}

android {
    namespace = "com.jongh0.bassshot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jongh0.bassshot"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "1.3.0"

        buildConfigField("String", "GEMINI_API_KEY", "\"${getSecretProperty("GEMINI_API_KEY")}\"")
        buildConfigField("String", "WEATHER_API_KEY", "\"${getSecretProperty("WEATHER_API_KEY")}\"")
        buildConfigField("String", "MAPS_API_KEY", "\"${getSecretProperty("MAPS_API_KEY")}\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"jongh0\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"bass-shot\"")
        val ghToken = getSecretProperty("GH_ISSUE_TOKEN").ifBlank { getSecretProperty("GITHUB_ISSUE_TOKEN") }
        buildConfigField("String", "GH_ISSUE_TOKEN", "\"$ghToken\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "bassshot1234"
                keyAlias = "bassshot"
                keyPassword = "bassshot1234"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // CameraX & Exif
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)

    // Google AI SDK
    implementation(libs.google.ai.generativeai)

    // Location
    implementation(libs.play.services.location)

    // Network & Serialization
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines, Coil, DataStore
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
}
