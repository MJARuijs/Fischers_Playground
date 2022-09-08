//package com.mjaruijs.fischersplayground.data
//
//import android.os.Parcel
//import android.os.Parcelable
//
//class UndoAcceptedData(val gameId: String, val numberOfReversedMoves: Int) : Parcelable {
//
//    constructor(parcel: Parcel) : this(
//        parcel.readString()!!,
//        parcel.readInt()
//    )
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//        parcel.writeString(gameId)
//        parcel.writeInt(numberOfReversedMoves)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR : Parcelable.Creator<UndoAcceptedData> {
//        override fun createFromParcel(parcel: Parcel): UndoAcceptedData {
//            return UndoAcceptedData(parcel)
//        }
//
//        override fun newArray(size: Int): Array<UndoAcceptedData?> {
//            return arrayOfNulls(size)
//        }
//    }
//}