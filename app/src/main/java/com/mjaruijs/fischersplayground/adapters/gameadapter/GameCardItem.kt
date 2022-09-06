package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.os.Parcel
import android.os.Parcelable

data class GameCardItem(val id: String, var lastUpdated: Long, val opponentName: String, var gameStatus: GameStatus, var isPlayingWhite: Boolean? = null, var hasUpdate: Boolean) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readString()!!,
        GameStatus.fromString(parcel.readString()!!),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeLong(lastUpdated)
        parcel.writeString(opponentName)
        parcel.writeString(gameStatus.toString())
        parcel.writeValue(isPlayingWhite)
        parcel.writeByte(if (hasUpdate) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GameCardItem> {
        override fun createFromParcel(parcel: Parcel): GameCardItem {
            return GameCardItem(parcel)
        }

        override fun newArray(size: Int): Array<GameCardItem?> {
            return arrayOfNulls(size)
        }

        fun fromString(content: String): GameCardItem {
            val data = content.split('|')
            val id = data[0]
            val lastUpdated = data[1].toLong()
            val opponentName = data[2]
            val gameStatus = GameStatus.fromString(data[3])
            val isPlayingWhite = data[4].toBoolean()
            val hasUpdate = data[5].toBoolean()
            return GameCardItem(id, lastUpdated, opponentName, gameStatus, isPlayingWhite, hasUpdate)
        }
    }

}