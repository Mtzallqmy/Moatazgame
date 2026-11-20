plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val releaseKeystore = providers.environmentVariable("MASALIK_KEYSTORE")
val releaseStorePassword = providers.environmentVariable("MASALIK_STORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("MASALIK_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("MASALIK_KEY_PASSWORD")

android {
    namespace = "com.masalik.yemen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.masalik.yemen"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "2.1.0"
    }

    if (releaseKeystore.isPresent) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystore.get())
            storePassword = releaseStorePassword.get()
            keyAlias = releaseKeyAlias.get()
            keyPassword = releaseKeyPassword.get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            ndk.abiFilters.add("arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
