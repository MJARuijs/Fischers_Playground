package com.mjaruijs.fischersplayground.parcelable

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class ParcelableInt(val value: Int) : Parcelable, Serializable {
    constructor(parcel: Parcel) : this(parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableInt> {
        override fun createFromParcel(parcel: Parcel): ParcelableInt {
            return ParcelableInt(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableInt?> {
            return arrayOfNulls(size)
        }
    }
}