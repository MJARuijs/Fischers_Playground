package com.mjaruijs.fischersplayground.adapters.openingmovesadapter

import com.mjaruijs.fischersplayground.chess.game.Move

class OpeningMove(val move: Move) {

    fun getSimpleNotation() = move.getSimpleChessNotation()

}