import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}
val appProperties = Properties().apply { load(FileInputStream(rootProject.file("app.properties"))) }

android {
    signingConfigs {
        create("release") {
            storeFile = appProperties["storeFile"]?.let { file(it) }
            storePassword = appProperties["storePassword"]?.toString()
            keyAlias = appProperties["keyAlias"]?.toString()
            keyPassword = appProperties["keyPassword"]?.toString()
            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    namespace = "com.tv.upload"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tv.upload"
        minSdk = 34
        targetSdk = 34
        versionCode = 8
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.add("armeabi-v7a")
        }
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/services/reactor.blockhound.integration.BlockHoundIntegration"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    java {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
    kotlin {
        compilerOptions {
            optIn.add("21")
        }
        composeOptions {
            kotlinCompilerExtensionVersion = "2.2.20"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Ktor 依赖
    implementation(libs.bundles.ktor.server)

    // AndroidX 和其他库
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)

    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.tv.foundation)
}