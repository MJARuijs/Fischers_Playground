package com.mjaruijs.fischersplayground.adapters.gameadapter

data class GameCardItem(val id: String, val lastUpdated: Long, val opponentName: String, var gameStatus: GameStatus, var isPlayingWhite: Boolean? = null, var hasUpdate: Boolean)