plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Am eliminat alias(libs.plugins.kotlin.compose) care era incorect.
    // buildFeatures { compose = true } este suficient.
    id("com.google.gms.google-services")
}

android {
    namespace = "com.raceup.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raceup.app"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = false
    }
    // Trebuie adăugată această secțiune pentru a specifica unde se află compilatorul Compose
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Asigură-te că aceasta este o versiune compatibilă cu Kotlin-ul tău
    }
}

dependencies {
    // Dependențe de bază AndroidX și Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.compose.ui)
//    implementation(libs.androidx.compose.ui.graphics)
//    implementation(libs.androidx.compose.ui.tooling.preview)
//    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)

//    implementation(platform(libs.firebase.bom))
//    implementation(libs.firebase.auth.ktx)
//    implementation(libs.firebase.firestore.ktx)

    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation(libs.androidx.material3)

//    implementation(platform(libs.firebase.bom))
//
//    // Acum adăugăm librăriile specifice FĂRĂ versiune
//    implementation(libs.firebase.auth.ktx)
//    implementation(libs.firebase.firestore.ktx)



    // Dependențe de testare
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//
//    // Dependențe de debug
//    debugImplementation(libs.androidx.compose.ui.tooling)
//    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
