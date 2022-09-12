package com.mjaruijs.fischersplayground.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.util.FileManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap

class DataManager(context: Context) {

    private val savedGames = HashMap<String, MultiPlayerGame>()
    val savedInvites = HashMap<String, InviteData>()
    val recentOpponents = Stack<Pair<String, String>>()

    private val userId: String

    private val lock = AtomicBoolean(false)

    init {
        val preferences = context.getSharedPreferences("user_data", MODE_PRIVATE)
        userId = preferences.getString(ClientActivity.USER_ID_KEY, "")!!

        loadData(context)
    }

    fun getSavedGames(): HashMap<String, MultiPlayerGame> {
        while (isLocked()) {
            Thread.sleep(1)
        }
        return savedGames
    }

    fun removeGame(id: String) {
        while (isLocked()) {
            Thread.sleep(1)
        }
        savedGames.remove(id)
    }

    operator fun set(id: String, game: MultiPlayerGame) {
        while (isLocked()) {
            Thread.sleep(1)
        }
        savedGames[id] = game
    }

    operator fun get(id: String): MultiPlayerGame {
        while (isLocked()) {
            Thread.sleep(1)
        }
        return savedGames[id] ?: throw IllegalArgumentException("No game could be found with id: $id")
    }

    fun isLocked() = lock.get()

    private fun lock() {
        lock.set(true)
    }

    private fun unlock() {
        lock.set(false)
    }

    fun loadData(context: Context) {
        if (isLocked()) {
            println("Data is already loading.. returning..")
            return
        }

        println("LOADING DATA")

        lock()
        Thread {
            try {
                savedGames.clear()
                savedInvites.clear()
                recentOpponents.clear()
                loadSavedGames(context)
                loadInvites(context)
                loadRecentOpponents(context)
            } catch (e: Exception) {
                FileManager.write(context, "file_loading_crash.txt", e.stackTraceToString())
            } finally {
                unlock()
            }

        }.start()

    }

    fun saveData(context: Context) {
        println("SAVING DATA")

        while (isLocked()) {
            Thread.sleep(1)
        }

        lock()
        Thread {
            saveGames(context)
            saveInvites(context)
            saveRecentOpponents(context)
            unlock()
        }.start()
    }

    private fun loadSavedGames(context: Context) {
        val lines = FileManager.read(context, MULTIPLAYER_GAME_FILE) ?: ArrayList()

        for (gameData in lines) {
            if (gameData.isBlank()) {
                continue
            }

            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
            val gameId = data[0]
            val opponentId = data[1]
            val opponentName = data[2]
            val gameStatus = GameStatus.fromString(data[3])
            val opponentStatus = data[4]
            val lastUpdated = data[5].toLong()
            val isPlayerWhite = data[6].toBoolean()
            val moveToBeConfirmed = data[7]
            val moveList = data[8].removePrefix("[").removeSuffix("]").split('\\')
            val chatMessages = data[9].removePrefix("[").removeSuffix("]").split('\\')
            val newsData = data[10].removePrefix("[").removeSuffix("]").split("\\")

//            val winner = data[7]

            val moves = ArrayList<Move>()

            for (move in moveList) {
                if (move.isNotBlank()) {
                    moves += Move.fromChessNotation(move)
                }
            }

            val messages = ArrayList<ChatMessage>()
            for (message in chatMessages) {
                if (message.isNotBlank()) {
                    val messageData = message.split(',')
                    val timeStamp = messageData[0]
                    val messageContent = messageData[1]
                    val type = MessageType.fromString(messageData[2])

                    messages += ChatMessage(timeStamp, messageContent, type)
                }
            }

            val newsUpdates = ArrayList<News>()
            for (news in newsData) {
                if (news.isBlank()) {
                    continue
                }

                newsUpdates += News.fromString(news)
            }

            println("LOADED FROM STORAGE: $gameStatus")

            val newGame = MultiPlayerGame(gameId, opponentId, opponentName, gameStatus, opponentStatus, lastUpdated, isPlayerWhite, moveToBeConfirmed, moves, messages, newsUpdates)
            newGame.status = gameStatus
            savedGames[gameId] = newGame
        }
    }

    private fun loadInvites(context: Context) {
        val lines = FileManager.read(context, INVITES_FILE) ?: ArrayList()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val data = line.split('|')
            val inviteId = data[0]
            val opponentName = data[1]
            val timeStamp = data[2].toLong()
            val type = InviteType.fromString(data[3])

            savedInvites[inviteId] = InviteData(inviteId, opponentName, timeStamp, type)
        }
    }

    private fun loadRecentOpponents(context: Context) {
        val lines = FileManager.read(context, RECENT_OPPONENTS_FILE) ?: ArrayList()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val data = line.split('|')
            val opponentName = data[0]
            val opponentId = data[1]
            updateRecentOpponents(context, Pair(opponentName, opponentId))
        }
    }

    fun updateRecentOpponents(context: Context, newOpponent: Pair<String, String>?) {
        if (newOpponent == null) {
            return
        }
        if (newOpponent.second == userId) {
            return
        }

        val temp = Stack<Pair<String, String>>()

        while (temp.size < 2 && recentOpponents.isNotEmpty()) {
            val opponent = recentOpponents.pop()

            if (opponent == newOpponent) {
                continue
            }

            temp.push(opponent)
        }

        while (recentOpponents.isNotEmpty()) {
            recentOpponents.pop()
        }

        for (i in 0 until temp.size) {
            recentOpponents.push(temp.pop())
        }

        recentOpponents.push(newOpponent)
        saveRecentOpponents(context)
    }

    fun saveGames(context: Context) {
        var content = ""

        for ((gameId, game) in savedGames) {
            var moveData = "["

            for ((i, move) in game.moves.withIndex()) {
                moveData += move.toChessNotation()
                if (i != game.moves.size - 1) {
                    moveData += "\\"
                }
            }
            moveData += "]"

            var chatData = "["

            for ((i, message) in game.chatMessages.withIndex()) {
                chatData += message.toString()
                if (i != game.chatMessages.size - 1) {
                    chatData += "\\"
                }
            }
            chatData += "]"

            var newsContent = "["

            for ((i, news) in game.newsUpdates.withIndex()) {
                newsContent += news.toString()
                if (i != game.newsUpdates.size - 1) {
                    newsContent += "\\"
                }
            }
            newsContent += "]"

            content += "$gameId|${game.opponentId}|${game.opponentName}|${game.status}|${game.opponentStatus}|${game.lastUpdated}|${game.isPlayingWhite}|${game.moveToBeConfirmed}|$moveData|$chatData|$newsContent\n"
        }

        FileManager.write(context, MULTIPLAYER_GAME_FILE, content)
    }

    fun saveInvites(context: Context) {
        var content = ""

        for ((inviteId, invite) in savedInvites) {
            content += "$inviteId|${invite.opponentName}|${invite.timeStamp}|${invite.type}\n"
        }

        FileManager.write(context, INVITES_FILE, content)
    }

    private fun saveRecentOpponents(context: Context) {
        var data = ""
        for (recentOpponent in recentOpponents) {
            data += "${recentOpponent.first}|${recentOpponent.second}\n"
        }
        FileManager.write(context, RECENT_OPPONENTS_FILE, data)
    }

    companion object {

        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "received_invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"

        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            if (instance == null) {
                instance = DataManager(context)
            } else {
                // TODO: Is this useful?
//                instance!!.loadData()
            }
            instance!!.loadData(context)

            return instance!!
        }

    }

}