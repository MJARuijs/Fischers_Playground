package com.mjaruijs.fischersplayground.adapters.gameadapter

data class GameCardItem(val id: String, val opponentName: String, var gameStatus: GameStatus, var isPlayingWhite: Boolean? = null)