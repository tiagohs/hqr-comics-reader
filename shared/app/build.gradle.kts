// The app's shared wiring (Koin): repositories, sources and ViewModels for every platform. On iOS it is the "Shared"
// framework the SwiftUI app links (architecture §3.1): ViewModels, models and the graph, without Compose.
plugins {
    id("hqr.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":shared:domain"))
            export(project(":shared:data"))
            export(project(":shared:presentation"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":shared:presentation"))
            api(project(":shared:data"))
            implementation(project(":sources:local"))
            implementation(project(":sources:opds"))
            api(libs.koin.core)
            api(libs.koin.core.viewmodel)
            implementation(libs.kotlinx.serialization.json)
        }
        // iOS only when the targets exist (-Phqr.ios=true).
        matching { it.name == "iosMain" }.configureEach {
            dependencies {
                implementation(project(":sources:mangadex"))
                implementation(project(":sources:hqnow"))
                implementation(project(":sources:archive"))
                implementation(project(":sources:namicomi"))
                implementation(project(":sources:mangaflix"))
                implementation(project(":sources:yomu"))
                implementation(project(":sources:weebcentral"))
                implementation(project(":sources:xoxo"))
                implementation(libs.ktor.client.darwin)
            }
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.multiplatform.settings.test)
        }
        jvmTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}

kover {
    reports {
        verify {
            rule { minBound(60) }
        }
    }
}
