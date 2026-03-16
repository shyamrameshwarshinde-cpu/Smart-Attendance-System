plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.smart_attendance_system"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smart_attendance_system"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    // ✅ FIXED: renamed from packagingOptions (deprecated)
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        xmlReport = false
        htmlReport = true
        disable += setOf(
            "MissingPermission",
            "ObsoleteSdkInt",
            "UnusedResources",
            "GradleDependency",
            "VectorPath"
        )
    }
}

dependencies {

    // ─── Multidex ───
    implementation("androidx.multidex:multidex:2.0.1")

    // ─── Core Desugaring ───
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ─── AndroidX ───
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    // ─── Material UI ───
    implementation("com.google.android.material:material:1.12.0")

    // ─── Firebase ───
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // ─── Google Play Services ───
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ─── ML Kit ───
    implementation("com.google.mlkit:face-detection:16.1.6")

    // ─── TensorFlow Lite ───
    implementation("org.tensorflow:tensorflow-lite:2.14.0")

    // ─── Apache POI (Excel) ───
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    implementation ("androidx.cardview:cardview:1.0.0")
    // ─── Testing ───
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

}