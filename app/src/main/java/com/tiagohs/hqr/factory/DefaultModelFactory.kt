package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.view_models.DefaultModelView
import io.realm.Realm
import io.realm.RealmList

object DefaultModelFactory {

    fun createDefaultModelForRealm(defaultModel: DefaultModelView, realm: Realm): DefaultModel {
        val realmObject: DefaultModel?

        realmObject = realm.createObject(DefaultModel::class.java, RealmUtils.getDataId<DefaultModel>(realm))
        realmObject.copyFrom(defaultModel)

        return realmObject
    }

    fun createListOfDefaultModelForRealm(list: List<DefaultModelView>?, realm: Realm): RealmList<DefaultModel> {
        val realmList = RealmList<DefaultModel>()
        list?.forEach { realmList.add(createDefaultModelForRealm(it, realm)) }

        return realmList
    }

    fun createListOfDefaultModel(list: List<DefaultModel>?): List<DefaultModel> {
        val realmList = ArrayList<DefaultModel>()
        list?.forEach {
            val defaultModel = DefaultModel()
            defaultModel.copyFrom(it)

            realmList.add(defaultModel) }

        return realmList
    }

    fun createListOfTags(tags: RealmList<String>?): List<String> {
        val list = ArrayList<String>()
        if (tags != null) list.addAll(tags)

        return list
    }

}