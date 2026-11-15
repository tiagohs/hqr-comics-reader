package com.tiagohs.hqr.sources.local

import com.tiagohs.hqr.data.zlib.Zlib

actual suspend fun inflateRaw(
    data: ByteArray,
    expectedSize: Int,
): ByteArray = Zlib.inflateRaw(data, expectedSize) ?: ByteArray(0)
