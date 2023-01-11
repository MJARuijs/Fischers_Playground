package com.mjaruijs.fischersplayground.activities.parcelable

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.work.Data
import androidx.work.WorkerParameters
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.math.vectors.Vector2

class Worker(context: Context, workerParameters: WorkerParameters) : androidx.work.Worker(context, workerParameters) {
    override fun doWork(): Result {
        val output = getOutput()
        val builder = Data.Builder().putParcelable("output", output)
        return Result.success(builder.build())
    }

    private fun Data.Builder.putParcelable(key: String, parcelable: Parcelable): Data.Builder {
        val parcel = Parcel.obtain()
        try {
            parcelable.writeToParcel(parcel, 0)
            putByteArray(key, parcel.marshall())
        } catch (e: Exception) {
            throw e
        } finally {
            parcel.recycle()
        }
        return this
    }

    private fun getOutput(): Parcelable {
//        val moves = arrayListOf<Move>(
//            Move.fromChessNotation("Pe2-e4"),
//            Move.fromChessNotation("pe7-e5"),
//            Move.fromChessNotation("Nc1-f6"),
//        )

//        val moves = arrayListOf(
//            "hoi",
//            "doei"
//        )
        val moves = ArrayList<MoveArrow>()
//        val moves = Lin(
//            MoveArrow(Vector2(), Vector2()),
//            MoveArrow(Vector2(), Vector2())
//        )
        moves += MoveArrow(Vector2(), Vector2())
        moves += MoveArrow(Vector2(), Vector2())
        val list = HashMap<Int, ArrayList<MoveArrow>>()
        list[0] = moves
        val parcelableArrayList = ParcelableArrayList(list)
        return parcelableArrayList
    }
}