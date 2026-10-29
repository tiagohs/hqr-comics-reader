package com.tiagohs.hqr.sources.local

import com.tiagohs.hqr.domain.source.SourceException

// The browser version (DecompressionStream "deflate-raw") arrives with the file import of the web app (FEAT-052).
actual suspend fun inflateRaw(
    data: ByteArray,
    expectedSize: Int,
): ByteArray = throw SourceException("CBZ compactado ainda não abre no navegador", SourceException.Kind.UNAVAILABLE)
