plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")

}

android {
    namespace = "com.example.smartgymapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartgymapp"
        minSdk = 26
        targetSdk = 34
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
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    viewBinding {
        enable = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation ("androidx.navigation:navigation-ui-ktx:2.6.0")


    //Preference
    implementation("androidx.preference:preference-ktx:1.2.1")



    //circular image
    implementation ("de.hdodenhof:circleimageview:3.1.0")


    //Android Ktx
    implementation ("androidx.navigation:navigation-fragment-ktx:2.4.2")


    implementation ("androidx.biometric:biometric:1.2.0-alpha05") // Fingerprint Authentication

    // Firebase
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.1")
    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")

    // Lotte Animation
    implementation ("com.airbnb.android:lottie:6.1.0")

    //circular image
    implementation("de.hdodenhof:circleimageview:3.1.0")

    //Dots Indicator
    implementation("com.tbuonomo:dotsindicator:5.0")

}
// Allow references to generated code
kapt {
    correctErrorTypes = true
}

