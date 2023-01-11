//package com.mjaruijs.fischersplayground.parcelable
//
//import android.os.Parcel
//import android.os.Parcelable
//
//class ParcelableMap<K : Parcelable, V : Parcelable>() : Parcelable {
//
//    private var map = HashMap<K, V>()
//
//    constructor(parcel: Parcel) : this() {
//
//    }
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    fun getMap() = map
//
//    companion object CREATOR : Parcelable.Creator<ParcelableMap<Parcelable, Parcelable>> {
//        override fun createFromParcel(parcel: Parcel): ParcelableMap<Parcelable, Parcelable> {
//            return ParcelableMap(parcel)
//        }
//
//        override fun newArray(size: Int): Array<ParcelableMap<Parcelable, Parcelable>?> {
//            return arrayOfNulls(size)
//        }
//    }
//
//
//}