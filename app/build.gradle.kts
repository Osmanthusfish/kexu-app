plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.schedule.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.schedule.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 48
        versionName = "1.48"
        manifestPlaceholders["appLabel"] = "课序"
        testInstrumentationRunner = "com.schedule.app.RegressionInstrumentation"
    }
    buildTypes {
        debug {
            if (providers.gradleProperty("isolatedReview").orNull == "true") {
                applicationIdSuffix = ".review"
                manifestPlaceholders["appLabel"] = "课序·测试"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = "2024.11.00"
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")
    // Bundled Simplified Chinese OCR model: offline recognition without uploading selected images.
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    testImplementation("junit:junit:4.13.2")
    // JVM only; Android supplies org.json at runtime. Approved backup regression dependency.
    testImplementation("org.json:json:20240303")
    constraints {
        // Match the app's already resolved collection version; no new library or version download.
        androidTestImplementation("androidx.collection:collection:1.4.4")
    }
    debugImplementation("androidx.compose.ui:ui-tooling")
}
