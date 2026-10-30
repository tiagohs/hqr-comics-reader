package com.tiagohs.hqr.sources.local

import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import com.tiagohs.hqr.domain.source.SourceException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.encodeUtf8
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * CBR (junrar, RAR up to v7) and CB7 (Commons Compress) unpacked to [cache], one folder per archive, the last [keep]
 * kept (RF-13, ADR-011); covers are kept beside it, in "archive-covers". Each file is saved under its position, never
 * under its name, so a name with ".." goes nowhere; files bigger than [maxEntryBytes] or archives past [maxTotalBytes]
 * are refused (RNF-26).
 */
class JvmArchiveUnpacker(
    private val tree: FileTree,
    private val cache: File,
    private val keep: Int = KEEP,
    private val maxEntryBytes: Long = MAX_ENTRY,
    private val maxTotalBytes: Long = MAX_TOTAL,
) : ArchiveUnpacker {
    private val lock = Mutex()
    private var last: Pair<String, Unpacked>? = null

    private class Unpacked(
        val dir: File,
        val names: List<String>,
    )

    override suspend fun names(path: String): List<String> = lock.withLock { unpack(path).names }

    override suspend fun read(
        path: String,
        name: String,
    ): ByteArray =
        lock.withLock {
            val unpacked = unpack(path)
            val index = unpacked.names.indexOf(name)
            if (index < 0) throw SourceException("Página não encontrada", SourceException.Kind.NOT_FOUND)
            withContext(Dispatchers.IO) { File(unpacked.dir, index.toString()).readBytes() }
        }

    override suspend fun cover(path: String): ByteArray? =
        lock.withLock {
            val file = File(File(cache.parentFile, COVERS), key(path, tree.size(path)))
            if (withContext(Dispatchers.IO) { file.exists() }) return@withLock withContext(Dispatchers.IO) { file.readBytes() }
            val unpacked = unpack(path)
            val first =
                unpacked.names
                    .withIndex()
                    .filter { isPage(it.value) }
                    .minWithOrNull(compareBy(NaturalOrder) { it.value })
            first?.let { page ->
                withContext(Dispatchers.IO) {
                    File(unpacked.dir, page.index.toString()).readBytes().also { bytes ->
                        file.parentFile.mkdirs()
                        file.writeBytes(bytes)
                    }
                }
            }
        }

    private fun key(
        path: String,
        size: Long,
    ) = "$path|$size".encodeUtf8().sha256().hex().take(KEY)

    private suspend fun unpack(path: String): Unpacked {
        val size = tree.size(path)
        // The same file (path and size) still unpacked: the cache may have been cleared by the system meanwhile.
        last
            ?.takeIf { it.first == "$path|$size" && withContext(Dispatchers.IO) { File(it.second.dir, INDEX).exists() } }
            ?.let { return it.second }
        val dir = File(cache, key(path, size))
        val index = File(dir, INDEX)
        if (!index.exists()) {
            withContext(Dispatchers.IO) { dir.deleteRecursively() }
            val copy = File(cache, "archive.tmp")
            try {
                copyOut(path, size, copy)
                val names = withContext(Dispatchers.IO) { unpackTo(LocalFormat.of(path), copy, dir) }
                withContext(Dispatchers.IO) {
                    val tmp = File(dir, "$INDEX.tmp")
                    tmp.writeText(names.joinToString("\n"))
                    tmp.renameTo(index)
                }
            } catch (e: SourceException) {
                withContext(Dispatchers.IO) { dir.deleteRecursively() }
                throw e
            } finally {
                withContext(Dispatchers.IO) { copy.delete() }
            }
        }
        val unpacked =
            withContext(Dispatchers.IO) {
                dir.setLastModified(System.currentTimeMillis())
                trim()
                Unpacked(dir, index.readLines().filter { it.isNotEmpty() })
            }
        last = "$path|$size" to unpacked
        return unpacked
    }

    /** The archive comes out of the chosen folder by ranges (the folder may be a content provider). */
    private suspend fun copyOut(
        path: String,
        size: Long,
        target: File,
    ) {
        withContext(Dispatchers.IO) { cache.mkdirs() }
        withContext(Dispatchers.IO) { target.outputStream() }.use { out ->
            var offset = 0L
            while (offset < size) {
                val chunk = tree.read(path, offset, minOf(CHUNK.toLong(), size - offset).toInt())
                if (chunk.isEmpty()) break
                withContext(Dispatchers.IO) { out.write(chunk) }
                offset += chunk.size
            }
        }
    }

    private fun unpackTo(
        format: LocalFormat?,
        file: File,
        dir: File,
    ): List<String> {
        dir.mkdirs()
        val names = mutableListOf<String>()
        val budget = Budget(maxEntryBytes, maxTotalBytes)
        try {
            when (format) {
                LocalFormat.CBR -> rar(file, dir, names, budget)
                LocalFormat.CB7 -> sevenZ(file, dir, names, budget)
                else -> throw SourceException("Formato não suportado", SourceException.Kind.UNAVAILABLE)
            }
        } catch (e: PasswordRequiredException) {
            throw SourceException("Arquivo com senha", SourceException.Kind.UNAVAILABLE, e)
        } catch (e: RarException) {
            throw SourceException("Não foi possível abrir o CBR", SourceException.Kind.UNAVAILABLE, e)
        } catch (e: IOException) {
            throw SourceException("Não foi possível abrir o arquivo", SourceException.Kind.UNAVAILABLE, e)
        }
        return names
    }

    private fun rar(
        file: File,
        dir: File,
        names: MutableList<String>,
        budget: Budget,
    ) = Archive(file).use { archive ->
        if (archive.isEncrypted) throw SourceException("Arquivo com senha", SourceException.Kind.UNAVAILABLE)
        // In order: a solid archive only unpacks from the start.
        archive.fileHeaders.filter { !it.isDirectory }.forEach { header ->
            if (header.isEncrypted) throw SourceException("Arquivo com senha", SourceException.Kind.UNAVAILABLE)
            save(archive.getInputStream(header), File(dir, names.size.toString()), budget)
            names += header.fileName.replace('\\', '/')
        }
    }

    private fun save(
        input: InputStream,
        target: File,
        budget: Budget,
    ) = input.use { source -> target.outputStream().use { budget.copy(source, it) } }

    private fun sevenZ(
        file: File,
        dir: File,
        names: MutableList<String>,
        budget: Budget,
    ) = SevenZFile.builder().setFile(file).get().use { archive ->
        generateSequence { archive.nextEntry }.filter { !it.isDirectory && it.hasStream() }.forEach { entry ->
            save(archive.getInputStream(entry), File(dir, names.size.toString()), budget)
            names += entry.name.replace('\\', '/')
        }
    }

    /** Counts what really comes out, whatever the headers claim (compression bombs). */
    private class Budget(
        private val perEntry: Long,
        private val total: Long,
    ) {
        private var used = 0L

        fun copy(
            input: InputStream,
            out: OutputStream,
        ) {
            val buffer = ByteArray(BUFFER)
            var entry = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                entry += n
                used += n
                if (entry > perEntry || used > total) throw SourceException("Arquivo grande demais", SourceException.Kind.UNAVAILABLE)
                out.write(buffer, 0, n)
            }
        }
    }

    /** Only the most recent archives stay unpacked. */
    private fun trim() {
        cache
            .listFiles { f -> f.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keep)
            ?.forEach { it.deleteRecursively() }
    }

    private companion object {
        const val KEEP = 3
        const val COVERS = "archive-covers"
        const val KEY = 24
        const val INDEX = "index"
        const val CHUNK = 1 shl 20
        const val BUFFER = 1 shl 16
        const val MAX_ENTRY = 64L shl 20
        const val MAX_TOTAL = 2L shl 30
    }
}
