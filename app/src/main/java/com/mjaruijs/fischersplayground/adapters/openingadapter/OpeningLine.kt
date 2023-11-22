package com.mjaruijs.fischersplayground.adapters.openingadapter

import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.math.vectors.Vector2

class OpeningLine(val setupMoves: ArrayList<Move> = arrayListOf(), val lineMoves: ArrayList<Move> = arrayListOf(), val arrows: HashMap<Int, ArrayList<MoveArrow>> = HashMap()) : Parcelable {

    constructor(parcel: Parcel) : this() {
        if (android.os.Build.VERSION.SDK_INT < TIRAMISU) {
            parcel.readList(setupMoves, Move::class.java.classLoader)
            parcel.readList(lineMoves, Move::class.java.classLoader)
            parcel.readMap(arrows, MoveArrow::class.java.classLoader)
        } else {
            parcel.readList(setupMoves, Move::class.java.classLoader, Move::class.java)
            parcel.readList(lineMoves, Move::class.java.classLoader, Move::class.java)
            parcel.readMap(arrows, MoveArrow::class.java.classLoader, Int::class.java, ArrayList<MoveArrow>()::class.java)
        }
    }

    fun getAllMoves(): ArrayList<Move> {
        val moves = ArrayList<Move>()

        for (move in setupMoves) {
            moves += move
        }

        for (move in lineMoves) {
            moves += move
        }

        return moves
    }

    fun clearMoves() {
        setupMoves.clear()
        lineMoves.clear()
    }

    override fun toString(): String {
        var content = ""

        if (setupMoves.isEmpty()) return ""

        for ((i, move) in setupMoves.withIndex()) {
            content += move.toChessNotation()

            if (i != setupMoves.size - 1) {
                content += ","
            }
        }

        content += "~"

        for ((i, move) in lineMoves.withIndex()) {
            content += move.toChessNotation()

            if (i != lineMoves.size - 1) {
                content += ","
            }
        }

        if (arrows.isNotEmpty()) {
            content += "~"

            // 1[x y x y,x y x y,x y x y],5[<x, y> <x, y>,<x, y> <x, y>]

            for ((i, entry) in arrows.entries.withIndex()) {
//                content += arrow.toString()
                if (entry.value.isEmpty()) {
                    continue
                }

                var arrowContent = "${entry.key}["

                for ((j, arrow) in entry.value.withIndex()) {
                    arrowContent += "${arrow.startSquare.x} ${arrow.startSquare.y} ${arrow.endSquare.x} ${arrow.endSquare.y}"

                    if (j != entry.value.size - 1) {
                        arrowContent += ","
                    }
                }

                arrowContent += "]"

                if (i != arrows.size - 1) {
                    arrowContent += ","
                }
                content += arrowContent
            }
        }

        return content
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (this === other) {
            return true
        }

        if (other !is OpeningLine) {
            return false
        }

        if (setupMoves.size != other.setupMoves.size) {
            return false
        }

        if (lineMoves.size != other.lineMoves.size) {
            return false
        }

        for (i in 0 until setupMoves.size) {
            if (setupMoves[i] != other.setupMoves[i]) {
                return false
            }
        }

        for (i in 0 until lineMoves.size) {
            if (lineMoves[i] != other.lineMoves[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = setupMoves.hashCode()
        result = 31 * result + lineMoves.hashCode()
        result = 31 * result + arrows.hashCode()
        return result
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OpeningLine> {

        private const val TAG = "OpeningLine"

        fun fromString(content: String): OpeningLine {
            try {
                val firstSeparatorIndex = content.indexOf("~")

                if (firstSeparatorIndex == -1) {
                    val startingMovesData = content.split(",")
                    val startingMoves = ArrayList<Move>()

                    startingMovesData.forEach {
                        if (it.isNotBlank()) {
                            startingMoves += Move.fromChessNotation(it)
                        }
                    }

                    return OpeningLine(startingMoves, ArrayList())
                } else {
                    val startingMovesString = content.substring(0, firstSeparatorIndex)
                    val secondSeparatorIndex = content.indexOf("~", firstSeparatorIndex + 1)

                    val movesString = if (secondSeparatorIndex == -1) {
                        content.substring(firstSeparatorIndex + 1)
                    } else {
                        content.substring(firstSeparatorIndex + 1, secondSeparatorIndex)
                    }

                    val startingMovesData = startingMovesString.split(",")
                    val startingMoves = ArrayList<Move>()

                    startingMovesData.forEach {
                        if (it.isNotBlank()) {
                            startingMoves += Move.fromChessNotation(it)
                        }
                    }

                    val movesData = movesString.split(",")
                    val moves = ArrayList<Move>()

                    movesData.forEach {
                        if (it.isNotBlank()) {
                            moves += Move.fromChessNotation(it)
                        }
                    }

                    val arrowMap = HashMap<Int, ArrayList<MoveArrow>>()

                    if (secondSeparatorIndex != -1) {
                        val arrowString = content.substring(secondSeparatorIndex + 1)
                        var currentIndex = 0
                        while (true) {
                            val listEndIndex = arrowString.indexOf("]", currentIndex + 1)
                            if (listEndIndex == -1) {
                                break
                            }

                            val arrowData = arrowString.substring(currentIndex, listEndIndex)
                            val listStartIndex = arrowData.indexOf("[")
                            val moveIndex = arrowData.substring(0, listStartIndex).toInt()
                            val arrowCoordinates = arrowData.substring(listStartIndex + 1).split(",")
                            val moveArrows = ArrayList<MoveArrow>()
                            for (arrowCoordinate in arrowCoordinates) {
                                val floats = arrowCoordinate.split(" ").map { string -> string.toFloat() }
                                val startX = floats[0]
                                val startY = floats[1]
                                val endX = floats[2]
                                val endY = floats[3]
                                val moveArrow = MoveArrow(Vector2(startX, startY), Vector2(endX, endY))
                                if (moveArrow.isValidArrow()) {
                                    moveArrows += moveArrow
                                }
                            }

                            arrowMap[moveIndex] = moveArrows
                            currentIndex = listEndIndex + 2
                        }
                    }

                    return OpeningLine(startingMoves, moves, arrowMap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalArgumentException("Failed to parse text into openingLine: $content")
            }

        }

        override fun createFromParcel(parcel: Parcel): OpeningLine {
            return OpeningLine(parcel)
        }

        override fun newArray(size: Int): Array<OpeningLine?> {
            return arrayOfNulls(size)
        }
    }

}