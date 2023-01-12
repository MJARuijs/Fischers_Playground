package com.mjaruijs.fischersplayground.chess.news

import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable

open class News(val newsType: NewsType, val data: Bundle = Bundle()) : Parcelable {

    constructor(newsType: NewsType, extra: Int) : this(newsType) {
        data.putInt("data", extra)
    }

    constructor(newsType: NewsType, extra: Parcelable) : this(newsType) {
        data.putParcelable("data", extra)
    }

    constructor(parcel: Parcel) : this(
        NewsType.fromString(parcel.readString()!!)
    ) {
        if (newsType.dataType != null) {
            parcel.readBundle(newsType.dataType::class.java.classLoader)
        }
    }

    inline fun <reified T : Any> getData(): T {
        if (T::class.java == Int::class) {
            return data.getInt("data") as T
        }
        if (T::class.java == Parcelable::class) {
            return if (Build.VERSION.SDK_INT < TIRAMISU) {
                @Suppress("DEPRECATION")
                data.getParcelable<Parcelable>("data")!! as T
            } else {
                data.getParcelable("data", Parcelable::class.java) as T
            }
        }
        throw IllegalArgumentException("Could not find extra data of type ${T::class.java} in news..")
    }

    override fun toString(): String {
        var content = "$newsType"

        if (newsType.dataType == null) {
            return content
        }
        if (newsType.dataType == Int) {

        }
        return content
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(newsType.toString())
        parcel.writeBundle(data)
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