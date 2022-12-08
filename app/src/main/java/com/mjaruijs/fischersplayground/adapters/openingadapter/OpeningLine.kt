package com.mjaruijs.fischersplayground.adapters.openingadapter

import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.util.Logger

class OpeningLine(val setupMoves: ArrayList<Move>, val lineMoves: ArrayList<Move>, val arrows: HashMap<Int, ArrayList<MoveArrow>> = HashMap()) {

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
            val chessNotation = move.toChessNotation()
            Logger.debug(TAG, "Adding $chessNotation to string")
            content += move.toChessNotation()

            if (i != lineMoves.size - 1) {
                content += ","
            }
        }

        if (arrows.isNotEmpty()) {
            content += "~"

            // 1[<x, y> <x, y>,<x, y> <x, y>,<x, y> <x, y>],5[<x, y> <x, y>,<x, y> <x, y>]

            for ((i, entry) in arrows.entries.withIndex()) {
//                content += arrow.toString()
                var arrowContent = "${entry.key}["

                for (arrow in entry.value) {
                    arrowContent += "${arrow.startSquare} ${arrow.endSquare}"
                }

                arrowContent += "]"

                if (i != arrows.size - 1) {
                    content += ","
                }
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

    companion object {

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

                    if (secondSeparatorIndex != -1) {
                        var arrowString = content.substring(secondSeparatorIndex + 1)
                        var currentIndex = 0
                        while (true) {
                            val listEndIndex = arrowString.indexOf("]", currentIndex + 1)
                            if (listEndIndex == -1) {
                                break
                            }

                            val arrowData = arrowString.substring(currentIndex, listEndIndex)
                            Logger.debug(TAG, "Got arrow data: $arrowData")



                            currentIndex = listEndIndex
                        }

                    }

                    return OpeningLine(startingMoves, moves)
                }

            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse text into openingLine: $content")
            }

        }

    }

}