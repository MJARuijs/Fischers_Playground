package com.mjaruijs.fischersplayground.chess.game

import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus

class MoveData(val gameId: String, val status: GameStatus, val time: Long, val move: Move) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        GameStatus.fromString(parcel.readString()!!),
        parcel.readLong(),
        Move.fromChessNotation(parcel.readString()!!)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(gameId)
        parcel.writeString(status.toString())
        parcel.writeLong(time)
        parcel.writeString(move.toChessNotation())
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "$gameId,$status,$time,${move.toChessNotation()}"
    }

    companion object CREATOR : Parcelable.Creator<MoveData> {
        override fun createFromParcel(parcel: Parcel): MoveData {
            return MoveData(parcel)
        }

        override fun newArray(size: Int): Array<MoveData?> {
            return arrayOfNulls(size)
        }

        fun fromString(content: String): MoveData {
            val data = content.split(',')
            val gameId = data[0]
            val status = GameStatus.fromString(data[1])
            val time = data[2].toLong()
            val move = Move.fromChessNotation(data[3])
            return MoveData(gameId, status, time, move)
        }
    }
}