package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import io.realm.Realm
import io.realm.RealmList

object ChapterFactory {

    fun createChapterForRealm(chapterViewModel: ChapterViewModel, comic: Comic, realm: Realm): Chapter {
        return Chapter().create().apply {
            copyFromChapterViewModel(this, chapterViewModel, comic, realm)

            if (chapterViewModel.chapterPath != null) {
                this.chapterPath = chapterViewModel.chapterPath!!
            }
        }
    }

    fun copyFromChapterViewModel(chapter: Chapter, chapterViewModel: ChapterViewModel, comic: Comic, realm: Realm): Chapter {

        if (chapterViewModel.chapterName != null) {
            chapter.chapterName = chapterViewModel.chapterName
        }

        if (chapterViewModel.chapterPath != null) {
            chapter.chapterPath = chapterViewModel.chapterPath
        }

        chapter.comic = realm.copyToRealmOrUpdate(comic)
        chapter.lastPageRead = chapterViewModel.lastPageRead

        return chapter
    }

    fun createListOfChaptersFormRealm(listNetwork: List<ChapterViewModel>?, comic: Comic,  realm: Realm): RealmList<Chapter> {
        val list = RealmList<Chapter>()
        var id = RealmUtils.getDataId<Chapter>()

        listNetwork?.forEach {
            val chapter = createChapterForRealm(it, comic, realm)
            chapter.id = id++
            it.id = id++

            id++
            list.add(realm.copyToRealmOrUpdate(chapter) )
        }

        return list
    }

    fun createListOfChapterViewModel(comicsDb: List<Chapter>): List<ChapterViewModel> {
        val chapterDbList = ArrayList<ChapterViewModel>()
        chapterDbList.addAll(comicsDb.map { ChapterViewModel().create(it) })

        return chapterDbList
    }

}