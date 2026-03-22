plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) // Apply Google Services plugin
}

android {
    namespace = "com.FreeRave.shredzilla"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.FreeRave.shredzilla"
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("SHREDZILLA_RELEASE_STORE_FILE") as? String
            val storePwd = project.findProperty("SHREDZILLA_RELEASE_STORE_PASSWORD") as? String
            val aliasName = project.findProperty("SHREDZILLA_RELEASE_KEY_ALIAS") as? String
            val aliasPwd = project.findProperty("SHREDZILLA_RELEASE_KEY_PASSWORD") as? String

            if (storeFilePath != null && storePwd != null && aliasName != null && aliasPwd != null) {
                // Assuming the keystore file is in the 'app' directory relative to the project root
                storeFile = rootProject.file("app/$storeFilePath")
                storePassword = storePwd
                keyAlias = aliasName
                keyPassword = aliasPwd
            } else {
                System.err.println("WARNING: Keystore properties not fully defined in gradle.properties. Release build may be unsigned or fail.")
                // Attempting to set dummy values to avoid immediate build failure due to missing properties,
                // but the build will likely fail at packaging if these are not valid.
                // This is primarily to let the build proceed further to see other potential errors.
                // A truly unsigned build would typically omit the signingConfig from the buildType.
                storeFile = rootProject.file("app/dummy.keystore") // Dummy path
                storePassword = "dummyPassword"
                keyAlias = "dummyAlias"
                keyPassword = "dummyPassword"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enabled for release
            isShrinkResources = true // Enabled for release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3)
    }
}

composeCompiler {
    includeComposeMappingFile.set(false)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.process) // Added for ProcessLifecycleOwner
    implementation(libs.firebase.auth.ktx)      // Firebase Authentication
    implementation(libs.firebase.firestore.ktx) // Firebase Firestore
    implementation(libs.firebase.analytics.ktx) // Firebase Analytics
    implementation(libs.firebase.fcm)           // Firebase Cloud Messaging
    implementation(libs.play.services.auth)     // Google Sign-In
    implementation(libs.play.services.ads)      // Google AdMob
    implementation(libs.app.updater)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
