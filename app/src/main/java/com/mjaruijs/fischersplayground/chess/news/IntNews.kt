//package com.mjaruijs.fischersplayground.chess.news
//
//import android.os.Parcel
//import android.os.Parcelable
//
//class IntNews(newsType: NewsType, val data: Int) : News(newsType), Parcelable {
//
//    constructor(parcel: Parcel) : this(
//        NewsType.fromString(parcel.readString()!!),
//        parcel.readInt()
//    )
//
//    override fun toString(): String {
//        var content = "$newsType"
//
//        if (data != -1) {
//            content += "$SEPARATOR$data"
//        }
//        return content
//    }
//
//    override fun writeToParcel(parcel: Parcel, flags: Int) {
//        super.writeToParcel(parcel, flags)
//        parcel.writeInt(data)
//    }
//
//    override fun describeContents(): Int {
//        return 0
//    }
//
//    companion object CREATOR : Parcelable.Creator<IntNews> {
//
//        private const val SEPARATOR = '~'
//
//        fun fromString(content: String): IntNews {
//            val separatorIndex = content.indexOf(SEPARATOR)
//
//            if (separatorIndex == -1) {
//                return IntNews(NewsType.fromString(content), -1)
//            }
//
//            val typeData = content.substring(0, separatorIndex)
//            val extraData = content.substring(separatorIndex + 1)
//
//            return IntNews(NewsType.fromString(typeData), extraData.toInt())
//        }
//
//        override fun createFromParcel(parcel: Parcel): IntNews {
//            return IntNews(parcel)
//        }
//
//        override fun newArray(size: Int): Array<IntNews?> {
//            return arrayOfNulls(size)
//        }
//    }
//
//}