plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    alias(libs.plugins.androidx.navigation.safe.args)
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.androidapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.androidapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AWS Cognito Configuration
        buildConfigField("String", "COGNITO_USER_POOL_ID", "\"eu-north-1_eOvAx8nlu\"")
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"7f8b8tgho76tcl9dmirq2tomar\"")
        buildConfigField("String", "COGNITO_REGION", "\"eu-north-1\"")

        // Backend API Configuration
        //buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8081/api/v1/\"")
        buildConfigField("String", "BASE_URL", "\"http://192.168.91.17:8081/api/v1/\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            // debug settings if needed
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("profile") {
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("C:/Users/AnujiWeragoda/Android_Project/Kotlin_Development_MVVM/flutter_module/build/host/outputs/repo") }
    maven { url = uri("https://storage.googleapis.com/download.flutter.io") }
}

dependencies {
    // AndroidX and UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.scalars)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // AWS
    implementation(libs.aws.auth.cognito)
    implementation(libs.aws.core)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.firebase.crashlytics)
    kapt(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Misc
    implementation(libs.timber)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    coreLibraryDesugaring(libs.core.desugar)


    // Flutter Module
    // Include the flutter module project for all variants
    debugImplementation(project(":flutter"))
    releaseImplementation(project(":flutter"))
    add("profileImplementation", project(":flutter"))


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Adyen Checkout with Compose support
    implementation("com.adyen.checkout:drop-in-compose:5.15.0")
    implementation("com.adyen.checkout:sessions-core:5.15.0")
    implementation("com.adyen.checkout:card:5.15.0")

    //Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
}
