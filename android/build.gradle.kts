import java.util.Properties

// The Android app (architecture §3): the shared screens (shared/ui), Koin and the platform pieces.
plugins {
    id("hqr.android-app")
    alias(libs.plugins.roborazzi)
}

// Crashlytics only when the Firebase file exists (it stays out of git; ADR-014).
val hasFirebase = file("google-services.json").exists()
if (hasFirebase) {
    apply(
        plugin =
            libs.plugins.google.services
                .get()
                .pluginId,
    )
    apply(
        plugin =
            libs.plugins.firebase.crashlytics
                .get()
                .pluginId,
    )
}

val localProperties =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { load(it) }
    }

fun secret(name: String): String? = providers.environmentVariable(name).orNull ?: localProperties.getProperty(name)

val keystorePassword = secret("HQR_KEYSTORE_PASSWORD")

android {
    namespace = "com.tiagohs.hqr.android"
    defaultConfig {
        // C-11: new id and key; the 0.0.5 key is never used.
        applicationId = "com.tiagohs.hqr.reader"
        versionCode = providers.gradleProperty("hqr.versionCode").orNull?.toInt() ?: 100
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    androidResources {
        generateLocaleConfig = true
    }
    buildFeatures {
        buildConfig = true
    }
    signingConfigs {
        create("release") {
            storeFile = secret("HQR_KEYSTORE_PATH")?.let { rootProject.file(it) }
            storePassword = keystorePassword
            keyAlias = secret("HQR_KEY_ALIAS") ?: "hqr"
            keyPassword = secret("HQR_KEY_PASSWORD") ?: keystorePassword
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without the key (C-11, still to be created) the release is signed with the debug key: test builds only.
            signingConfig =
                if (keystorePassword != null &&
                    secret("HQR_KEYSTORE_PATH") != null
                ) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }
    sourceSets["main"].kotlin.directories += if (hasFirebase) "src/firebase/kotlin" else "src/noFirebase/kotlin"
    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/INDEX.LIST", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
    // RNF-08: one APK per ABI plus the universal one.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(project(":shared:data"))
    implementation(project(":sources:local"))
    implementation(project(":sources:mangadex"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.profileinstaller)
    if (hasFirebase) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.crashlytics)
    }
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.test.manifest)
}

roborazzi {
    outputDir.set(file("src/test/snapshots"))
}

// Robolectric's Android jar comes through Gradle (same repositories), so tests run offline.
val robolectricAndroid by configurations.creating
dependencies {
    robolectricAndroid(libs.robolectric.android.all)
}
val robolectricJars =
    tasks.register<Sync>("robolectricJars") {
        from(robolectricAndroid)
        into(layout.buildDirectory.dir("robolectric-jars"))
    }
tasks.withType<Test>().configureEach {
    dependsOn(robolectricJars)
    systemProperty("robolectric.offline", "true")
    systemProperty(
        "robolectric.dependency.dir",
        layout.buildDirectory
            .dir("robolectric-jars")
            .get()
            .asFile.absolutePath,
    )
    maxHeapSize = "2g"
}
