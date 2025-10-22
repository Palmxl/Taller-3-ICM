plugins {
    // Plugins base de Android y Kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Google Services (para Firebase)
    id("com.google.gms.google-services")

    // Kapt (necesario para Glide)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.taller_3_icm"
    compileSdk = 34 // ✅ Último SDK compatible con tus dependencias

    defaultConfig {
        applicationId = "com.example.taller_3_icm"
        minSdk = 24
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
        // Compatibilidad con Java 11
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        // Habilita ViewBinding para acceder a vistas sin findViewById()
        viewBinding = true
    }
}

dependencies {
    // --- AndroidX base ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")

    // --- Glide (imágenes) ---
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // --- Mapas (OSMDroid + BonusPack + ubicación Google) ---
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.MKergall:osmbonuspack:6.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // --- Firebase (autenticación, base de datos y almacenamiento) ---
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // --- Tests ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
