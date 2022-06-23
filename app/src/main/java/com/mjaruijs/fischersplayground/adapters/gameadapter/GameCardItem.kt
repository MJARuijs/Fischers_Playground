package com.mjaruijs.fischersplayground.adapters.gameadapter

data class GameCardItem(val id: String, var lastUpdated: Long, val opponentName: String, var gameStatus: GameStatus, var isPlayingWhite: Boolean? = null, var hasUpdate: Boolean) {

    companion object {

        fun fromString(content: String): GameCardItem {
            val data = content.split('|')
            val id = data[0]
            val lastUpdated = data[1].toLong()
            val opponentName = data[2]
            val gameStatus = GameStatus.fromString(data[3])
            val isPlayingWhite = data[4].toBoolean()
            val hasUpdate = data[5].toBoolean()
            return GameCardItem(id, lastUpdated, opponentName, gameStatus, isPlayingWhite, hasUpdate)
        }

    }

}