// MangaDex (RF-04, RF-08 to RF-10, ADR-005, ADR-008): API v5, at-home image servers, rate limits and reports.
plugins {
    id("hqr.kmp-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:data"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
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

tasks.withType<Test>().configureEach {
    systemProperty("liveCheck", providers.gradleProperty("liveCheck").orNull ?: "false")
    testLogging.showStandardStreams = providers.gradleProperty("liveCheck").orNull == "true"
}
