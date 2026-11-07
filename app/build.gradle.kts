plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) // Apply Google Services plugin
}

val storeListingCapture = providers.gradleProperty("storeListingCapture")
    .map { it.toBoolean() }
    .getOrElse(false)

if (storeListingCapture) layout.buildDirectory.set(layout.projectDirectory.dir("build/storeCapture"))

android {
    namespace = "com.FreeRave.shredzilla"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.FreeRave.shredzilla"
        minSdk = 29
        targetSdk = 37
        versionCode = 9
        versionName = "1.5.1"

        testInstrumentationRunner = if (storeListingCapture) {
            "com.FreeRave.shredzilla.review.StoreCaptureRunner"
        } else {
            "androidx.test.runner.AndroidJUnitRunner"
        }
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

    if (storeListingCapture) {
        sourceSets.getByName("debug").manifest.srcFile("src/storeCapture/AndroidManifest.xml")
    }

    buildTypes {
        debug {
            if (storeListingCapture) {
                applicationIdSuffix = ".storecapture"
                resValue("string", "default_web_client_id", "store-capture.invalid")
            }
        }
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
        resValues = storeListingCapture
        buildConfig = true
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
    implementation(libs.reorderable)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.process) // Added for ProcessLifecycleOwner
    implementation(libs.firebase.auth.ktx)      // Firebase Authentication
    implementation(libs.firebase.firestore.ktx) // Firebase Firestore
    implementation(libs.firebase.analytics.ktx) // Firebase Analytics
    implementation(libs.firebase.fcm)           // Firebase Cloud Messaging
    implementation(libs.firebase.config)        // Remote Config: minimum supported version, update message
    implementation(libs.play.app.update)        // Google Play in-app updates
    implementation(libs.play.services.auth)              // Google Play Services transport for Credential Manager
    implementation(libs.androidx.credentials)             // Credential Manager core
    implementation(libs.androidx.credentials.play.services) // Credential Manager + Play Services bridge
    implementation(libs.googleid)                         // GetGoogleIdOption
    implementation(libs.play.services.ads)      // Google AdMob
    // AdMob pulls in WorkManager 2.7 with Room 2.2, whose database is created by reflection and
    // stripped by R8 (release crash on launch); a current WorkManager brings Room's keep rules
    implementation(libs.androidx.work.runtime)

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

// Capture fixtures have a separate package and cannot access the network.
// Release builds keep their normal Firebase configuration and application id.
tasks.configureEach {
    if (storeListingCapture && name == "processDebugGoogleServices") enabled = false
}
