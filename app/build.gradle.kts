plugins {
    alias(libs.plugins.android.application)
    // Usunięte pluginy Kotlina
}

android {
    namespace = "com.example.adaptivesoundscapemooddiary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.adaptivesoundscapemooddiary"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas".toString()
                )
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Room dla Javy
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler) // Dla Javy używamy annotationProcessor

    // ImagePicker
    implementation(libs.imagepicker)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}