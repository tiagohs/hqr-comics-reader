package com.tiagohs.hqr.database.repository

import com.tiagohs.hqr.database.ISourceRepository
import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.SourceDB
import io.reactivex.Observable
import io.realm.Realm


class SourceRepository() : BaseRepository(), ISourceRepository {

    override fun insertSource(sourceDB: SourceDB): Observable<SourceDB> {
        return insert(sourceDB)
    }

    override fun getAllSources(): Observable<List<SourceDB>?> {
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(SourceDB::class.java)
                            .findAll()
                    val sources = SourceDB().createList(results.toList())

                    finishTransaction(realm)

                    sources
                }
    }

    override fun getAllCatalogueSources(): Observable<List<CatalogueSource>?> {
        return startGetTransaction()
                .map { realm ->
                    val results = realm.where(CatalogueSource::class.java).findAll()
                    val catalogueSources = CatalogueSource().createList(results.toList())

                    finishTransaction(realm)

                    catalogueSources
                }
    }

    override fun getCatalogueSourceById(catalogueSourceId: Long): Observable<CatalogueSource?> {
        return startGetTransaction()
                .map { realm ->
                    val result = realm.where(CatalogueSource::class.java)
                                .equalTo("id", catalogueSourceId)
                                .findFirst()
                    var catalogueSource: CatalogueSource? = null

                    if (result != null) {
                        catalogueSource = CatalogueSource().create(result)
                    }

                    finishTransaction(realm)

                    catalogueSource
                }
    }

    override fun getSourceByIdRealm(sourceId: Long): SourceDB? {
        val realm = Realm.getDefaultInstance()

        try {
            val result = realm.where(SourceDB::class.java)
                    .equalTo("id", sourceId)
                    .findFirst()
            var source: SourceDB? = null

            if (result != null) {
                source = SourceDB().create(result)
            }

            finishTransaction(realm)

            return source
        } catch (ex: Exception) {
            if (!realm.isClosed)
                realm.close()
        }

        return null
    }

    override fun getSourceById(sourceId: Long): Observable<SourceDB?> {
        return Observable.create<SourceDB> { emitter ->
            val realm = Realm.getDefaultInstance()

            try {
                val result = realm.where(SourceDB::class.java)
                        .equalTo("id", sourceId)
                        .findFirst()
                var source: SourceDB? = null

                if (result != null) {
                    source = SourceDB().create(result)
                    emitter.onNext(source)
                }

                finishTransaction(realm)

                emitter.onComplete()
            } catch (ex: Exception) {
                if (!realm.isClosed)
                    realm.close()

                emitter.onError(ex)
            }
        }
    }

}