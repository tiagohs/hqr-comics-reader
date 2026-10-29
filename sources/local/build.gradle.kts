// Local files (RF-13 to RF-16): the folder the user chose, read only. Platform file access enters through FileTree.
plugins {
    id("hqr.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:domain"))
            implementation(libs.okio)
        }
        jvmTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
