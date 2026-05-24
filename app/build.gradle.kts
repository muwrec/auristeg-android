plugins {
    id("com.android.application")
}

android {
    namespace = "com.auristeg.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.auristeg.android"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
}
