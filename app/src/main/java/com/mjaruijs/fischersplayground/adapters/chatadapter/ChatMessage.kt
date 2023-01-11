package com.mjaruijs.fischersplayground.adapters.chatadapter

import android.os.Parcel
import android.os.Parcelable

class ChatMessage(val gameId: String, val timeStamp: String, val message: String, val type: MessageType) : Parcelable  {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        MessageType.fromString(parcel.readString()!!)
    )

    override fun toString(): String {
        return "$timeStamp~$message~$type"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(gameId)
        parcel.writeString(timeStamp)
        parcel.writeString(message)
        parcel.writeString(type.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ChatMessage> {
        override fun createFromParcel(parcel: Parcel): ChatMessage {
            return ChatMessage(parcel)
        }

        override fun newArray(size: Int): Array<ChatMessage?> {
            return arrayOfNulls(size)
        }
    }
}