package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

class InviteData(val inviteId: String, val opponentName: String, val timeStamp: Long, val type: InviteType) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readLong(),
        InviteType.fromString(parcel.readString()!!)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(inviteId)
        parcel.writeString(opponentName)
        parcel.writeLong(timeStamp)
        parcel.writeString(type.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InviteData> {
        override fun createFromParcel(parcel: Parcel): InviteData {
            return InviteData(parcel)
        }

        override fun newArray(size: Int): Array<InviteData?> {
            return arrayOfNulls(size)
        }
    }
}