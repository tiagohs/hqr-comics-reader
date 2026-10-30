// Models, rules and contracts (architecture §3): no network, database or platform here.
plugins {
    id("hqr.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
    }
}

kover {
    reports {
        verify {
            rule { minBound(80) }
        }
    }
}
