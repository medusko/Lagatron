import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

val requiredSigningProps = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
val configuredStoreFile = keystoreProperties.getProperty("storeFile")?.trim().orEmpty()
val resolvedStoreFile = if (configuredStoreFile.isNotEmpty()) {
    val moduleRelative = file(configuredStoreFile)
    if (moduleRelative.exists()) moduleRelative else rootProject.file(configuredStoreFile)
} else {
    null
}
val hasValidReleaseSigning = keystorePropertiesFile.exists() &&
    requiredSigningProps.all { !keystoreProperties.getProperty(it).isNullOrBlank() } &&
    resolvedStoreFile != null &&
    resolvedStoreFile.exists()

android {
    namespace = "com.medusko.lagatron"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.medusko.lagatron"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.1-RC2-Beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("Boolean", "SHOW_ADS", "true")
    }

    signingConfigs {
        create("release") {
            if (hasValidReleaseSigning) {
                storeFile = resolvedStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (hasValidReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    debugImplementation(libs.androidx.ui.tooling)
}

gradle.taskGraph.whenReady {
    val releaseBuildTasks = setOf(
        "assembleRelease",
        "bundleRelease",
        "packageRelease",
        "packageReleaseBundle",
        "signReleaseBundle",
        "validateSigningRelease"
    )
    val runningReleasePackaging = allTasks.any { task ->
        task.project == project && task.name in releaseBuildTasks
    }
    if (runningReleasePackaging && !hasValidReleaseSigning) {
        throw GradleException(
            "Release signing is not configured: missing keystore.properties, required signing properties, " +
                "or keystore file. Refusing to fall back to debug signing."
        )
    }
}
