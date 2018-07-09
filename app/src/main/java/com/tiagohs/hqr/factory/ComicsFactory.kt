package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Comic
import com.tiagohs.hqr.models.view_models.ComicViewModel
import io.realm.Realm
import io.realm.RealmList

object ComicsFactory {

    fun createComicViewModel(comicDb: Comic): ComicViewModel {
        return ComicViewModel().create(comicDb)
    }

    fun copyFromComicViewModel(comic: Comic, comicViewModel: ComicViewModel, source: SourceDB?, realm: Realm): Comic {

        comic.inicialized = comicViewModel.inicialized
        comic.favorite = comicViewModel.favorite

        if (comicViewModel.name != null) {
            comic.name = comicViewModel.name
        }

        if (comicViewModel.posterPath != null) {
            comic.posterPath = comicViewModel.posterPath
        }

        if (comicViewModel.pathLink != null) {
            comic.pathLink = comicViewModel.pathLink
        }

        if (comicViewModel.summary != null) {
            comic.summary = comicViewModel.summary
        }

        if (comicViewModel.publicationDate != null) {
            comic.publicationDate = comicViewModel.publicationDate
        }

        if (source != null) {
            comic.source = realm.copyToRealmOrUpdate(source)
        }

        if (comicViewModel.publisher != null) {
            comic.publisher = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.publisher, source, realm)
        }

        if (comicViewModel.genres != null) {
            comic.genres = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.genres, source, realm)
        }

        if (comicViewModel.authors != null) {
            comic.authors = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.authors, source, realm)
        }

        if (comicViewModel.scanlators != null) {
            comic.scanlators = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.scanlators, source, realm)
        }

        if (comicViewModel.chapters != null) {
            comic.chapters = ChapterFactory.createListOfChaptersFormRealm(comicViewModel.chapters, comic, realm)
        }

        if (comicViewModel.lastUpdate != null) {
            comic.lastUpdate = comicViewModel.lastUpdate
        }

        if (comicViewModel.status != null) {
            comic.status = comicViewModel.status
        }

        if (comicViewModel.tags != null) {
            comic.tags = createListOfTagsForRealm(comicViewModel.tags)
        }

        return comic
    }

    fun createComicModelForRealm(comicViewModel: ComicViewModel, source: SourceDB?, realm: Realm): Comic {
        return Comic().create().apply {
            copyFromComicViewModel(this, comicViewModel, source, realm)
        }
    }

    fun createListOfComicModelFormRealm(comicViewModels: List<ComicViewModel>?, source: SourceDB?, realm: Realm): List<Comic> {
        val comicDbList = ArrayList<Comic>()
        var id = RealmUtils.getDataId<Comic>()

        comicViewModels?.forEach {

            val comic = createComicModelForRealm(it, source, realm)
            comic.id = id
            it.id = id

            id++

            comicDbList.add(realm.copyToRealmOrUpdate(comic) )
        }

        return comicDbList
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