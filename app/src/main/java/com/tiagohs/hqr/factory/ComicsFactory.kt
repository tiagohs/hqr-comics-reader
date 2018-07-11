package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.view_models.ChapterViewModel
import com.tiagohs.hqr.models.view_models.ComicViewModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import io.realm.Realm
import io.realm.RealmList

object ComicsFactory {

    fun createComicModelForRealm(comicViewModel: ComicViewModel, source: SourceDB?, realm: Realm): Comic {
        val realmObject = realm.createObject(Comic::class.java, RealmUtils.getDataId<Comic>(realm))

        realmObject.apply {
            if (!comicViewModel.name.isNullOrEmpty()) {
                this.name = comicViewModel.name
            }

            if (!comicViewModel.posterPath.isNullOrEmpty()) {
                this.posterPath = comicViewModel.posterPath
            }

            if (!comicViewModel.pathLink.isNullOrEmpty()) {
                this.pathLink = comicViewModel.pathLink
            }

            if (!comicViewModel.summary.isNullOrEmpty()) {
                this.summary = comicViewModel.summary
            }

            if (!comicViewModel.publicationDate.isNullOrEmpty()) {
                this.publicationDate = comicViewModel.publicationDate
            }

            if (source != null) {
                this.source = realm.copyToRealmOrUpdate(source)
            }

            if (comicViewModel.publisher != null && comicViewModel.publisher!!.isNotEmpty()) {
                this.publisher = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.publisher, source, realm)
            }

            if (comicViewModel.genres != null && comicViewModel.genres!!.isNotEmpty()) {
                this.genres = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.genres, source, realm)
            }

            if (comicViewModel.authors != null && comicViewModel.authors!!.isNotEmpty()) {
                this.authors = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.authors, source, realm)
            }

            if (comicViewModel.scanlators != null && comicViewModel.scanlators!!.isNotEmpty()) {
                this.scanlators = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.scanlators, source, realm)
            }

            if (comicViewModel.chapters != null && comicViewModel.chapters!!.isNotEmpty()) {
                this.chapters = ChapterFactory.createListOfChaptersFormRealm(comicViewModel.chapters, this, realm)
            }

            if (!comicViewModel.lastUpdate.isNullOrEmpty()) {
                this.lastUpdate = comicViewModel.lastUpdate
            }

            if (!comicViewModel.status.isNullOrEmpty()) {
                this.status = comicViewModel.status
            }

            if (comicViewModel.tags != null && comicViewModel.tags!!.isNotEmpty()) {
                this.tags = createListOfTagsForRealm(comicViewModel.tags)
            }
        }

        return realmObject
    }

    fun copyFromComicViewModel(comic: Comic, comicViewModel: ComicViewModel, source: SourceDB?, realm: Realm): Comic {

        if (!comicViewModel.name.isNullOrEmpty()) {
            comic.name = comicViewModel.name
        }

        if (!comicViewModel.posterPath.isNullOrEmpty()) {
            comic.posterPath = comicViewModel.posterPath
        }

        if (!comicViewModel.pathLink.isNullOrEmpty()) {
            comic.pathLink = comicViewModel.pathLink
        }

        if (!comicViewModel.summary.isNullOrEmpty()) {
            comic.summary = comicViewModel.summary
        }

        if (!comicViewModel.publicationDate.isNullOrEmpty()) {
            comic.publicationDate = comicViewModel.publicationDate
        }

        if (source != null) {
            comic.source = realm.copyToRealmOrUpdate(source)
        }

        if (comicViewModel.publisher != null && comicViewModel.publisher!!.isNotEmpty()) {
            comic.publisher = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.publisher, source, realm)
        }

        if (comicViewModel.genres != null && comicViewModel.genres!!.isNotEmpty()) {
            comic.genres = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.genres, source, realm)
        }

        if (comicViewModel.authors != null && comicViewModel.authors!!.isNotEmpty()) {
            comic.authors = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.authors, source, realm)
        }

        if (comicViewModel.scanlators != null && comicViewModel.scanlators!!.isNotEmpty()) {
            comic.scanlators = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.scanlators, source, realm)
        }

        if (comicViewModel.chapters != null && comicViewModel.chapters!!.isNotEmpty()) {
            comic.chapters = ChapterFactory.createListOfChaptersFormRealm(comicViewModel.chapters, comic, realm)
        }

        if (!comicViewModel.lastUpdate.isNullOrEmpty()) {
            comic.lastUpdate = comicViewModel.lastUpdate
        }

        if (!comicViewModel.status.isNullOrEmpty()) {
            comic.status = comicViewModel.status
        }

        if (comicViewModel.tags != null && comicViewModel.tags!!.isNotEmpty()) {
            comic.tags = createListOfTagsForRealm(comicViewModel.tags)
        }

        comic.favorite = comicViewModel.favorite
        comic.inicialized = comicViewModel.inicialized

        return comic
    }

    fun createComicViewModel(comicDb: Comic): ComicViewModel {
        return ComicViewModel().apply {
            if (comicDb.id != -1L) {
                this.id = comicDb.id
            }

            if (!comicDb.name.isNullOrEmpty()) {
                this.name = comicDb.name
            }

            if (!comicDb.pathLink.isNullOrEmpty()) {
                this.pathLink = comicDb.pathLink
            }

            if (comicDb.source != null) {
                this.source = SourceDB().create(comicDb.source!!)
            }

            if (!comicDb.posterPath.isNullOrEmpty()) {
                this.posterPath = comicDb.posterPath
            }

            if (!comicDb.summary.isNullOrEmpty()) {
                this.summary = comicDb.summary
            }

            if (!comicDb.publicationDate.isNullOrEmpty()) {
                this.publicationDate = comicDb.publicationDate
            }

            if (comicDb.publisher != null && comicDb.publisher!!.isNotEmpty()) {
                this.publisher = comicDb.publisher!!.map { DefaultModelView().create(it, source) }
            }

            if (comicDb.genres != null && comicDb.genres!!.isNotEmpty()) {
                this.genres = comicDb.genres!!.map { DefaultModelView().create(it, source) }
            }

            if (comicDb.authors != null && comicDb.authors!!.isNotEmpty()) {
                this.authors = comicDb.authors!!.map { DefaultModelView().create(it, source) }
            }

            if (comicDb.scanlators != null && comicDb.scanlators!!.isNotEmpty()) {
                this.scanlators = comicDb.scanlators!!.map { DefaultModelView().create(it, source) }
            }

            if (comicDb.chapters != null && comicDb.chapters!!.isNotEmpty()) {
                this.chapters = comicDb.chapters!!.toList().map { ChapterViewModel().create(it) }
            }

            if (comicDb.tags != null && comicDb.tags!!.isNotEmpty()) {
                this.tags = comicDb.tags!!.toList()
            }

            if (!comicDb.lastUpdate.isNullOrEmpty()) {
                this.lastUpdate = comicDb.lastUpdate
            }

            if (!comicDb.status.isNullOrEmpty()) {
                this.status = comicDb.status
            }

            this.favorite = comicDb.favorite
            this.inicialized = comicDb.inicialized
        }
    }

    fun createListOfComicModelFormRealm(comicViewModels: List<ComicViewModel>?, source: SourceDB?, realm: Realm): List<Comic> {
        val list = ArrayList<Comic>()

        comicViewModels?.forEach {
            val comicLocal = realm.where(Comic::class.java)
                    .equalTo("source.id", source?.id)
                    .equalTo("pathLink", it.pathLink)
                    .findFirst()

            if (comicLocal == null) {
                list.add( createComicModelForRealm(it, source, realm) )
            } else {
                list.add( realm.copyToRealmOrUpdate(comicLocal))
            }
        }

        return list
    }

    fun createListOfComicViewModel(comicsDb: List<Comic>): List<ComicViewModel> {
        val comicDbList = ArrayList<ComicViewModel>()
        comicDbList.addAll(comicsDb.map { createComicViewModel(it) })

        return comicDbList
    }

    fun createListOfTagsForRealm(tags: List<String>?): RealmList<String> {
        val list = RealmList<String>()
        if (tags != null) list.addAll(tags)

        return list
    }

}