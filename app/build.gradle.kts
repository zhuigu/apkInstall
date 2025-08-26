import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        targetSdk = 36
        versionCode = 5
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Ktor 依赖
    implementation(libs.bundles.ktor.server)

    // AndroidX 和其他库
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.ui.tooling.preview.android)
}