import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The web app (ADR-022): the same shared screens in the browser (wasmJs), a static site for GitHub Pages (C-12).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("hqr")
        browser {
            commonWebpackConfig { outputFileName = "hqr.js" }
        }
        binaries.executable()
    }
    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared:ui"))
            implementation(project(":shared:data"))
            implementation(project(":sources:local"))
            implementation(libs.koin.core)
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.foundation)
            implementation(libs.sqlite.web)
            implementation(npm("@sqlite.org/sqlite-wasm", "3.50.4-build1"))
        }
    }
}

// SQLite in the browser (ADR-004): the worker and the sqlite3 module go next to the app, served as static files.
val sqliteWasm =
    tasks.register<Copy>("copySqliteWasm") {
        dependsOn(rootProject.tasks.named("kotlinWasmNpmInstall"))
        from(rootProject.layout.buildDirectory.dir("wasm/node_modules/@sqlite.org/sqlite-wasm/sqlite-wasm/jswasm")) {
            include("sqlite3.mjs", "sqlite3.wasm", "sqlite3-opfs-async-proxy.js")
        }
        into(layout.buildDirectory.dir("generated/sqlite"))
    }
kotlin.sourceSets.named("wasmJsMain") { resources.srcDir(sqliteWasm.map { it.destinationDir }) }
