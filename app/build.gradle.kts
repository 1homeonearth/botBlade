import org.gradle.api.Project

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun Project.releaseValue(name: String): String? =
    providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull

val releaseStoreFile = releaseValue("ROYAL_SCEPTER_RELEASE_STORE_FILE")
val releaseStorePassword = releaseValue("ROYAL_SCEPTER_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseValue("ROYAL_SCEPTER_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseValue("ROYAL_SCEPTER_RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.princess.botblade"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.princess.botblade"
        minSdk = 26
        targetSdk = 35

        // Release policy: bump VERSION_CODE for every Play upload; use SemVer for VERSION_NAME.
        // Override without editing source: -PVERSION_CODE=2 -PVERSION_NAME=0.1.1.
        versionCode = providers.gradleProperty("VERSION_CODE").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("VERSION_NAME").orNull ?: "0.1.0"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("localDev") {
            dimension = "environment"
            applicationIdSuffix = ".localdev"
            versionNameSuffix = "-local-dev"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
            buildConfigField("boolean", "USE_REMOTE_BACKEND", "false")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${providers.gradleProperty("PROD_API_BASE_URL").orNull ?: "http://127.0.0.1:7432"}\"",
            )
            buildConfigField("boolean", "USE_REMOTE_BACKEND", "false")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            assets.srcDir("../backend")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.github.LiquidPlayer:LiquidCore:0.6.2")
}

tasks.register("printResolvedVersionMetadata") {
    group = "help"
    description = "Print resolved VERSION_NAME and VERSION_CODE for CI metadata extraction."
    doLast {
        val resolvedVersionName = android.defaultConfig.versionName
            ?: throw GradleException("Resolved versionName is null.")
        val resolvedVersionCode = android.defaultConfig.versionCode
            ?: throw GradleException("Resolved versionCode is null.")
        if (resolvedVersionCode <= 0) {
            throw GradleException("Resolved versionCode must be > 0 (was $resolvedVersionCode).")
        }
        println("VERSION_NAME=$resolvedVersionName")
        println("VERSION_CODE=$resolvedVersionCode")
    }
}

tasks.register("processDebugMainManifest") {
    group = "verification"
    description = "Process main manifest for all debug variants."
    dependsOn(
        ":app:processLocalDevDebugMainManifest",
        ":app:processProdDebugMainManifest",
    )
}

tasks.register("processReleaseMainManifest") {
    group = "verification"
    description = "Process main manifest for all release variants."
    dependsOn(
        ":app:processLocalDevReleaseMainManifest",
        ":app:processProdReleaseMainManifest",
    )
}
