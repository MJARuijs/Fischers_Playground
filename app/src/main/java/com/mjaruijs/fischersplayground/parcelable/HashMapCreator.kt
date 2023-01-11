//package com.mjaruijs.fischersplayground.parcelable
//
//import android.os.Build
//import android.os.Parcel
//import android.os.Parcelable
//
//class HashMapCreator<K, V> {
//
//    companion object CREATOR : Parcelable.Creator<HashMap<Any, Any>> {
//
//        inline fun <reified K, reified V> HashMap<K, V>.writeToParcel(parcel: Parcel, flags: Int) {
//            parcel.writeMap(this)
//        }
//
//        override fun createFromParcel(parcel: Parcel): HashMap<Any, Any> {
//            val map = kotlin.collections.HashMap<K, V>()
//
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//                parcel.readMap(map, V::class.java.classLoader)
//            } else {
//                parcel.readMap(map, V::class.java.classLoader, K::class.java, V::class.java)
//            }
//
//            return map        }
//
//        override fun newArray(p0: Int): Array<HashMap<Any, Any>> {
//            TODO("Not yet implemented")
//        }
//
//    }
//
//}