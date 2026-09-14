plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android { namespace = "com.gainztracker.health"; compileSdk = 35
    defaultConfig { applicationId = "com.gainztracker.health"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha12")
    implementation("androidx.webkit:webkit:1.12.1")
}
