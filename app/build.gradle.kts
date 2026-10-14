plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.m4isper.planets"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.m4isper.planets"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(project(":data:di"))
    implementation(project(":domain"))

    // DI
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)

    // Image loading
    implementation(libs.coil.compose)
    // Coil 3 pulls in only `coil-core`; without an explicit fetcher images fail to
    // load entirely, and silently — no exception, just empty placeholders.
    implementation(libs.coil.network.okhttp)

    // Navigation
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    // Scopes hiltViewModel() to the back stack entry inside NavDisplay.
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Serializable NavKeys: the back stack survives process death.
    implementation(libs.kotlinx.serialization.core)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.jsoup)
    testImplementation(libs.junit)
    testImplementation(libs.jsoup)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // The Compose BOM has to be repeated here: `androidTestImplementation` does not
    // extend `implementation`, where the BOM is declared above, so without this copy
    // the versionless `ui-test-junit4` below reaches Gradle with no version at all.
    // It only surfaces through `check`/`build` — `assemble*` and the unit tests never
    // resolve `debugAndroidTestRuntimeClasspath`.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}