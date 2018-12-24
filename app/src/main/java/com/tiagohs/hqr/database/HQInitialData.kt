package com.tiagohs.hqr.database

import com.tiagohs.hqr.models.database.CatalogueSource
import com.tiagohs.hqr.models.database.SourceDB
import io.realm.Realm
import io.realm.RealmList

object HQRInitialData {

    fun initialData(realm: Realm?): List<CatalogueSource> {
        return arrayListOf(
                /*CatalogueSource().apply {
                    this.id = 1L
                    this.language = "pt-BR"
                    this.sourceDBS = getPortugueseCatalogue(realm)
                },*/
                CatalogueSource().apply {
                    this.id = 2L
                    this.language = "en"
                    this.sourceDBS = getEnglishCatalogue(realm)
                }/*,
                CatalogueSource().apply {
                    this.id = 3L
                    this.language = "es"
                    this.sourceDBS = getSpanishCatalogue(realm)
                }*/
        )
    }

    private fun getPortugueseCatalogue(realm: Realm?): RealmList<SourceDB> {
        val list = RealmList<SourceDB>()

        list.add(realm!!.createObject(SourceDB::class.java, 1L).apply {
            this.name = "HQBR - Leitor Online de Quadrinhos"
            this.hasThumbnailSupport = false
            this.baseUrl = "https://hqbr.com.br/"
            this.language = "BR"
            this.hasInAllPageSupport = false
            this.hasInGenresPageSupport = false
            this.hasInPublisherPageSupport = false
        })
/*
        list.add(realm.createObject(SourceDB::class.java, 2L).apply {
            this.name = "HQ Ultimate"
            this.hasPageSupport= true
            this.hasThumbnailSupport = true
            this.baseUrl = "http://hqultimate.site/"
            this.language = "pt-BR"
        })*/

        return list
    }

    private fun getEnglishCatalogue(realm: Realm?): RealmList<SourceDB> {
        val list = RealmList<SourceDB>()

        if (realm != null) {

            list.add(realm.createObject(SourceDB::class.java, 4L).apply {
                this.name = "Read Comics Book Online"
                this.hasThumbnailSupport = false
                this.baseUrl = "https://readcomicbooksonline.org/"
                this.language = "en"
                this.hasInAllPageSupport = false
                this.hasInGenresPageSupport = true
                this.hasInPublisherPageSupport = true
            })
/*
            list.add(realm.createObject(SourceDB::class.java, 3L).apply {
                this.name = "Read Comic Online"
                this.hasPageSupport= true
                this.hasThumbnailSupport = false
                this.baseUrl = "http://readcomiconline.to/"
                this.language = "en"
            })

            list.add(realm.createObject(SourceDB::class.java, 5L).apply {
                this.name = "Comics Online"
                this.hasPageSupport= true
                this.hasThumbnailSupport = true
                this.baseUrl = "http://comiconline.me/"
                this.language = "en"
            })*/
        }

        return list
    }

    private fun getSpanishCatalogue(realm: Realm?): RealmList<SourceDB> {
        val list = RealmList<SourceDB>()

        if (realm != null) {
            /*list.add(realm.createObject(SourceDB::class.java, 6L).apply {
                this.name = "Leer DC Comics Online"
                this.hasPageSupport= false
                this.hasThumbnailSupport = false
                this.baseUrl = "https://leerdccomicsonline.blogspot.com/"
                this.language = "es"
            })*/
        }

        return list
    }

}