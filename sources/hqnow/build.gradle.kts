// HQ Now (RF-12): a GraphQL API with Brazilian comics in Portuguese.
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
