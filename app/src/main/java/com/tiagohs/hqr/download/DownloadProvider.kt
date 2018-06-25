package com.tiagohs.hqr.download

import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.helpers.tools.PreferenceHelper
import com.tiagohs.hqr.helpers.tools.getOrDefault
import com.tiagohs.hqr.helpers.utils.DiskUtils
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.sources.IHttpSource

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

    fun getComicDirectory(comic: Comic, source: IHttpSource): UniFile? {
        return downloadDirectory
                        .createDirectory(getSourceDirectoryName(source))
                        .createDirectory(getComicDirectoryName(comic))
    }

    fun findSourceDirectory(source: IHttpSource): UniFile? {
        return downloadDirectory.findFile(getSourceDirectoryName(source))
    }

    fun findComicDirectory(comic: Comic, source: IHttpSource): UniFile? {
        val sourceFile = findSourceDirectory(source)
        return sourceFile?.findFile(getComicDirectoryName(comic))
    }

    fun findChapterDirectory(chapter: Chapter, comic: Comic, source: IHttpSource): UniFile? {
        val comicFile = findComicDirectory(comic, source)
        return comicFile?.findFile(getChapterDirectoryName(chapter))
    }

    fun getSourceDirectoryName(source: IHttpSource): String {
        return DiskUtils.buildValidFilename(source.name)
    }

    fun getComicDirectoryName(comic: Comic): String {
        return DiskUtils.buildValidFilename(comic.title!!)
    }

    fun getChapterDirectoryName(chapter: Chapter): String {
        return DiskUtils.buildValidFilename(chapter.name!!)
    }
}