package com.tiagohs.hqr.models.view_models

import android.os.Parcel
import android.os.Parcelable
import com.tiagohs.hqr.helpers.tools.RealmUtils
import com.tiagohs.hqr.models.base.IDefaultModel
import com.tiagohs.hqr.models.database.DefaultModel
import com.tiagohs.hqr.models.database.comics.Comic

class DefaultModelView() : Parcelable {

    var id: Long = -1L
    var name: String? = ""
    var pathLink: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        pathLink = parcel.readString()
    }

    fun create(other: IDefaultModel): DefaultModelView {
        val d = DefaultModelView()
        d.copyFrom(other)

        return d
    }

    fun create(other: DefaultModelView): DefaultModelView {
        val d = DefaultModelView()
        d.copyFrom(other)

        return d
    }

    fun copyFrom(other: IDefaultModel) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
        }
    }

    fun copyFrom(other: DefaultModelView) {

        if (other.id != -1L) {
            this.id = other.id
        }

        if (other.name != null) {
            this.name = other.name
        }

        if (other.pathLink != null) {
            this.pathLink = other.pathLink
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