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
        // jvmShared turns the default hierarchy off, so iosMain is declared here. The system zlib (CBZ pages compressed
        // with DEFLATE) lives in shared/data for iOS. Only when the targets exist (-Phqr.ios=true).
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(project(":shared:data"))
            }
        }
        matching { it.name == "iosArm64Main" || it.name == "iosSimulatorArm64Main" }.configureEach { dependsOn(iosMain) }
        jvmTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
