package com.tiagohs.hqr.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.hippo.unifile.UniFile
import com.tiagohs.hqr.R
import com.tiagohs.hqr.models.sources.Chapter
import com.tiagohs.hqr.models.sources.Comic
import com.tiagohs.hqr.models.sources.Source
import com.tiagohs.hqr.utils.DiskUtils
import java.io.File

// Classe responsável pela manipulação das pastas criadas durante os downloads

class DownloadProvider(
        context: Context?
) {

    val downloadDirectory = UniFile.fromUri(context, Uri.fromFile(
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator +
                    context?.getString(R.string.app_name), "downloads")))

    fun getComicDirectory(comic: Comic, source: Source): UniFile? {
        return downloadDirectory
                        .createDirectory(getSourceDirectoryName(source))
                        .createDirectory(getComicDirectoryName(comic))
    }

    fun findSourceDirectory(source: Source): UniFile? {
        return downloadDirectory.findFile(getSourceDirectoryName(source))
    }

    fun findComicDirectory(comic: Comic, source: Source): UniFile? {
        var source = findSourceDirectory(source)
        return source?.findFile(getComicDirectoryName(comic))
    }

    fun findChapterDirectory(chapter: Chapter, comic: Comic, source: Source): UniFile? {
        var comic = findComicDirectory(comic, source)
        return comic?.findFile(getChapterDirectoryName(chapter))
    }

    fun getSourceDirectoryName(source: Source): String {
        return DiskUtils.buildValidFilename(source.name!!)
    }

    fun getComicDirectoryName(comic: Comic): String {
        return DiskUtils.buildValidFilename(comic.title!!)
    }

    fun getChapterDirectoryName(chapter: Chapter): String {
        return DiskUtils.buildValidFilename(chapter.name!!)
    }
}