plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.compofelice.stiereats"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.compofelice.stiereats"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }

        // Maps SDK key for the manifest placeholder. Sourced (in order) from
        // local.properties (gitignored — where you put it locally), a Gradle
        // property, or the MAPS_API_KEY env var (CI secret). Empty if unset —
        // map tiles just stay blank, everything else works.
        val mapsApiKey: String = run {
            val local = rootProject.file("local.properties")
            val fromLocal = if (local.exists()) {
                java.util.Properties().apply { local.inputStream().use { load(it) } }
                    .getProperty("MAPS_API_KEY")
            } else null
            fromLocal
                ?: (project.findProperty("MAPS_API_KEY") as String?)
                ?: System.getenv("MAPS_API_KEY")
                ?: ""
        }
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
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
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.android)
}
