package com.tiagohs.hqr.helpers.tools

import io.realm.Realm
import io.realm.RealmObject

class RealmUtils {

    companion object {

        inline fun <reified T: RealmObject> getDataId(): Long {
            val realm = Realm.getDefaultInstance()
            var nextId = 1L

            if (realm.where(T::class.java).max("id") != null) {
                nextId = realm.where(T::class.java).max("id")!!.toLong() + 1
            }

            realm.close()

            return nextId
        }

    }
}