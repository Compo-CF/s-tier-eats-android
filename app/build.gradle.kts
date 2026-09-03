plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.compofelice.stiereats"
    // API 36 (Android 16): required by Google Play's target-API bar effective
    // Aug 31, 2026 (was API 35). targetSdk must be ≤ compileSdk, so both move
    // together. AGP 8.7.3 predates API 36, so gradle.properties carries
    // android.suppressUnsupportedCompileSdk=36 to silence the "untested SDK"
    // warning — safe here because we use no API-36-specific APIs.
    compileSdk = 36

    // Secrets/config from local.properties (gitignored) with an env-var
    // fallback for CI. Simple line-parse — the java.util.Properties().apply{}
    // form breaks Kotlin-DSL type inference here.
    val localProps: Map<String, String> = rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.mapNotNull { line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) null
            else t.substringBefore("=").trim() to t.substringAfter("=").trim()
        }
        ?.toMap()
        ?: emptyMap()
    fun secret(key: String): String? = localProps[key] ?: System.getenv(key)

    defaultConfig {
        applicationId = "com.compofelice.stiereats"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.1.1"
        vectorDrawables { useSupportLibrary = true }

        // Maps SDK key → manifest placeholder. Empty if unset (map tiles just
        // stay blank; everything else works).
        manifestPlaceholders["MAPS_API_KEY"] = secret("MAPS_API_KEY") ?: ""
    }

    signingConfigs {
        // Shared, committed debug keystore so every build environment (Windows,
        // MacInCloud, CI) signs debug builds with the SAME key → one SHA-1
        // registered in Firebase works everywhere. Debug key = throwaway, safe
        // to commit; the release key is NOT committed (see .gitignore).
        getByName("debug") {
            storeFile = file("$rootDir/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Release (upload) key. Creds come from local.properties / env, and the
        // keystore itself is gitignored — never committed. Only wired up when
        // the keystore + password are actually present, so debug/CI builds that
        // lack them are unaffected.
        create("release") {
            val ksFile = rootProject.file(secret("RELEASE_STORE_FILE") ?: "keystore/release.keystore")
            val ksPass = secret("RELEASE_STORE_PASSWORD")
            if (ksFile.exists() && ksPass != null) {
                storeFile = ksFile
                storePassword = ksPass
                keyAlias = secret("RELEASE_KEY_ALIAS") ?: "upload"
                keyPassword = secret("RELEASE_KEY_PASSWORD") ?: ksPass
            }
        }
    }

    buildTypes {
        release {
            // Use the release signing config when its creds are present;
            // otherwise leave unsigned so `assembleDebug`/CI still work.
            val relSigning = signingConfigs.getByName("release")
            if (relSigning.storeFile != null) signingConfig = relSigning
            isMinifyEnabled = false
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
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true   // AboutScreen reads BuildConfig.VERSION_NAME/CODE
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.play.services.maps)
    implementation(libs.coil.compose)
    implementation(libs.play.services.ads)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.android)
}
