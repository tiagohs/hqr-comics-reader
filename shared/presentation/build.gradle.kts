// ViewModels and screen state (UiState), shared by Android and the web (ADR-022) and later by the iOS app.
plugins {
    id("hqr.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            api(libs.jb.lifecycle.viewmodel)
            api(libs.jb.lifecycle.viewmodel.savedstate)
            api(libs.materialkolor.utilities)
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.turbine)
        }
        jvmTest.dependencies {
            implementation(project(":shared:data"))
            implementation(project(":sources:local"))
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
