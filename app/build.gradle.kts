import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val startIoAppId: String = (project.findProperty("STARTIO_APP_ID") as String?)?.trim().orEmpty()
val adsEnabled: Boolean = (project.findProperty("ADS_ENABLED") as String?)?.toBoolean() ?: true

// versionCode auto-increments on every build so "adb install -r" (and sideloading) works as an
// upgrade rather than a reinstall.  The counter lives in version.properties next to this file;
// it starts at 1 and goes up by 1 each time Gradle configures the project.
val versionProps = Properties().apply {
    val f = project.file("version.properties")
    if (f.exists()) f.inputStream().use { load(it) } else setProperty("BUILD_NUMBER", "1")
}
val buildNumber: Int = (versionProps.getProperty("BUILD_NUMBER", "1").toIntOrNull() ?: 1)
    .also { current ->
        versionProps.setProperty("BUILD_NUMBER", (current + 1).toString())
        project.file("version.properties").outputStream().use { versionProps.store(it, null) }
    }

// Optional release signing, read from the environment / keystore.properties so that NO secret is committed.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.mimo.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mimo.companion" // fixed: never change after publishing
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "0.2.$buildNumber"
        buildConfigField("String", "STARTIO_APP_ID", "\"$startIoAppId\"")
        buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Start.io (documented: https://support.start.io/hc/en-us/articles/360014774799 - Maven Central, SDK 5.3.1)
    implementation("com.startapp:inapp-sdk:5.3.1")

    testImplementation("junit:junit:4.13.2")
}
