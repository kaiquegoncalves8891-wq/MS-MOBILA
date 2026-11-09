plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.msmobila.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.msmobila.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-prototipo"
    }
}
