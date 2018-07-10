package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.view_models.DefaultModelView
import io.realm.Realm
import io.realm.RealmList

object DefaultModelFactory {

    fun createDefaultModelForRealm(defaultModel: DefaultModelView, sourceDB: SourceDB?, realm: Realm): DefaultModel {
        val realmObject = realm.createObject(DefaultModel::class.java, RealmUtils.getDataId<DefaultModel>(realm))

        realmObject.apply {
            copyFromDefaultModelView(this, defaultModel, sourceDB, realm)
        }

        return realmObject
    }

    fun copyFromDefaultModelView(defaultModel: DefaultModel, other: DefaultModelView, sourceDB: SourceDB?, realm: Realm): DefaultModel {

        if (other.name != null) {
            defaultModel.name = other.name
        }

        if (other.pathLink != null) {
            defaultModel.pathLink = other.pathLink
        }

        if (other.type != null) {
            defaultModel.type = other.type
        }

        if (sourceDB != null) {
            defaultModel.source = realm.copyToRealmOrUpdate(sourceDB)
        }

        return defaultModel
    }

    fun createListOfDefaultModelForRealm(defaultModelList: List<DefaultModelView>?, source: SourceDB?, realm: Realm): RealmList<DefaultModel> {
        val list = RealmList<DefaultModel>()

        defaultModelList?.forEach {
            val defaultModelLocal = realm.where(DefaultModel::class.java)
                    .equalTo("pathLink", it.pathLink)
                    .findFirst()

            if (defaultModelLocal == null) {
                list.add( createDefaultModelForRealm(it, source, realm) )
            } else {
                list.add( realm.copyToRealmOrUpdate(defaultModelLocal) )
            }
        }

        return list
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