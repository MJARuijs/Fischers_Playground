package com.mjaruijs.fischersplayground.adapters.openingmovesadapter

import com.mjaruijs.fischersplayground.chess.game.Move

class OpeningLine(val id: Int, val moves: ArrayList<Move> = arrayListOf(), var currentIndex: Int = 0) {

    operator fun get(i: Int): Move {
        return moves[i]
    }

    fun indexOf(move: Move): Int {
        return moves.indexOf(move)
    }

    fun addMove(move: Move) {
        moves.add(move)
        currentIndex++
    }

    fun deleteMoveAt(i: Int) {
        moves.removeAt(i)
    }

    fun totalDepth(): Int {
        return moves.size / 2 + 1
    }

    fun getNumberOfMoves() = moves.size

    fun jumpToMove(move: Move) {
        val moveIndex = moves.indexOf(move)
        currentIndex = moveIndex + 1
    }

}