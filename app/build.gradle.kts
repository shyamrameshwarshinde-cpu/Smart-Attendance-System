plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Firebase
}

android {
    namespace = "com.example.smart_attendance_system"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.smart_attendance_system"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity:1.7.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.6")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Excel (Apache POI)
    implementation("org.apache.poi:poi:5.2.2")
    implementation("org.apache.poi:poi-ooxml:5.2.2")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.10"))

    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation ("com.google.mlkit:face-detection:16.1.5")

        implementation ("com.google.android.material:material:1.9.0' // latest stable")


}
