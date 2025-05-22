plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.studin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.studin"
        minSdk = 24
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // Importa el BoM de Firebase aquí.
    // Esto le dice a Gradle que use el catálogo de versiones para las dependencias de Firebase.
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // Ahora solo necesitas la dependencia principal de Realtime Database
    // Ya no necesitas firebase-database-ktx si usas BoM 32.5.0+
    // implementation(libs.firebase.database.ktx) // <-- BORRA ESTA LÍNEA en app/build.gradle.kts
    implementation(libs.firebase.database)
    // ** AÑADE ESTA LÍNEA PARA FIREBASE AUTHENTICATION **
    // La versión es gestionada por el BoM que importaste arriba
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
