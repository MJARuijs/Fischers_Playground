package com.mjaruijs.fischersplayground.chess.game

import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.FloatUtils

class MoveArrow(val startSquare: Vector2, val endSquare: Vector2) : Parcelable {

    constructor(parcel: Parcel) : this(
        Vector2.fromString(parcel.readString()!!),
        Vector2.fromString(parcel.readString()!!)
    )

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (other !is MoveArrow) {
            return false
        }
        if (!FloatUtils.compare(startSquare, other.startSquare)) {
            return false
        }
        if (!FloatUtils.compare(endSquare, other.endSquare)) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return "[$startSquare;$endSquare]"
    }

    override fun hashCode(): Int {
        var result = startSquare.hashCode()
        result = 31 * result + endSquare.hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(startSquare.toString())
        parcel.writeString(endSquare.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MoveArrow> {
        override fun createFromParcel(parcel: Parcel): MoveArrow {
            return MoveArrow(parcel)
        }

        override fun newArray(size: Int): Array<MoveArrow?> {
            return arrayOfNulls(size)
        }
    }

}