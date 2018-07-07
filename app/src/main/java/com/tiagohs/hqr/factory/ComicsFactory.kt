package com.tiagohs.hqr.factory

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

        if (comicViewModel.pathLink != null) {
            comic.pathLink = comicViewModel.pathLink
        }

        if (comicViewModel.posterPath != null) {
            comic.posterPath = comicViewModel.posterPath
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
            comic.publisher = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.publisher, realm)
        }

        if (comicViewModel.genres != null) {
            comic.genres = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.genres, realm)
        }

        if (comicViewModel.authors != null) {
            comic.authors = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.authors, realm)
        }

        if (comicViewModel.scanlators != null) {
            comic.scanlators = DefaultModelFactory.createListOfDefaultModelForRealm(comicViewModel.scanlators, realm)
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
        val comic = Comic().create().apply {
            copyFromComicViewModel(this, comicViewModel, source, realm)
        }

        return comic
    }

    fun createListOfComicModelFormRealm(comicViewModels: List<ComicViewModel>, source: SourceDB?, realm: Realm): List<Comic> {
        val comicDbList = ArrayList<Comic>()
        comicDbList.addAll(comicViewModels.map { createComicModelForRealm(it, source, realm) })

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