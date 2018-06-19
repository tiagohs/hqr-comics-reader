package com.tiagohs.hqr.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.R
import com.tiagohs.hqr.helpers.utils.DiskUtils
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.sources.ISource
import java.io.File

// Classe responsável pela manipulação das pastas criadas durante os downloads

class DownloadProvider(
        context: Context?
) {

    val downloadDirectory = UniFile.fromUri(context, Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context?.getString(R.string.app_name), "downloads")))

    fun getComicDirectory(comic: Comic, source: ISource): UniFile? {
        return downloadDirectory
                        .createDirectory(getSourceDirectoryName(source))
                        .createDirectory(getComicDirectoryName(comic))
    }

    fun findSourceDirectory(source: ISource): UniFile? {
        return downloadDirectory.findFile(getSourceDirectoryName(source))
    }

    fun findComicDirectory(comic: Comic, source: ISource): UniFile? {
        val sourceFile = findSourceDirectory(source)
        return sourceFile?.findFile(getComicDirectoryName(comic))
    }

    fun findChapterDirectory(chapter: Chapter, comic: Comic, source: ISource): UniFile? {
        val comicFile = findComicDirectory(comic, source)
        return comicFile?.findFile(getChapterDirectoryName(chapter))
    }

    fun getSourceDirectoryName(source: ISource): String {
        return DiskUtils.buildValidFilename(source.name)
    }

    fun getComicDirectoryName(comic: Comic): String {
        return DiskUtils.buildValidFilename(comic.title!!)
    }

    fun getChapterDirectoryName(chapter: Chapter): String {
        return DiskUtils.buildValidFilename(chapter.name!!)
    }
}