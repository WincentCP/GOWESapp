plugins {
    id("com.android.application")
    // DIUBAH: Menggunakan alias camelCase
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.uph.m23si1.gowesapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.uph.m23si1.gowesapp"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    // Dependensi default (dari TOML Anda)
    // DIUBAH: Menggunakan alias camelCase
    implementation(libs.androidxCoreKtx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.extJunit)
    androidTestImplementation(libs.espressoCore)

    // Dependensi kustom (dari TOML Anda)
    implementation(libs.circleimageview)
    implementation(libs.androidxCameraCamera2)
    implementation(libs.androidxCameraLifecycle)
    implementation(libs.androidxCameraView)
    implementation(libs.mlkitBarcodeScanning)
    implementation(libs.guava)
}
    // 4. Tambahkan dependensi Firebase
    // Import Bill of Materials (BOM)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Tambahkan dependensi untuk produk Firebase
    implementation(libs.firebaseAuth)
    implementation(libs.firebaseFirestore)
    implementation(libs.firebaseDatabase)

    // 5. Tambahkan dependensi ML Kit (pengganti Firebase ML Vision)
    implementation(libs.mlkitBarcodeScanning)
}