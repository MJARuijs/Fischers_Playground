package com.mjaruijs.fischersplayground.chess.pieces

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus

class MoveData(val gameId: String, val status: GameStatus, val time: Long, val move: Move)