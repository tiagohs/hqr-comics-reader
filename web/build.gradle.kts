import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// The web app (ADR-022): the same shared screens in the browser (wasmJs), a static site for GitHub Pages (C-12).
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.kotlin.serialization)
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
            implementation(project(":sources:mangadex"))
            implementation(project(":sources:hqnow"))
            implementation(project(":sources:opds"))
            implementation(project(":sources:archive"))
            implementation(project(":sources:namicomi"))
            implementation(project(":sources:mangaflix"))
            implementation(project(":sources:yomu"))
            implementation(project(":sources:weebcentral"))
            implementation(project(":sources:xoxo"))
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.cmp.runtime)
            implementation(libs.cmp.ui)
            implementation(libs.cmp.foundation)
            implementation(libs.sqlite.web)
            implementation(npm("@sqlite.org/sqlite-wasm", "3.50.4-build1"))
            implementation(npm("pdfjs-dist", "4.10.38"))
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

// PDF pages in the browser (ADR-011): pdf.js and its worker go next to the app too, loaded only when a PDF opens.
val pdfJs =
    tasks.register<Copy>("copyPdfJs") {
        dependsOn(rootProject.tasks.named("kotlinWasmNpmInstall"))
        from(rootProject.layout.buildDirectory.dir("wasm/node_modules/pdfjs-dist/build")) {
            include("pdf.min.mjs", "pdf.worker.min.mjs")
        }
        into(layout.buildDirectory.dir("generated/pdfjs"))
    }
kotlin.sourceSets.named("wasmJsMain") {
    resources.srcDir(sqliteWasm.map { it.destinationDir })
    resources.srcDir(pdfJs.map { it.destinationDir })
}
