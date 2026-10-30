// XOXO Comics (RF-104): Western comics read from the site's pages (Ksoup), one request per second.
plugins {
    id("hqr.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:data"))
            implementation(libs.ksoup)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}
