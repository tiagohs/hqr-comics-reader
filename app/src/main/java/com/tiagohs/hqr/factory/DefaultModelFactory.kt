package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.DefaultModelView
import io.realm.Realm
import io.realm.RealmList

object DefaultModelFactory {

    fun createDefaultModelForRealm(defaultModel: DefaultModelView, sourceDB: SourceDB?, realm: Realm): DefaultModel {
        return DefaultModel().create().apply {

            if (defaultModel.name != null) {
                this.name = defaultModel.name
            }

            if (defaultModel.pathLink != null) {
                this.pathLink = defaultModel.pathLink
            }

            if (defaultModel.type != null) {
                this.type = defaultModel.type
            }

            if (sourceDB != null) {
                this.source = realm.copyToRealmOrUpdate(sourceDB)
            }
        }
    }

    fun copyFromDefaultModelView(defaultModel: DefaultModel, other: DefaultModelView, sourceDB: SourceDB?, realm: Realm): DefaultModel {

        if (other.name != null) {
            defaultModel.name = other.name
        }

        if (other.pathLink != null) {
            defaultModel.pathLink = defaultModel.pathLink
        }

        if (other.type != null) {
            defaultModel.type = other.type
        }

        if (sourceDB != null) {
            defaultModel.source = realm.copyToRealmOrUpdate(sourceDB)
        }

        return defaultModel
    }

    fun createListOfDefaultModelForRealm(list: List<DefaultModelView>?, sourceDB: SourceDB?, realm: Realm): RealmList<DefaultModel> {
        val realmList = RealmList<DefaultModel>()
        var id = RealmUtils.getDataId<DefaultModel>()

        list?.forEach {
            val defaultModel = createDefaultModelForRealm(it, sourceDB, realm)
            defaultModel.id = id
            it.id = id

            id++
            realmList.add(realm.copyToRealmOrUpdate(defaultModel) )
        }

        return realmList
    }

    fun createListOfDefaultModelView(listDb: List<DefaultModel>?, sourceDB: SourceDB?): List<DefaultModelView> {
        val defaultModelViewList = ArrayList<DefaultModelView>()

        if (listDb != null) {
            defaultModelViewList.addAll(listDb.map { DefaultModelView().create(it, sourceDB) })
        }

        return defaultModelViewList
    }

    fun createListOfTags(tags: RealmList<String>?): List<String> {
        val list = ArrayList<String>()
        if (tags != null) list.addAll(tags)

        return list
    }

}