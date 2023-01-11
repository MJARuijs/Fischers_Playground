package com.mjaruijs.fischersplayground.chess.news

import android.os.Parcel
import android.os.Parcelable

open class News(val newsType: NewsType) : Parcelable {

    constructor(parcel: Parcel) : this(
        NewsType.fromString(parcel.readString()!!)
    )

    override fun toString(): String {
        return "$newsType"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(newsType.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<News> {
        override fun createFromParcel(parcel: Parcel): News {
            return News(parcel)
        }

        override fun newArray(size: Int): Array<News?> {
            return arrayOfNulls(size)
        }

        fun fromString(content: String): News {
            return News(NewsType.fromString(content))
        }
    }

}