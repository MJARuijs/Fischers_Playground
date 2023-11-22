package com.mjaruijs.fischersplayground.parcelable

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class ParcelableString(val value: String) : Parcelable, Serializable {

    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableString> {
        override fun createFromParcel(parcel: Parcel): ParcelableString {
            return ParcelableString(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableString?> {
            return arrayOfNulls(size)
        }
    }
}