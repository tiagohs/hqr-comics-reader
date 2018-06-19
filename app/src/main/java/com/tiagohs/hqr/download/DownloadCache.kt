package com.tiagohs.hqr.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.sources.SourceManager
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadCache(
        private val context: Context,
        private val sourceManager: SourceManager,
        private val provider: DownloadProvider
) {

    private val renewInterval = TimeUnit.HOURS.toMillis(1)
    private var lastRenew = 0L
    private var rootDir = RootDir(getDirectory())

    fun isChapterDownloaded(comic: Comic, chapter: Chapter, skipCache: Boolean): Boolean {

        if (skipCache) {
            val source = sourceManager.get(comic.sourceId) ?: return false
            return provider.findChapterDirectory(chapter, comic, source) != null
        }

        checkRenew()

        val sourceDir = rootDir.files[comic.sourceId]

        if (sourceDir != null) {
            val comicDir = sourceDir.files[provider.getComicDirectoryName(comic)]

            if (comicDir != null) {
                return provider.getChapterDirectoryName(chapter) in comicDir.files
            }
        }

        return false
    }

    fun getDownloadCount(comic: Comic): Int {
        checkRenew()

        val sourceDir = rootDir.files[comic.sourceId]
        if (sourceDir != null) {
            val comicDir = sourceDir.files[provider.getComicDirectoryName(comic)]

            if (comicDir != null) return comicDir.files.size
        }

        return 0
    }

    @Synchronized
    fun addChapter(chapterDirName: String, comicFile: UniFile, comic: Comic) {
        var sourceDir = rootDir.files[comic.sourceId]

        if (sourceDir == null) {
            val source = sourceManager.get(comic.sourceId) ?: return
            val sourceUniFile = provider.findSourceDirectory(source) ?: return
            sourceDir = SourceDir(sourceUniFile)
            rootDir.files += comic.sourceId to sourceDir
        }

        val comicDirName = provider.getComicDirectoryName(comic)
        var comicDir = sourceDir.files[comicDirName]

        if (comicDir == null) {
            comicDir = ComicDir(comicFile)
            sourceDir.files += comicDirName to comicDir
        }

        comicDir.files += chapterDirName
    }

    @Synchronized
    fun removeChapter(chapter: Chapter, comic: Comic) {
        val sourceDir = rootDir.files[comic.sourceId] ?: return
        val comicDir = sourceDir.files[provider.getComicDirectoryName(comic)] ?: return
        val chapterDirName = provider.getChapterDirectoryName(chapter)

        if (chapterDirName in comicDir.files) {
            comicDir.files -= chapterDirName
        }
    }

    @Synchronized
    fun removeManga(comic: Comic) {
        val sourceDir = rootDir.files[comic.sourceId] ?: return
        val comicDirName = provider.getComicDirectoryName(comic)
        if (comicDirName in sourceDir.files) {
            sourceDir.files -= comicDirName
        }
    }

    @Synchronized
    private fun checkRenew() {
        if (lastRenew + renewInterval < System.currentTimeMillis()) {
            renew()
            lastRenew = System.currentTimeMillis()
        }
    }

    private fun renew() {
        val sources = sourceManager.getHttpSouces()

        val sourceDirs = rootDir.dir.listFiles()
                .orEmpty()
                .associate { it.name to SourceDir(it) }
                .mapNotNullKeys { entry ->
                    sources.find { provider.getSourceDirectoryName(it) == entry.key }?.id
                }

        rootDir.files = sourceDirs

        sourceDirs.values.forEach { sourceDir ->
            var comicDirs = sourceDir.dir.listFiles()
                    .orEmpty()
                    .associateNotNullKeys { it.name to ComicDir(it) }

            sourceDir.files = comicDirs

            comicDirs.values.forEach { comicDir ->
                val chapterDirs = comicDir.dir.listFiles()
                        .orEmpty()
                        .mapNotNull { it.name }
                        .toHashSet()

                comicDir.files = chapterDirs
            }
        }
    }

    private fun getDirectory(): UniFile {
        return UniFile.fromUri(context, Uri.fromFile(
                File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                        context?.getString(R.string.app_name), "downloads")))
    }

    private class RootDir(val dir: UniFile,
                                var files: Map<Long, SourceDir> = hashMapOf())

    private class SourceDir(val dir: UniFile,
                                  var files: Map<String, ComicDir> = hashMapOf())

    private class ComicDir(val dir: UniFile,
                                 var files: Set<String> = hashSetOf())

    private inline fun <K, V, R> Map<out K, V>.mapNotNullKeys(transform: (Map.Entry<K?, V>) -> R?): Map<R, V> {
        val destination = LinkedHashMap<R, V>()
        forEach { element -> transform(element)?.let { destination.put(it, element.value) } }
        return destination
    }

    private inline fun <T, K, V> Array<T>.associateNotNullKeys(transform: (T) -> Pair<K?, V>): Map<K, V> {
        val destination = LinkedHashMap<K, V>()
        for (element in this) {
            val (key, value) = transform(element)
            if (key != null) {
                destination.put(key, value)
            }
        }
        return destination
    }

}