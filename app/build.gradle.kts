// Module-level build file for the ":app" module - the actual Android app.
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Kotlin 2.0's Compose compiler plugin (replaces the old
    // composeOptions { kotlinCompilerExtensionVersion = ... } approach).
    alias(libs.plugins.kotlin.compose)
}

//Adding auto api importer
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
}

tasks.register("generateGoogleServicesJson") {
    doLast {
        val templateFile = file("google-services.json.template")
        val outputFile = file("google-services.json")
        val content = templateFile.readText().replace(
            "REPLACE_WITH_YOUR_KEY",
            localProps.getProperty("FIREBASE_API_KEY", "").trim()
        )
        outputFile.writeText(content)
    }
}

tasks.named("preBuild") {
    dependsOn("generateGoogleServicesJson")
}

android {
    namespace = "com.example.focusapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.focusapp"
        minSdk = 26      // Android 8.0 - needed later for reliable AccessibilityService / Geofencing use
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-skeleton"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Extended icon set (not just material-icons-core) - needed for
    // Icons.Filled.Apps / Icons.Filled.Map used in the bottom nav bar,
    // which aren't part of the small default core icon bundle.
    implementation(libs.androidx.material.icons.extended)

    // --- Navigation between screens (bottom nav: Focus Mode / History / Rewards / Settings) ---
    implementation(libs.androidx.navigation.compose)

    // --- Activity + ViewModel + lifecycle glue for Compose ---
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.ktx)

    // Needed because AndroidManifest.xml's application theme uses
    // "Theme.Material3.DayNight.NoActionBar", which is defined by Google's
    // Material Components library, not by Compose's material3 artifact
    // above (those are two separate things with confusingly similar
    // names - Compose's MaterialTheme{} in MainActivity.kt doesn't need
    // this at all, but the manifest's XML window theme does).
    implementation(libs.material)

    debugImplementation(libs.androidx.ui.tooling)

    // --- Testing (empty for now, but kept so Android Studio's default
    //     "Run Tests" action has somewhere to look) ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
