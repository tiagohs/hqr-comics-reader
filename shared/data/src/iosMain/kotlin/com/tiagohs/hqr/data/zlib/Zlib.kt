// zlib's window bits and chunk size are fixed values of its API, named where they are used.
@file:Suppress("MagicNumber")
@file:OptIn(ExperimentalForeignApi::class)

package com.tiagohs.hqr.data.zlib

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.zlib.Z_DEFAULT_COMPRESSION
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_FINISH
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.Z_BUF_ERROR
import platform.zlib.ZLIB_VERSION
import platform.zlib.deflate
import platform.zlib.deflateEnd
import platform.zlib.deflateInit2_
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream

/** The system zlib for iOS: gzip for the backup, raw DEFLATE for CBZ pages (the JVM uses java.util.zip). */
object Zlib {
    private const val GZIP = 15 + 16
    private const val RAW = -15
    private const val CHUNK = 64 * 1024

    fun gzip(data: ByteArray): ByteArray =
        memScoped {
            val stream = alloc<z_stream>()
            val init = deflateInit2_(stream.ptr, Z_DEFAULT_COMPRESSION, Z_DEFLATED, GZIP, 8, Z_DEFAULT_STRATEGY, ZLIB_VERSION, zSize())
            check(init == Z_OK) { "zlib deflate init" }
            val out = ArrayList<ByteArray>()
            val chunk = ByteArray(CHUNK)
            try {
                data.usePinnedOrEmpty { input ->
                    stream.next_in = input
                    stream.avail_in = data.size.convert()
                    chunk.usePinned { buffer ->
                        do {
                            stream.next_out = buffer.addressOf(0).reinterpret()
                            stream.avail_out = CHUNK.convert()
                            val result = deflate(stream.ptr, Z_FINISH)
                            check(result == Z_OK || result == Z_STREAM_END || result == Z_BUF_ERROR) { "zlib deflate $result" }
                            out += chunk.copyOf(CHUNK - stream.avail_out.toInt())
                        } while (result != Z_STREAM_END)
                    }
                }
            } finally {
                deflateEnd(stream.ptr)
            }
            out.join()
        }

    /** Null when [data] is not a whole gzip stream. */
    fun gunzip(data: ByteArray): ByteArray? = inflate(data, GZIP, limit = Int.MAX_VALUE)

    /** Raw DEFLATE (ZIP method 8), never past [expectedSize] bytes (RNF-26: no compression bombs). */
    fun inflateRaw(
        data: ByteArray,
        expectedSize: Int,
    ): ByteArray? = inflate(data, RAW, limit = expectedSize, partialOk = true)

    private fun inflate(
        data: ByteArray,
        windowBits: Int,
        limit: Int,
        partialOk: Boolean = false,
    ): ByteArray? =
        memScoped {
            val stream = alloc<z_stream>()
            check(inflateInit2_(stream.ptr, windowBits, ZLIB_VERSION, zSize()) == Z_OK) { "zlib inflate init" }
            val out = ArrayList<ByteArray>()
            var total = 0
            val chunk = ByteArray(CHUNK)
            var ended = false
            try {
                data.usePinnedOrEmpty { input ->
                    stream.next_in = input
                    stream.avail_in = data.size.convert()
                    chunk.usePinned { buffer ->
                        var going = true
                        while (going && total < limit) {
                            stream.next_out = buffer.addressOf(0).reinterpret()
                            stream.avail_out = CHUNK.convert()
                            val result = inflate(stream.ptr, Z_NO_FLUSH)
                            val produced = CHUNK - stream.avail_out.toInt()
                            if (produced > 0) {
                                out += chunk.copyOf(minOf(produced, limit - total))
                                total += produced
                            }
                            ended = result == Z_STREAM_END
                            going = !ended && result == Z_OK && (produced > 0 || stream.avail_in.toInt() > 0)
                        }
                    }
                }
            } finally {
                inflateEnd(stream.ptr)
            }
            if (ended || partialOk) out.join() else null
        }

    private inline fun <R> ByteArray.usePinnedOrEmpty(block: (kotlinx.cinterop.CPointer<UByteVar>?) -> R): R =
        if (isEmpty()) block(null) else usePinned { block(it.addressOf(0).reinterpret()) }

    private fun zSize(): Int = sizeOf<z_stream>().toInt()

    private fun List<ByteArray>.join(): ByteArray {
        val all = ByteArray(sumOf { it.size })
        var at = 0
        for (part in this) {
            part.copyInto(all, at)
            at += part.size
        }
        return all
    }
}
