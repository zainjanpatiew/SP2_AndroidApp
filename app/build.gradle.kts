import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.libsDirectory

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("com.google.gms.google-services")
    id ("kotlin-kapt")
}

android {
    namespace = "com.example.ermes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ermes"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures{
        viewBinding=true
    }
    kotlinOptions {
        jvmTarget = "1.8"
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

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    //implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.firebase:firebase-firestore-ktx")

    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.sunmi:SunmiAuthorize-SDK:1.0.1")
    implementation(files("libs/facelib-release.aar"))

    //daxter permissions handler
    implementation ("com.karumi:dexter:6.2.3")

    //Image loading library
    implementation ("com.github.bumptech.glide:glide:4.15.1")


    implementation ("androidx.room:room-runtime:2.6.1")
    kapt ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.room:room-ktx:2.6.1")



}