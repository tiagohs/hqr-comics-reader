// Database, network and files: Room 3, Ktor and settings (ADR-004, ADR-005).
plugins {
    id("hqr.kmp-library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            api(libs.room3.runtime)
            api(libs.ktor.client.core)
            api(libs.multiplatform.settings)
            api(libs.okio)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.okhttp)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqlite.web)
            implementation(libs.ktor.client.js)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.okio.fakefilesystem)
        }
    }
}

dependencies {
    listOf("kspAndroid", "kspJvm", "kspWasmJs").forEach { add(it, libs.room3.compiler) }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

kover {
    reports {
        verify {
            rule { minBound(60) }
        }
    }
}
