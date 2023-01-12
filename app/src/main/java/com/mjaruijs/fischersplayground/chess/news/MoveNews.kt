//package com.mjaruijs.fischersplayground.chess.news
//
//import android.os.Parcel
//import android.os.Parcelable
//import com.mjaruijs.fischersplayground.chess.game.MoveData
//
//class MoveNews(newsType: NewsType, val moveData: MoveData) : News(newsType), Parcelable {
//
//    constructor(parcel: Parcel) : this(
//        NewsType.fromString(parcel.readString()!!),
//        parcel.readParcelable(MoveData::class.java.classLoader)!!
//    )
//
//    override fun toString(): String {
//        return "$newsType,$moveData"
//    }
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//        super.writeToParcel(parcel, flags)
//        parcel.writeParcelable(moveData, flags)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR : Parcelable.Creator<MoveNews> {
//        override fun createFromParcel(parcel: Parcel): MoveNews {
//            return MoveNews(parcel)
//        }
//
//        override fun newArray(size: Int): Array<MoveNews?> {
//            return arrayOfNulls(size)
//        }
//
//        fun fromString(content: String): MoveNews {
//            val separatorIndex = content.indexOf('~')
//
//            val newsType = NewsType.fromString(content.substring(0, separatorIndex))
//            val moveData = MoveData.fromString(content.substring(separatorIndex + 1))
//
//            return MoveNews(newsType, moveData)
//        }
//    }
//
//}