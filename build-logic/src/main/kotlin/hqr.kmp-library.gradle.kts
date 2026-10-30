// Template for the core modules (shared/domain, shared/data, shared/presentation, sources/*).
// Targets: Android, JVM (fast tests, real SQLite) and wasmJs (the web app, ADR-022). iOS joins later (C-09):
// set the Gradle property hqr.ios=true to add the iPhone targets.
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlinx.kover")
    id("org.jlleitschuh.gradle.ktlint")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun version(name: String) = catalog.findVersion(name).get().requiredVersion
fun lib(name: String) = catalog.findLibrary(name).get()

val iosEnabled = providers.gradleProperty("hqr.ios").orNull == "true"

kotlin {
    android {
        namespace = "com.tiagohs.hqr." + project.path.removePrefix(":").replace(":", ".").replace("-", "")
        compileSdk = version("compileSdk").toInt()
        minSdk = version("minSdk").toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest {}
    }
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    if (iosEnabled) {
        iosArm64()
        iosSimulatorArm64()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(lib("kotest-assertions-core"))
            implementation(lib("kotlinx-coroutines-test"))
        }
    }
}

// The browser tests need a headless browser; the JVM and Android host runs cover the same commonTest suite.
tasks.matching { it.name == "wasmJsBrowserTest" }.configureEach { enabled = false }

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

// Coverage counts our code only: Room, serialization and resources generate the rest.
configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
    reports {
        filters {
            excludes {
                classes("*_Impl", "*_Impl\$*", "*\$\$serializer", "*.HqrDatabaseConstructor", "*.generated.resources.*", "*ComposableSingletons*")
            }
        }
    }
}

// Compose UI tests run on the JVM only; the browser build has no test executable.
tasks.matching { it.name == "checkComposeUiTestConfigurationForWasmJs" }.configureEach { enabled = false }
