// Module-level build file for the ":app" module - the actual Android app.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Kotlin 2.0's Compose compiler plugin (replaces the old
    // composeOptions { kotlinCompilerExtensionVersion = ... } approach).
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.focusapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.focusapp"
        minSdk = 26      // Android 8.0 - needed later for reliable AccessibilityService / Geofencing use
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-skeleton"
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- Jetpack Compose (UI layer) ---
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // Extended icon set (not just material-icons-core) - needed for
    // Icons.Filled.Apps / Icons.Filled.Map used in the bottom nav bar,
    // which aren't part of the small default core icon bundle.
    implementation("androidx.compose.material:material-icons-extended")

    // --- Navigation between screens (bottom nav: Focus Mode / History / Rewards / Settings) ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- Activity + ViewModel + lifecycle glue for Compose ---
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.core:core-ktx:1.13.1")

    // Needed because AndroidManifest.xml's application theme uses
    // "Theme.Material3.DayNight.NoActionBar", which is defined by Google's
    // Material Components library, not by Compose's material3 artifact
    // above (those are two separate things with confusingly similar
    // names - Compose's MaterialTheme{} in MainActivity.kt doesn't need
    // this at all, but the manifest's XML window theme does).
    implementation("com.google.android.material:material:1.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Testing (empty for now, but kept so Android Studio's default
    //     "Run Tests" action has somewhere to look) ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
