plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.m4isper.remote"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Non-secret: keep the default in gradle.properties (committed),
        // override per environment with -PapodBaseUrl=...
        buildConfigField(
            "String",
            "BASE_URL",
            "\"${findProperty("apodBaseUrl") ?: "https://science.nasa.gov/"}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.bundles.retrofit)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}