package com.tiagohs.hqr.data.backup

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual fun gzip(bytes: ByteArray): ByteArray =
    ByteArrayOutputStream()
        .also { out ->
            GZIPOutputStream(out).use {
                it.write(bytes)
            }
        }.toByteArray()

actual fun gunzip(bytes: ByteArray): ByteArray = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
