package com.tiagohs.hqr.sources.local

actual suspend fun inflateRaw(
    data: ByteArray,
    expectedSize: Int,
): ByteArray = inflateWithJavaZip(data, expectedSize)
