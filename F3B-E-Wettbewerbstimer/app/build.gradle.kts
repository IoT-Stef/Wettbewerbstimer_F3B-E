plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.wintgen.sprachcountdown"
    compileSdk = 35

    defaultConfig {
        // Eigene Kennung: wird als neue App neben einer evtl. alten Version installiert
        applicationId = "de.wintgen.f3betimer"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.0"
    }

    // Fester Schlüssel, damit spätere Versionen die App einfach aktualisieren können
    signingConfigs {
        create("fest") {
            storeFile = file("f3b-timer.keystore")
            storePassword = "f3btimer"
            keyAlias = "f3btimer"
            keyPassword = "f3btimer"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fest")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fest")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
