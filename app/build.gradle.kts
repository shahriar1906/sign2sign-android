plugins {
    // This is the modern way to declare plugins using the version catalog
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.sign2sign"
    // FIX: Updated to 36 as required by the dependencies
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.sign2sign"
        minSdk = 24
        // FIX: Also update targetSdk to match compileSdk
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Updated to a more modern Java version.
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // CLEANUP: Using only the Version Catalog dependencies that you already have set up.
    // This avoids conflicts and keeps versions managed in one place (libs.versions.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity) // Often needed

    // CameraX dependencies
    val cameraxVersion = "1.3.1" // It's fine to keep this manual if not in your version catalog
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    // TensorFlow Lite dependencies
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4") // Using a slightly newer, stable version
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("com.google.mlkit:translate:17.0.2")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}