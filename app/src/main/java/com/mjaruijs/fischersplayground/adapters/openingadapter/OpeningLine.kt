package com.mjaruijs.fischersplayground.adapters.openingadapter

import com.mjaruijs.fischersplayground.chess.game.GameState
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.util.Logger

class OpeningLine(val startingState: String, val moves: ArrayList<Move>) {

    override fun toString(): String {
        var content = "${startingState.toString()}|"

        for ((i, move) in moves.withIndex()) {
            content += move.toChessNotation()

            if (i != moves.size - 1) {
                content += ","
            }
        }

        return content
    }

    companion object {

        fun fromString(content: String): OpeningLine {
            val firstSeparatorIndex = content.indexOf("|")
            val secondSeparatorIndex = content.indexOf("|", firstSeparatorIndex + 1)

            val gameStateString = content.substring(0, secondSeparatorIndex)
            val movesString = content.substring(secondSeparatorIndex + 1)

            val movesData = movesString.split(",")
            val moves = ArrayList<Move>()

            movesData.forEach {
                if (it.isNotBlank()) {
                    moves += Move.fromChessNotation(it)
                }
            }

            return OpeningLine(gameStateString, moves)
        }

    }

}