package com.tiagohs.hqr.models.view_models

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IDefaultModel
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.SourceDB
import com.tiagohs.hqr.models.database.comics.Comic

class DefaultModelView() : Parcelable {

    var id: Long = -1L
    var name: String? = ""
    var pathLink: String? = ""
    var type: String? = ""

    var source: SourceDB? = null

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        pathLink = parcel.readString()
        type = parcel.readString()
    }

    fun create(other: IDefaultModel, sourceDb: SourceDB?): DefaultModelView {
        val d = DefaultModelView()
        d.copyFrom(other, sourceDb)

        return d
    }

    fun create(other: DefaultModelView, sourceDb: SourceDB?): DefaultModelView {
        val d = DefaultModelView()
        d.copyFrom(other, sourceDb)

        return d
    }

    fun copyFrom(other: IDefaultModel, sourceDb: SourceDB?) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

        if (other.type != null) {
            this.type = other.type
        }

        if (sourceDb != null) {
            this.source = sourceDb
        }
    }

    fun copyFrom(other: DefaultModelView, sourceDb: SourceDB?) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }

        if (other.type != null) {
            this.type = other.type
        }

        if (sourceDb != null) {
            this.source = sourceDb
        }
    }

    fun create(): DefaultModel {
        return DefaultModel().apply {
            this.id = RealmUtils.getDataId<Comic>()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeString(pathLink)
        parcel.writeString(type)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DefaultModelView> {
        override fun createFromParcel(parcel: Parcel): DefaultModelView {
            return DefaultModelView(parcel)
        }

        override fun newArray(size: Int): Array<DefaultModelView?> {
            return arrayOfNulls(size)
        }
    }


}