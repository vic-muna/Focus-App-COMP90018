// Top-level build file. Declares which versions of the Android Gradle
// Plugin (AGP) and Kotlin the project uses, but does not apply them here
// (apply false) - the ":app" module applies them itself.
plugins {
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
