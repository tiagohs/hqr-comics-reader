package com.tiagohs.hqr.sources.local

import java.util.zip.Inflater

internal fun inflateWithJavaZip(
    data: ByteArray,
    expectedSize: Int,
): ByteArray {
    val inflater = Inflater(true)
    try {
        inflater.setInput(data)
        val out = ByteArray(expectedSize)
        var done = 0
        while (!inflater.finished() && done < expectedSize) {
            val n = inflater.inflate(out, done, expectedSize - done)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            done += n
        }
        return if (done == expectedSize) out else out.copyOf(done)
    } finally {
        inflater.end()
    }
}
