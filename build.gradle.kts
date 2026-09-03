// Top-level build file. Declares which versions of the Android Gradle
// Plugin (AGP) and Kotlin the project uses, but does not apply them here
// (apply false) - the ":app" module applies them itself.
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}
