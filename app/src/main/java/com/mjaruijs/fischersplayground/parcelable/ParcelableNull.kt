package com.mjaruijs.fischersplayground.parcelable

import android.os.Parcel
import android.os.Parcelable

class ParcelableNull() : Parcelable {

    constructor(parcel: Parcel) : this() {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableNull> {
        override fun createFromParcel(parcel: Parcel): ParcelableNull {
            return ParcelableNull(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableNull?> {
            return arrayOfNulls(size)
        }
    }
}