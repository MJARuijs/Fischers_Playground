package com.mjaruijs.fischersplayground.adapters.openingadapter

import com.mjaruijs.fischersplayground.chess.pieces.Move

class OpeningLine(val startingState: String, val setupMoves: ArrayList<Move>, val lineMoves: ArrayList<Move>) {

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

    override fun toString(): String {
//        var content = "$startingState|"

        var content = ""

        for ((i, move) in setupMoves.withIndex()) {
            content += move.toChessNotation()

            if (i != setupMoves.size - 1) {
                content += ","
            }
        }

        content += "|"

        for ((i, move) in lineMoves.withIndex()) {
            content += move.toChessNotation()

            if (i != lineMoves.size - 1) {
                content += ","
            }
        }

        return content
    }

    companion object {

        fun fromString(content: String): OpeningLine {
            val firstSeparatorIndex = content.indexOf("|")
//            val secondSeparatorIndex = content.indexOf("|", firstSeparatorIndex + 1)
//            val thirdSeparatorIndex = content.indexOf("|", secondSeparatorIndex + 1)

//            val gameStateString = content.substring(0, secondSeparatorIndex)
            val gameStateString = ""
//            val startingMovesString = content.substring(secondSeparatorIndex + 1, thirdSeparatorIndex)
//            val movesString = content.substring(thirdSeparatorIndex + 1)

            val startingMovesString = content.substring(0, firstSeparatorIndex)
            val movesString = content.substring(firstSeparatorIndex + 1)

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

            return OpeningLine(gameStateString, startingMoves, moves)
        }

    }

}