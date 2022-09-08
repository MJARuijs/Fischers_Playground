package com.mjaruijs.fischersplayground.parcelable

import android.os.Parcel
import android.os.Parcelable

class ParcelablePair<F : Parcelable, S : Parcelable>() : Parcelable {

    lateinit var first: F
    lateinit var second: S

    constructor(first: F, second: S) : this() {
        this.first = first
        this.second = second
    }

    constructor(parcel: Parcel) : this() {
        val firstType = parcel.readSerializable() as Class<*>
        first = parcel.readParcelable(firstType.classLoader)!!

        val secondType = parcel.readSerializable() as Class<*>
        second = parcel.readParcelable(secondType.classLoader)!!
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(first.javaClass)
        parcel.writeParcelable(first, 0)
        parcel.writeSerializable(second.javaClass)
        parcel.writeParcelable(second, 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelablePair<Parcelable, Parcelable>> {
        override fun createFromParcel(parcel: Parcel): ParcelablePair<Parcelable, Parcelable> {
            return ParcelablePair(parcel)
        }

        override fun newArray(size: Int): Array<ParcelablePair<Parcelable, Parcelable>?> {
            return arrayOfNulls(size)
        }
    }
}