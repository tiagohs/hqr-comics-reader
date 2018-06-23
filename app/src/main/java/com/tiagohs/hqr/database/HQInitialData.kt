package com.tiagohs.hqr.database

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.Source
import io.realm.Realm
import io.realm.RealmList

object HQRInitialData {

    fun initialData(realm: Realm?): List<CatalogueSource> {
        return arrayListOf(
                CatalogueSource().apply {
                    this.id = 1L
                    this.language = "PT-BR"
                    this.sources = getPortugueseCatalogue(realm)
                },
                CatalogueSource().apply {
                    this.id = 2L
                    this.language = "EN"
                    this.sources = getEnglishCatalogue(realm)
                },
                CatalogueSource().apply {
                    this.id = 3L
                    this.language = "ES"
                    this.sources = getSpanishCatalogue(realm)
                }
        )
    }

    private fun getPortugueseCatalogue(realm: Realm?): RealmList<Source> {
        val list = RealmList<Source>()

        list.add(realm!!.createObject(Source::class.java, 1L).apply {
            this.name = "HQBR - Leitor Online de Quadrinhos"
            this.hasPageSupport= false
            this.hasThumbnailSupport = false
            this.baseUrl = "https://hqbr.com.br/"
            this.language = "PT-BR"
        })

        list.add(realm.createObject(Source::class.java, 2L).apply {
            this.name = "HQ Ultimate"
            this.hasPageSupport= true
            this.hasThumbnailSupport = true
            this.baseUrl = "http://hqultimate.site/"
            this.language = "PT-BR"
        })

        return list
    }

    private fun getEnglishCatalogue(realm: Realm?): RealmList<Source> {
        val list = RealmList<Source>()

        if (realm != null) {
            list.add(realm.createObject(Source::class.java, 3L).apply {
                this.name = "Read Comic Online"
                this.hasPageSupport= true
                this.hasThumbnailSupport = false
                this.baseUrl = "http://readcomiconline.to/"
                this.language = "EN"
            })

            list.add(realm.createObject(Source::class.java, 4L).apply {
                this.name = "Read Comics Book Online"
                this.hasPageSupport= false
                this.hasThumbnailSupport = false
                this.baseUrl = "https://readcomicbooksonline.org/"
                this.language = "EN"
            })

            list.add(realm.createObject(Source::class.java, 5L).apply {
                this.name = "Comics Online"
                this.hasPageSupport= true
                this.hasThumbnailSupport = true
                this.baseUrl = "http://comiconline.me/"
                this.language = "EN"
            })
        }

        return list
    }

    private fun getSpanishCatalogue(realm: Realm?): RealmList<Source> {
        val list = RealmList<Source>()

        if (realm != null) {
            list.add(realm.createObject(Source::class.java, 6L).apply {
                this.name = "Leer DC Comics Online"
                this.hasPageSupport= false
                this.hasThumbnailSupport = false
                this.baseUrl = "https://leerdccomicsonline.blogspot.com/"
                this.language = "ES"
            })
        }

        return list
    }

}