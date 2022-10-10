package com.mjaruijs.fischersplayground.adapters.openingmovesadapter

import com.mjaruijs.fischersplayground.chess.pieces.Move

class OpeningMove(val move: Move) {

    fun getSimpleNotation() = move.getSimpleChessNotation()

}