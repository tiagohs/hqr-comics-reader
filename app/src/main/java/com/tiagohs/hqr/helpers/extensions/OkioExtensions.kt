package com.tiagohs.hqr.helpers.extensions

import okio.BufferedSource
import okio.Okio
import java.io.File
import java.io.OutputStream


fun BufferedSource.saveTo(file: File) {
    try {
        file.parentFile.mkdirs()

        saveTo(file.outputStream())
    } catch (e: Exception) {
        close()
        file.delete()
        throw e
    }
}

fun BufferedSource.saveTo(stream: OutputStream) {
    use { input ->
        Okio.buffer(Okio.sink(stream)).use {
            it.writeAll(input)
            it.flush()
        }
    }
}
