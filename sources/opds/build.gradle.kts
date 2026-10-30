// OPDS servers (RF-17 to RF-19, RF-95): Komga, Kavita and other catalogs the user runs; pages by OPDS-PSE or the file.
plugins {
    id("hqr.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(project(":shared:data"))
            implementation(project(":sources:local"))
        }
        jvmTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.okio.fakefilesystem)
        }
    }
}

kover {
    reports {
        verify {
            rule { minBound(75) }
        }
    }
}
