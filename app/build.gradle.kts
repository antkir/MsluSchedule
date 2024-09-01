plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "by.ntnk.msluschedule"

    val versionMajor = 1
    val versionMinor = 90
    val versionPatch = 0
    val versionBuild = 0

    compileSdk = 34
    buildToolsVersion = "34.0.0"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        disable += setOf("UseCompoundDrawables", "CheckResult")
    }
    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
        getByName("debug") {
            java.srcDir("src/debug/kotlin")
        }
        getByName("release") {
            java.srcDir("src/release/kotlin")
        }
        getByName("test") {
            java.srcDir("src/test/kotlin")
        }
        getByName("androidTest") {
            java.srcDir("src/androidTest/kotlin")
            assets.srcDir(files("$projectDir/schemas"))
        }
    }
    packaging {
        resources {
            excludes += listOf("META-INF/CHANGES", "META-INF/README.md", "org/apache/**")
        }
    }
    defaultConfig {
        applicationId = "by.ntnk.msluschedule"
        minSdk = 21
        targetSdk = 34
        versionCode = versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "be", "ru")
    }
    buildTypes {
        debug {
            multiDexEnabled = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            multiDexEnabled = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX libraries
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.window)
    ksp(libs.androidx.room.compiler)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Jsoup
    implementation(libs.jsoup)
    // Material Components
    implementation(libs.material)
    // Moshi
    ksp(libs.moshi.codegen)
    // Okhttp
    implementation(libs.okhttp.logging)
    // Poi
    implementation(libs.poi)
    // Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.adapter.rxjava3)
    implementation(libs.retrofit.converter.moshi)
    // RxJava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxkotlin)
    // Spotlight
    implementation(libs.spotlight)
    // Timber
    implementation(libs.timber)
    // Time
    implementation(libs.threetenbp)
    // LeakCanary
    debugImplementation(libs.leakcanary)
    // Testing
    testImplementation(libs.mockito.core)
    testImplementation(libs.junit)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
