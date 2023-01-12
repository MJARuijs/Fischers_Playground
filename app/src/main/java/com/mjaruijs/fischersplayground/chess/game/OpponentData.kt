package com.mjaruijs.fischersplayground.chess.game

import android.os.Parcel
import android.os.Parcelable

class OpponentData(val opponentName: String, val opponentId: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(opponentName)
        parcel.writeString(opponentId)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (other === this) {
            return true
        }

        if (other !is OpponentData) {
            return false
        }

        if (other.opponentName != opponentName) {
            return false
        }

        if (other.opponentId != opponentId) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = opponentName.hashCode()
        result = 31 * result + opponentId.hashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<OpponentData> {
        override fun createFromParcel(parcel: Parcel): OpponentData {
            return OpponentData(parcel)
        }

        override fun newArray(size: Int): Array<OpponentData?> {
            return arrayOfNulls(size)
        }
    }
}