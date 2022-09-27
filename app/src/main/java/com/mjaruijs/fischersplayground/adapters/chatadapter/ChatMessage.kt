package com.mjaruijs.fischersplayground.adapters.chatadapter

import android.os.Parcel
import android.os.Parcelable

class ChatMessage(val timeStamp: String, val message: String, val type: MessageType) {

    constructor(data: Data) : this(data.timeStamp, data.message, data.type)

    class Data(val gameId: String, val timeStamp: String, val message: String, val type: MessageType) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            MessageType.fromString(parcel.readString()!!)
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(gameId)
            parcel.writeString(timeStamp)
            parcel.writeString(message)
            parcel.writeString(type.toString())
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Data> {
            override fun createFromParcel(parcel: Parcel): Data {
                return Data(parcel)
            }

            override fun newArray(size: Int): Array<Data?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun toString(): String {
        return "$timeStamp~$message~$type"
    }

}