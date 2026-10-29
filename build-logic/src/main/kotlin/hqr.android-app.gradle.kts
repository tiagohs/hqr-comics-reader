// Template for the Android app: Compose + Material 3, screen tests on the JVM (Robolectric).
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun version(name: String) = catalog.findVersion(name).get().requiredVersion

android {
    compileSdk = version("compileSdk").toInt()
    defaultConfig {
        minSdk = version("minSdk").toInt()
        targetSdk = version("targetSdk").toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
