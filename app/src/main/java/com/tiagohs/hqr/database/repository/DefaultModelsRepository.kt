package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.IDefaultModelsRepository
import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.factory.DefaultModelFactory
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.DefaultModelView
import io.reactivex.Observable
import io.realm.Realm
import io.realm.RealmQuery

class DefaultModelsRepository(
        private val sourceRepository: ISourceRepository
): BaseRepository(), IDefaultModelsRepository {

    override fun insertRealm(defaultModelView: DefaultModelView, sourceId: Long): DefaultModelView {
        val realm = Realm.getDefaultInstance()
        val localDefaultModel = DefaultModelView()

        try {
            val source: SourceDB? = sourceRepository.getSourceByIdRealm(sourceId)
            var result = realm.where(DefaultModel::class.java)
                    .equalTo("pathLink", defaultModelView.pathLink)
                    .findFirst()

            realm.executeTransaction { r ->
                if (result != null) {
                    result = DefaultModelFactory.copyFromDefaultModelView(result!!, defaultModelView, source, r)
                } else {
                    result = DefaultModel().create().apply {
                        DefaultModelFactory.copyFromDefaultModelView(this, defaultModelView, source, r)
                    }
                }

                r.insertOrUpdate(result)
            }

            localDefaultModel.copyFrom(result!!, source)

            finishTransaction(realm)
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }

        return localDefaultModel
    }

    override fun insertRealm(defaultModelViewList: List<DefaultModelView>, sourceId: Long): List<DefaultModelView> {
        val realm = Realm.getDefaultInstance()
        var defaultModelLocal: List<DefaultModel>? = null
        var defaultModelViewFinal: List<DefaultModelView>? = null

        val source: SourceDB? = sourceRepository.getSourceByIdRealm(sourceId)

        try {
            realm.executeTransaction { r ->
                defaultModelLocal = DefaultModelFactory.createListOfDefaultModelForRealm(defaultModelViewList, source!!, r)
                r.insertOrUpdate(defaultModelLocal)

                defaultModelViewFinal = DefaultModelFactory.createListOfDefaultModelView(defaultModelLocal, source)
            }

            finishTransaction(realm)
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }

        return defaultModelViewFinal!!
    }

    override fun insertOrUpdateComic(defaultModelView: DefaultModelView, sourceId: Long): Observable<DefaultModelView> {
        return Observable.create<DefaultModelView> { emitter ->
            try {
                emitter.onNext(insertRealm(defaultModelView, sourceId))
                emitter.onComplete()

            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun insertOrUpdateComic(defaultModelViewList: List<DefaultModelView>, sourceId: Long): Observable<List<DefaultModelView>> {
        return Observable.create<List<DefaultModelView>> { emitter ->
            try {
                emitter.onNext(insertRealm(defaultModelViewList, sourceId))
                emitter.onComplete()

            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }

    override fun getAllPublishers(sourceId: Long): Observable<List<DefaultModelView>> {
        return getAllByType(sourceId, DefaultModel.PUBLISHER)
    }

    override fun getAllScanlators(sourceId: Long): Observable<List<DefaultModelView>> {
        return getAllByType(sourceId, DefaultModel.SCANLATOR)
    }

    override fun getAllGenres(sourceId: Long): Observable<List<DefaultModelView>> {
        return getAllByType(sourceId, DefaultModel.GENRE)
    }

    override fun getAllAuthor(sourceId: Long): Observable<List<DefaultModelView>> {
        return getAllByType(sourceId, DefaultModel.AUTHOR)
    }

    private fun getAllByType(sourceId: Long, type: String): Observable<List<DefaultModelView>> {
        return Observable.create<List<DefaultModelView>> { emitter ->
            try {
                val realm = Realm.getDefaultInstance()

                emitter.onNext(findAllDefaultModel(realm, realm.where(DefaultModel::class.java)
                        .equalTo("source.id", sourceId)
                        .equalTo("type", type), sourceId ) )

                emitter.onComplete()
            } catch (ex: Exception) {
                emitter.onError(ex)
            }
        }
    }


    private fun findAllDefaultModel(realm: Realm, realmQuery: RealmQuery<DefaultModel>, sourceId: Long): List<DefaultModelView> {
        try {
            val source: SourceDB? = sourceRepository.getSourceByIdRealm(sourceId)
            val results = realmQuery.findAll()

            var comics: List<DefaultModelView> = emptyList()
            if (results != null) {
                comics = DefaultModelFactory.createListOfDefaultModelView(results.toList(), source)
            }

            finishTransaction(realm)

            return comics
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()

            throw ex
        }
    }

}