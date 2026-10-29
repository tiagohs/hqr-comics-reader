package com.tiagohs.hqr.sources.local

import com.tiagohs.hqr.domain.source.SourceException

/** Inflates raw DEFLATE data (ZIP method 8). JVM and Android use java.util.zip; the browser its DecompressionStream. */
expect suspend fun inflateRaw(
    data: ByteArray,
    expectedSize: Int,
): ByteArray

/**
 * Reads CBZ/ZIP files by ranges, through the central directory (RF-13). Protects against paths with "..", entries
 * bigger than [MAX_ENTRY_BYTES] and compression bombs (RNF-26).
 */
class ZipReader(
    private val tree: FileTree,
    private val path: String,
) {
    data class Entry(
        val name: String,
        val method: Int,
        val compressedSize: Long,
        val size: Long,
        val localHeaderOffset: Long,
    )

    suspend fun entries(): List<Entry> {
        val fileSize = tree.size(path)
        if (fileSize < EOCD_SIZE) throw invalid("arquivo curto demais")
        val tailLength = minOf(fileSize, (EOCD_SIZE + MAX_COMMENT).toLong()).toInt()
        val tail = tree.read(path, fileSize - tailLength, tailLength)
        val eocd = (tail.size - EOCD_SIZE downTo 0).firstOrNull { tail.u32(it) == EOCD_SIGNATURE } ?: throw invalid("sem diretório")
        val count = tail.u16(eocd + EOCD_COUNT)
        val cdSize = tail.u32(eocd + EOCD_CD_SIZE)
        val cdOffset = tail.u32(eocd + EOCD_CD_OFFSET)
        if (cdOffset + cdSize > fileSize) throw invalid("diretório fora do arquivo")
        val cd = tree.read(path, cdOffset, cdSize.toInt())
        val entries = ArrayList<Entry>(count)
        var p = 0
        repeat(count) {
            if (p + CD_HEADER > cd.size || cd.u32(p) != CD_SIGNATURE) throw invalid("entrada inválida")
            val method = cd.u16(p + CD_METHOD)
            val compressed = cd.u32(p + CD_COMPRESSED)
            val size = cd.u32(p + CD_SIZE)
            val nameLength = cd.u16(p + CD_NAME_LENGTH)
            val extraLength = cd.u16(p + CD_EXTRA_LENGTH)
            val commentLength = cd.u16(p + CD_COMMENT_LENGTH)
            val offset = cd.u32(p + CD_LOCAL_OFFSET)
            val name = cd.decodeToString(p + CD_HEADER, p + CD_HEADER + nameLength).replace('\\', '/')
            p += CD_HEADER + nameLength + extraLength + commentLength
            if (name.endsWith("/") || !isSafe(name)) return@repeat
            entries += Entry(name, method, compressed, size, offset)
        }
        return entries
    }

    /** Image entries in reading order, macOS metadata skipped. */
    suspend fun images(): List<Entry> =
        entries()
            .filter { LocalFormat.isImage(it.name) && !it.name.startsWith("__MACOSX/") && !it.name.substringAfterLast('/').startsWith(".") }
            .sortedWith(compareBy(NaturalOrder) { it.name })

    suspend fun read(entry: Entry): ByteArray {
        if (entry.size > MAX_ENTRY_BYTES) throw invalid("imagem grande demais")
        if (entry.compressedSize > 0 && entry.size / entry.compressedSize > MAX_RATIO) throw invalid("compressão suspeita")
        val header = tree.read(path, entry.localHeaderOffset, LOCAL_HEADER)
        if (header.u32(0) != LOCAL_SIGNATURE) throw invalid("cabeçalho inválido")
        val dataStart = entry.localHeaderOffset + LOCAL_HEADER + header.u16(LOCAL_NAME_LENGTH) + header.u16(LOCAL_EXTRA_LENGTH)
        val raw = tree.read(path, dataStart, entry.compressedSize.toInt())
        return when (entry.method) {
            METHOD_STORED -> raw
            METHOD_DEFLATED -> inflateRaw(raw, entry.size.toInt())
            else -> throw invalid("compressão ${entry.method} não suportada")
        }
    }

    private fun invalid(reason: String) = SourceException("CBZ inválido ($reason): $path", SourceException.Kind.PARSE)

    companion object {
        const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
        private const val MAX_RATIO = 1000
        private const val EOCD_SIZE = 22
        private const val MAX_COMMENT = 65_535
        private const val CD_HEADER = 46
        private const val LOCAL_HEADER = 30
        private const val EOCD_SIGNATURE = 0x06054b50L
        private const val CD_SIGNATURE = 0x02014b50L
        private const val LOCAL_SIGNATURE = 0x04034b50L
        private const val METHOD_STORED = 0
        private const val METHOD_DEFLATED = 8

        // Field offsets from the ZIP specification (APPNOTE 4.3).
        private const val EOCD_COUNT = 10
        private const val EOCD_CD_SIZE = 12
        private const val EOCD_CD_OFFSET = 16
        private const val CD_METHOD = 10
        private const val CD_COMPRESSED = 20
        private const val CD_SIZE = 24
        private const val CD_NAME_LENGTH = 28
        private const val CD_EXTRA_LENGTH = 30
        private const val CD_COMMENT_LENGTH = 32
        private const val CD_LOCAL_OFFSET = 42
        private const val LOCAL_NAME_LENGTH = 26
        private const val LOCAL_EXTRA_LENGTH = 28

        fun isSafe(name: String): Boolean =
            !name.startsWith("/") && name.split('/').none { it == ".." } && !Regex("^[A-Za-z]:").containsMatchIn(name)
    }
}

private const val BYTE_MASK = 0xff
private const val BYTE_BITS = 8
private const val SHORT_BITS = 16

private fun ByteArray.u16(i: Int): Int = (this[i].toInt() and BYTE_MASK) or ((this[i + 1].toInt() and BYTE_MASK) shl BYTE_BITS)

private fun ByteArray.u32(i: Int): Long = (u16(i).toLong()) or (u16(i + 2).toLong() shl SHORT_BITS)
