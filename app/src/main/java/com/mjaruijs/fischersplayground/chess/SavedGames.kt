package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame

object SavedGames {

    private val games = HashMap<String, MultiPlayerGame>()

    fun put(gameId: String, game: MultiPlayerGame) {
        games[gameId] = game
    }

    fun get(gameId: String) = games[gameId]
//        ?: throw IllegalArgumentException("No saved game was found with id: $gameId")

    fun getAll(): ArrayList<Pair<String, MultiPlayerGame>> {
        val gameData = ArrayList<Pair<String, MultiPlayerGame>>()

        for (entry in games) {
            gameData += Pair(entry.key, entry.value)
        }

        return gameData
    }

    fun delete(gameId: String) {
        games.remove(gameId)
    }

}