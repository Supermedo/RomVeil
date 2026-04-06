import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.glyph.launcher"
    compileSdk = 35

    // ⬇️ Disable splits to fix "AAB Crash" (Force Universal install)
    bundle {
        language { enableSplit = false }
        density { enableSplit = false }
        abi { enableSplit = false }
    }

    defaultConfig {
        applicationId = "com.glyph.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.6"

        // Scraper API keys — set in Settings or leave empty
        buildConfigField("String", "THEGAMESDB_API_KEY", "\"\"")
        buildConfigField("String", "RAWG_API_KEY", "\"\"")
        buildConfigField("String", "MOBYGAMES_API_KEY", "\"\"")

        // Twitch (Retrosm) — from local.properties so client ID/secret are never committed. Settings shows "Default" only (no keys shown).
        // Add to local.properties: twitch.client.id=your_id  and  twitch.client.secret=your_secret
        val localProperties = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { localProperties.load(it) }
        val twitchClientId = (localProperties.getProperty("twitch.client.id") ?: "").trim().replace("\"", "\\\"")
        val twitchClientSecret = (localProperties.getProperty("twitch.client.secret") ?: "").trim().replace("\"", "\\\"")
        val ssDevId = (localProperties.getProperty("screenscraper.dev.id") ?: "").trim().replace("\"", "\\\"")
        val ssDevPass = (localProperties.getProperty("screenscraper.dev.password") ?: "").trim().replace("\"", "\\\"")

        buildConfigField("String", "TWITCH_CLIENT_ID", "\"$twitchClientId\"")
        buildConfigField("String", "TWITCH_CLIENT_SECRET", "\"$twitchClientSecret\"")
        buildConfigField("String", "SCREEN_SCRAPER_DEV_ID", "\"$ssDevId\"")
        buildConfigField("String", "SCREEN_SCRAPER_DEV_PASSWORD", "\"$ssDevPass\"")
    }

    signingConfigs {
        create("release") {
            val keystoreConfigFile = rootProject.file("keystore.properties")
            if (keystoreConfigFile.exists()) {
                val keyProps = Properties()
                keyProps.load(keystoreConfigFile.inputStream())
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
                storeFile = keyProps.getProperty("storeFile")?.let { file(it) }
                storePassword = keyProps.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")

            isMinifyEnabled = false
            isShrinkResources = false
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

    lint {
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.datastore.preferences)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image
    implementation(libs.coil.compose)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
}
