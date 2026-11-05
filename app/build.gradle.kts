import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.lamplore"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.lamplore"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.5.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // Release signing: the key lives outside the repo. Point LAMPLORE_SIGNING at a properties file
    // (storeFile, storePassword, keyAlias, keyPassword). Without it the release build is unsigned.
    val signingProps = System.getenv("LAMPLORE_SIGNING")?.let { file(it) }?.takeIf { it.exists() }
        ?.let { f -> Properties().also { p -> f.inputStream().use(p::load) } }
    if (signingProps != null) {
        signingConfigs.create("release") {
            storeFile = file(signingProps.getProperty("storeFile"))
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingProps != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // On-device Piper / Kokoro voices. Pinned to 1.13.8: sherpa-onnx 2.x removed espeak-ng,
    // which these voice packs need. Download the AAR (see README) into app/libs.
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))
    implementation("org.apache.commons:commons-compress:1.28.0")

    // On-device language model for the AI Director (arm64 only; guarded at run time by ai/DeviceCheck).
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.19.0")

    // optional app lock
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // background check for new podcast episodes
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    val media3 = "1.11.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-common:$media3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
