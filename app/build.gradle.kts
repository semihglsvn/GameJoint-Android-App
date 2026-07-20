plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.openapi.generator")
}

android {
    namespace = "com.gamejoint.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.gamejoint.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- FIXED: Read from local.properties without needing Java imports ---
        var mobileSecret = ""
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.readLines().forEach { line ->
                if (line.startsWith("MOBILE_API_SECRET=")) {
                    mobileSecret = line.substringAfter("=").trim()
                }
            }
        }

        // Inject it into the app via BuildConfig
        buildConfigField("String", "MOBILE_API_SECRET", "\"$mobileSecret\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Networking & UI
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Jetpack DataStore (For saving our JWT Session securely)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/swagger.json")

    // --- FIXED: Deprecated buildDir replaced with layout.buildDirectory ---
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)

    apiPackage.set("com.gamejoint.app.data.remote")
    modelPackage.set("com.gamejoint.app.data.model")

    configOptions.set(mapOf(
        "library" to "jvm-retrofit2",
        "serializationLibrary" to "gson",
        "useCoroutines" to "true"
    ))
}