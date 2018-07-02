package com.tiagohs.hqr.download

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DiskUtils
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel

// Classe responsável pela manipulação das pastas criadas durante os downloads

class DownloadProvider(
        private val context: Context?,
        private val preferences: PreferenceHelper
) {

    private var downloadDirectory = preferences.downloadsDirectory().getOrDefault().let {
        UniFile.fromUri(context, Uri.parse(it))
    }

    init {
        preferences.downloadsDirectory().asObservable()
                .skip(1)
                .subscribe { downloadDirectory = UniFile.fromUri(context, Uri.parse(it)) }
    }

    fun getDownloadDirectorySize(): String {
        return Formatter.formatFileSize(context, DiskUtils.getDirectorySize(downloadDirectory))
    }

    fun getComicDirectory(comic: ComicViewModel, source: SourceDB): UniFile? {
        return downloadDirectory.createDirectory(getSourceDirectoryName(source))
                                .createDirectory(getComicDirectoryName(comic))
    }

    fun findSourceDirectory(source: SourceDB): UniFile? {
        return downloadDirectory.findFile(getSourceDirectoryName(source))
    }

    fun findComicDirectory(comic: ComicViewModel, source: SourceDB): UniFile? {
        val sourceFile = findSourceDirectory(source)
        return sourceFile?.findFile(getComicDirectoryName(comic))
    }

    fun findChapterDirectory(chapter: ChapterViewModel, comic: ComicViewModel, source: SourceDB): UniFile? {
        val comicFile = findComicDirectory(comic, source)
        return comicFile?.findFile(getChapterDirectoryName(chapter))
    }

    fun getSourceDirectoryName(source: SourceDB): String {
        return DiskUtils.buildValidFilename(source.name!!)
    }

    fun getComicDirectoryName(comic: ComicViewModel): String {
        return DiskUtils.buildValidFilename(comic.name!!)
    }

    fun getChapterDirectoryName(chapter: ChapterViewModel): String {
        return DiskUtils.buildValidFilename(chapter.chapterName!!)
    }

    fun deleteAll() {
        downloadDirectory.listFiles()?.forEach {it.delete() }
    }
}