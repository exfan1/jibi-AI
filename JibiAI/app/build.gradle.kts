plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.jibi.ai"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.jibi.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}
