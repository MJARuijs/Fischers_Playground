package com.mjaruijs.fischersplayground.activities.parcelable

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import java.util.LinkedList

class ParcelableArrayList(val list: HashMap<Int, ArrayList<MoveArrow>> = HashMap()) : Parcelable {

    constructor(parcel: Parcel) : this() {
        parcel.readMap(list, MoveArrow::class.java.classLoader)
//        val setupMoves = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//            parcel.readArrayList(Move::class.java.classLoader) ?: ArrayList<Move>()
//        } else {
//            parcel.readArrayList(Move::class.java.classLoader, Opening::class.java) ?: ArrayList<Move>()
//        }
//
//        for (move in setupMoves) {
//            list.add(move as Move)
//        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeMap(list)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableArrayList> {
        override fun createFromParcel(parcel: Parcel): ParcelableArrayList {
            return ParcelableArrayList(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableArrayList?> {
            return arrayOfNulls(size)
        }
    }


}