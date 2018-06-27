package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.comics.Chapter
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.viewModels.ChapterViewModel
import io.realm.Realm
import io.realm.RealmList

object ChapterFactory {

    fun createChapterForRealm(chapterViewModel: ChapterViewModel, comic: Comic, realm: Realm): Chapter {
        val realmObject: Chapter?

        realmObject = realm.createObject(Chapter::class.java, RealmUtils.getDataId<Chapter>(realm))

        realmObject.apply {
            if (chapterViewModel.id != -1L) {
                this.id = chapterViewModel.id
            }

            if (chapterViewModel.chapterName != null) {
                this.chapterName = chapterViewModel.chapterName
            }

            if (chapterViewModel.chapterPath != null) {
                this.chapterPath = chapterViewModel.chapterPath
            }

            if (chapterViewModel.comic != null) {
                this.comic = realm.copyToRealmOrUpdate(comic)
            }

            this.lastPageRead = chapterViewModel.lastPageRead
        }

        return realmObject
    }

    fun createListOfChaptersFormRealm(listNetwork: List<ChapterViewModel>?, comic: Comic, realm: Realm): RealmList<Chapter> {
        val list = RealmList<Chapter>()
        listNetwork?.forEach { list.add(createChapterForRealm(it, comic, realm)) }

        return list
    }

}