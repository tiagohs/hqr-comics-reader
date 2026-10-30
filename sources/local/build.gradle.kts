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
        // CBR and CB7 (ADR-011): JVM libraries, so Android (and the JVM tests) only; the browser refuses them (RF-110).
        val jvmShared by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.junrar)
                implementation(libs.commons.compress)
                implementation(libs.xz)
            }
        }
        jvmMain.get().dependsOn(jvmShared)
        androidMain.get().dependsOn(jvmShared)
        jvmTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
