// Weeb Central (RF-103): English manga read from the site's pages (Ksoup), one request every 2 seconds.
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
