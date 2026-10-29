// Screens, components and theme in Compose Multiplatform, for Android and the web (ADR-022). Values from tokens.json.
plugins {
    id("hqr.compose-library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared:presentation"))
            implementation(project(":shared:data"))
            implementation(project(":sources:local"))
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.foundation)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.material3.nav.suite)
            implementation(libs.cmp.adaptive)
            implementation(libs.cmp.components.resources)
            implementation(libs.cmp.ui.tooling.preview)
            implementation(libs.jb.navigation.compose)
            implementation(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.telephoto.zoomable)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        jvmTest.dependencies {
            // Screen tests and screenshots on the JVM (Compose Desktop renderer): the same shared screens.
            implementation(compose.desktop.currentOs)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(project(":shared:data"))
            implementation(libs.kotlinx.coroutines.swing)
            implementation(project(":sources:mangadex"))
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.tiagohs.hqr.ui.resources"
    generateResClass = always
}

// -PliveCheck=true runs the screen flow against the real MangaDex too (screenshots only, never committed).
tasks.withType<Test>().configureEach {
    systemProperty("liveCheck", providers.gradleProperty("liveCheck").orNull ?: "false")
}
