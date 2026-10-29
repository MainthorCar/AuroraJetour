/*
 * SPDX-FileCopyrightText: 2021-2025 Rahul Kumar Patel <whyorean@gmail.com>
 * SPDX-FileCopyrightText: 2022-2025 The Calyx Institute
 * SPDX-FileCopyrightText: 2023 grrfe <grrfe@420blaze.it>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.rikka.tools.refine.plugin)
    alias(libs.plugins.hilt.android.plugin)
}

// Datos del commit para la versión. GitHub Actions los pasa con
// -PcommitHash=... y -PcommitTimestamp=...; en una PC se usan valores fijos,
// así el proyecto compila aunque Git no esté instalado o no esté en el PATH.
val lastCommitHash = providers.gradleProperty("commitHash").orElse("local")
val lastCommitTimestamp = providers.gradleProperty("commitTimestamp").orElse("0")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property"
        )
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
            "coil3.annotation.ExperimentalCoilApi",
            "kotlin.uuid.ExperimentalUuidApi"
        )
    }
}

configure<ApplicationExtension> {
    namespace = "com.aurora.store"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.aurora.store"
        minSdk {
            version = release(23)
        }
        targetSdk {
            version = release(37)
        }

        versionCode = 76
        versionName = "4.8.4"

        testInstrumentationRunner = "com.aurora.store.HiltInstrumentationTestRunner"
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        buildConfigField("String", "EXODUS_API_KEY", "\"bbe6ebae4ad45a9cbacb17d69739799b8df2c7ae\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${lastCommitTimestamp.get()}L")

        missingDimensionStrategy("device", "vanilla")
    }

    signingConfigs {
        if (file("signing.properties").exists()) {
            create("release") {
                val properties = Properties().apply {
                    file("signing.properties").inputStream().use { load(it) }
                }

                keyAlias = properties["KEY_ALIAS"] as String
                keyPassword = properties["KEY_PASSWORD"] as String
                storeFile = file(properties["STORE_FILE"] as String)
                storePassword = properties["KEY_PASSWORD"] as String
            }
        }
        create("aosp") {
            // Generated from the AOSP test key:
            // https://android.googlesource.com/platform/build/+/refs/tags/android-11.0.0_r29/target/product/security/testkey.pk8
            keyAlias = "testkey"
            keyPassword = "testkey"
            storeFile = file("testkey.jks")
            storePassword = "testkey"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (file("signing.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        register("nightly") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-${lastCommitHash.get()}"
        }

        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("aosp")
        }
    }

    flavorDimensions += "device"

    productFlavors {
        create("vanilla") {
            dimension = "device"
            buildConfigField("Boolean", "SHOW_ANONYMOUS_LOGIN", "true")
        }

        create("huawei") {
            dimension = "device"
            versionNameSuffix = "-hw"
            buildConfigField("Boolean", "SHOW_ANONYMOUS_LOGIN", "false")
        }

        // This flavor is only for preloaded devices / users who push the app to system
        create("preload") {
            dimension = "device"
            versionNameSuffix = "-preload"
            buildConfigField("Boolean", "SHOW_ANONYMOUS_LOGIN", "true")
        }

        // Aurora Store Jetour: pantalla de la Jetour T1 (AOSP sin root, con microG).
        // Basado en el flavor "byd" de AuroraStore-BYD (kangrio).
        create("jetour") {
            isDefault = true
            dimension = "device"
            applicationId = "com.mainthorcar.aurorajetour"

            // Versión de Aurora Store Jetour. Subir este número en cada release.
            // 1-9 -> 0.0.X, 10-99 -> 0.X.X, 100-999 -> X.X.X
            val jetourVersionCode = 103
            val major = jetourVersionCode / 100
            val minor = (jetourVersionCode / 10) % 10
            val patch = jetourVersionCode % 10
            val jetourVersionName = "jetour($major.$minor.$patch)"

            versionNameSuffix = "-$jetourVersionName"
            buildConfigField("Boolean", "SHOW_ANONYMOUS_LOGIN", "true")

            // Subir este número cuando cambie el parche que se aplica a las apps
            val patchVersionCode = 4
            buildConfigField("int", "PATCH_VERSION_CODE", "$patchVersionCode")
        }
    }

    buildFeatures {
        buildConfig = true
        aidl = true
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes.add("org/bouncycastle/pqc/crypto/picnic/*.properties")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        val flavour = variant.flavorName
        if ((flavour == "huawei" || flavour == "preload") && variant.buildType == "nightly") {
            variant.enable = false
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

ktlint {
    android = true
    verbose = true
}

dependencies {
    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Markdown
    implementation("org.jetbrains:markdown:0.7.9")

    // patcher
    implementation("io.github.reandroid:ARSCLib:1.4.0")
    implementation("com.android.tools.build:apksig:8.8.0")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")

    // Google's Goodies
    implementation(libs.google.android.material)
    implementation(libs.google.protobuf.javalite)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.navigation3)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.paging.runtime)

    implementation(libs.androidx.adaptive.core)
    implementation(libs.androidx.adaptive.navigation)
    implementation(libs.androidx.adaptive.layout)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Coil
    implementation(libs.coil.kt)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // HTTP Clients
    implementation(libs.squareup.okhttp)

    // Lib-SU
    implementation(libs.github.topjohnwu.libsu)

    // GPlayApi
    implementation(libs.auroraoss.gplayapi)

    // Shizuku
    compileOnly(libs.rikka.hidden.stub)
    implementation(libs.rikka.tools.refine.runtime)
    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.lsposed.hiddenapibypass)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.androidx.espresso.core)

    // Hilt
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.androidx.compiler)
    implementation(libs.androidx.hilt.viewmodel)
    implementation(libs.hilt.android.core)
    implementation(libs.hilt.androidx.work)

    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)

    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)

    implementation(libs.process.phoenix)

    "huaweiImplementation"(libs.huawei.hms.coreservice)

    // LeakCanary
    debugImplementation(libs.squareup.leakcanary.android)
}

tasks.named("preBuild") {
    dependsOn(":extension:buildExtension")
}