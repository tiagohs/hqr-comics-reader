package com.tiagohs.hqr.factory

import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.database.DefaultModel
import io.realm.Realm
import io.realm.RealmList

object DefaultModelFactory {

    fun createDefaultModelForRealm(defaultModel: DefaultModel, realm: Realm): DefaultModel {
        val realmObject: DefaultModel?

        realmObject = realm.createObject(DefaultModel::class.java, RealmUtils.getDataId<DefaultModel>(realm))
        realmObject.copyFrom(defaultModel)

        return realmObject
    }

    fun createListOfDefaultModelForRealm(list: List<DefaultModel>?, realm: Realm): RealmList<DefaultModel> {
        val realmList = RealmList<DefaultModel>()
        list?.forEach { realmList.add(createDefaultModelForRealm(it, realm)) }

        return realmList
    }

    fun createListOfTags(tags: RealmList<String>?, realm: Realm): List<String> {
        val list = ArrayList<String>()
        if (tags != null) list.addAll(tags)

        return list
    }

}