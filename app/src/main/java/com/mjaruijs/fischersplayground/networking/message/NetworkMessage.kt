package com.mjaruijs.fischersplayground.networking.message

import android.os.Parcel
import android.os.Parcelable

class NetworkMessage(val topic: Topic, val content: String, val id: Long = System.nanoTime()): Parcelable {

    constructor(parcel: Parcel) : this(
        Topic.fromString(parcel.readString()!!),
        parcel.readString()!!,
        parcel.readLong()
    )

    override fun toString() = "[$id;$topic;$content]"

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(content)
        parcel.writeLong(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NetworkMessage> {
        override fun createFromParcel(parcel: Parcel): NetworkMessage {
            return NetworkMessage(parcel)
        }

        override fun newArray(size: Int): Array<NetworkMessage?> {
            return arrayOfNulls(size)
        }

        fun fromString(input: String): NetworkMessage {
            val data = input.removePrefix("[").removeSuffix("]").split(';')
            val id = data[0].toLong()
            val topic = Topic.fromString(data[1])
            val content = data[2]

            return NetworkMessage(topic, content, id)
        }
    }

}