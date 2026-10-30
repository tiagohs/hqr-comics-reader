// The zip format and the image signatures are fixed byte values, named where they are used.
@file:Suppress("MagicNumber")

package com.tiagohs.hqr.data.download

import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/**
 * The `.cbz` of a downloaded chapter (RF-67): a zip with the pages "stored" (images are already compressed), written
 * and read here without a zip library, so the same code runs wherever the downloads do.
 */
internal object Cbz {
    /** One page inside the file: its name and where its bytes are. */
    data class Entry(
        val name: String,
        val offset: Long,
        val size: Long,
    )

    /** Writes [count] pages to [target], read one at a time by [read] and named by [name] from their bytes. */
    fun write(
        fs: FileSystem,
        target: Path,
        count: Int,
        read: (Int) -> ByteArray,
        name: (Int, ByteArray) -> String,
    ) {
        val central = Buffer()
        var offset = 0L
        fs.sink(target).buffer().use { out ->
            repeat(count) { i ->
                val bytes = read(i)
                val crc = Crc32.of(bytes)
                val nameBytes = name(i, bytes).encodeToByteArray()
                out.header(LOCAL, crc, bytes.size, nameBytes)
                out.write(nameBytes)
                out.write(bytes)
                central.writeIntLe(CENTRAL)
                central.writeShortLe(VERSION)
                central.header(null, crc, bytes.size, nameBytes)
                // No comment, disk 0, no attributes, then where the local header is.
                central
                    .writeShortLe(0)
                    .writeShortLe(0)
                    .writeShortLe(0)
                    .writeIntLe(0)
                central.writeIntLe(offset.toInt())
                central.write(nameBytes)
                offset += LOCAL_HEADER + nameBytes.size + bytes.size
            }
            val centralSize = central.size
            out.writeAll(central)
            out.writeIntLe(END).writeShortLe(0).writeShortLe(0)
            out.writeShortLe(count).writeShortLe(count)
            out.writeIntLe(centralSize.toInt()).writeIntLe(offset.toInt()).writeShortLe(0)
        }
    }

    private fun BufferedSink.header(
        signature: Int?,
        crc: Int,
        size: Int,
        name: ByteArray,
    ) {
        signature?.let { writeIntLe(it) }
        // Version needed, UTF-8 names, stored, 1980-01-01 00:00.
        writeShortLe(VERSION)
            .writeShortLe(UTF8)
            .writeShortLe(0)
            .writeShortLe(0)
            .writeShortLe(DATE)
        writeIntLe(crc).writeIntLe(size).writeIntLe(size)
        writeShortLe(name.size).writeShortLe(0)
    }

    /** The pages of a `.cbz` HQR wrote, in order. */
    fun entries(
        fs: FileSystem,
        file: Path,
    ): List<Entry> =
        fs.openReadOnly(file).use { handle ->
            val size = handle.size()
            val end = Buffer().also { handle.read(size - END_SIZE, it, END_SIZE.toLong()) }
            check(end.readIntLe() == END) { "Not a cbz written by HQR" }
            end.skip(SKIP_TO_COUNT)
            val count = end.readShortLe().toInt() and MASK
            val centralSize = end.readIntLe().toLong()
            val centralOffset = end.readIntLe().toLong()
            val central = Buffer().also { handle.read(centralOffset, it, centralSize) }
            List(count) {
                check(central.readIntLe() == CENTRAL) { "Broken cbz" }
                central.skip(SKIP_TO_SIZE)
                val length = central.readIntLe().toLong() and 0xFFFFFFFFL
                val nameSize = central.readShortLe().toInt() and MASK
                central.skip(SKIP_TO_OFFSET)
                val local = central.readIntLe().toLong() and 0xFFFFFFFFL
                val name = central.readUtf8(nameSize.toLong())
                Entry(name, local + LOCAL_HEADER + nameSize, length)
            }
        }

    fun read(
        fs: FileSystem,
        file: Path,
        entry: Entry,
    ): ByteArray =
        fs.openReadOnly(file).use { handle ->
            Buffer().also { handle.read(entry.offset, it, entry.size) }.readByteArray()
        }

    private const val LOCAL = 0x04034b50
    private const val CENTRAL = 0x02014b50
    private const val END = 0x06054b50
    private const val VERSION = 20
    private const val UTF8 = 0x0800
    private const val DATE = 0x21
    private const val LOCAL_HEADER = 30
    private const val END_SIZE = 22
    private const val MASK = 0xFFFF
    private const val SKIP_TO_COUNT = 6L
    private const val SKIP_TO_SIZE = 20L
    private const val SKIP_TO_OFFSET = 12L
}

/** CRC-32 (IEEE), as the zip format asks. */
internal object Crc32 {
    private val table =
        IntArray(256) { n ->
            var c = n
            repeat(8) { c = if (c and 1 != 0) POLY xor (c ushr 1) else c ushr 1 }
            c
        }

    fun of(bytes: ByteArray): Int {
        var crc = -1
        for (b in bytes) crc = table[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        return crc.inv()
    }

    private const val POLY = 0xEDB88320.toInt()
}

/** The file extension other readers expect for an image, from its first bytes. */
internal fun imageExtension(bytes: ByteArray): String =
    when {
        bytes.startsWith(0xFF, 0xD8) -> "jpg"
        bytes.startsWith(0x89, 0x50, 0x4E, 0x47) -> "png"
        bytes.startsWith(0x47, 0x49, 0x46) -> "gif"
        bytes.size > 11 && bytes.decodeAscii(8, 12) == "WEBP" -> "webp"
        bytes.size > 11 && bytes.decodeAscii(4, 8) == "ftyp" -> "avif"
        else -> "jpg"
    }

private fun ByteArray.startsWith(vararg magic: Int) = size >= magic.size && magic.indices.all { this[it] == magic[it].toByte() }

private fun ByteArray.decodeAscii(
    from: Int,
    to: Int,
) = copyOfRange(from, to).decodeToString()
