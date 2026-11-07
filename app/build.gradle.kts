plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val startupOptimizations = providers.gradleProperty("startupOptimizations").orElse("true").map(String::toBoolean)
val optimizeBuild = providers.gradleProperty("optimizeBuild").orElse("true").map(String::toBoolean)
val appProfiles = providers.gradleProperty("appProfiles").orElse("true").map(String::toBoolean)
// Apply both profile techniques by default; keep the switch for controlled E/F measurements.
// A packaged Startup Profile is a build optimization, not a guaranteed timing improvement.
val startupProfile = providers.gradleProperty("startupProfile").orElse("true").map(String::toBoolean)

android {
    namespace = "com.example.jetpackcompose"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.jetpackcompose"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "STARTUP_OPTIMIZATIONS", startupOptimizations.get().toString())
    }

    buildTypes {
        release {
            isMinifyEnabled = optimizeBuild.get()
            isShrinkResources = optimizeBuild.get()
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("benchmark") {
            initWith(getByName("release"))
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".benchmark"
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("main").baselineProfiles.setSrcDirs(
        if (appProfiles.get()) buildList {
            add("src/main/profiles/baseline")
            if (startupProfile.get()) add("src/main/profiles/startup")
        } else emptyList<String>()
    )
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // DataStore
    implementation(libs.datastore.preferences)

    // Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image Loading
    implementation(libs.coil.compose)

    // Location
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
