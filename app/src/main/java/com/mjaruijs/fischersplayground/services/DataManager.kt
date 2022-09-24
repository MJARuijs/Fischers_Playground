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
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.util.FileManager
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class DataManager(context: Context) {

    private val savedGames = HashMap<String, MultiPlayerGame>()
    private val savedInvites = HashMap<String, InviteData>()
    private val recentOpponents = Stack<Pair<String, String>>()
    private val handledMessages = HashSet<Long>()

    private val userId: String

    private val gamesLock = AtomicBoolean(false)
    private val invitesLock = AtomicBoolean(false)
    private val recentOpponentsLock = AtomicBoolean(false)
    private val messageLock = AtomicBoolean(false)

    init {
        val preferences = context.getSharedPreferences("user_data", MODE_PRIVATE)
        userId = preferences.getString(ClientActivity.USER_ID_KEY, "")!!

        loadData(context, "DataManager")
    }

    fun getSavedGames(): HashMap<String, MultiPlayerGame> {
        obtainGameLock()

        val games = savedGames
        unlockGames()

        return games
    }

    fun removeGame(id: String) {
        obtainGameLock()

        savedGames.remove(id)
        unlockGames()
    }

    fun getGame(id: String): MultiPlayerGame? {
        obtainGameLock()

        val game = savedGames[id]
        unlockGames()
        return game
    }

    fun setGame(id: String, game: MultiPlayerGame) {
        obtainGameLock()

        savedGames[id] = game
//        println("SETTING GAME: ${game.chatMessages.size} ${savedGames[id]!!.chatMessages.size}")
        unlockGames()
    }

    fun getSavedInvites(): HashMap<String, InviteData> {
        obtainInvitesLock()

        val invites = savedInvites
        unlockInvites()
        return invites
    }

    fun saveInvite(id: String, inviteData: InviteData) {
        obtainInvitesLock()
        savedInvites[id] = inviteData
        unlockInvites()
    }

    fun removeSavedInvite(id: String) {
        obtainInvitesLock()
        savedInvites.remove(id)
        unlockInvites()
    }

    fun getRecentOpponents(): Stack<Pair<String, String>> {
        obtainOpponentsLock()
        val opponents = recentOpponents
        unlockOpponents()
        return opponents
    }

    fun isLocked() = areGamesLocked() || areInvitesLocked() || areOpponentsLocked()

    private fun areGamesLocked() = gamesLock.get()

    private fun areInvitesLocked() = invitesLock.get()

    private fun areOpponentsLocked() = recentOpponentsLock.get()

    private fun areMessagesLocked() = messageLock.get()

    private fun obtainGameLock() {
        while (areGamesLocked()) {
            Thread.sleep(1)
        }

        lockGames()
    }

    private fun obtainInvitesLock() {
        while (areInvitesLocked()) {
            Thread.sleep(1)
        }

        lockInvites()
    }

    private fun obtainOpponentsLock() {
        while (areOpponentsLocked()) {
            Thread.sleep(1)
        }

        lockOpponents()
    }

    private fun obtainMessageLock() {
        while (areMessagesLocked()) {
            Thread.sleep(1)
        }

        lockMessages()
    }

    private fun lockGames() {
        gamesLock.set(true)
    }

    private fun unlockGames() {
        gamesLock.set(false)
    }

    private fun lockInvites() {
        invitesLock.set(true)
    }

    private fun unlockInvites() {
        invitesLock.set(false)
    }

    private fun lockOpponents() {
        recentOpponentsLock.set(true)
    }

    private fun unlockOpponents() {
        recentOpponentsLock.set(false)
    }

    private fun lockMessages() {
        messageLock.set(true)
    }

    private fun unlockMessages() {
        messageLock.set(false)
    }

    fun handledMessage(id: Long) {
        obtainMessageLock()
        handledMessages += id
        unlockMessages()
    }

    fun isMessageHandled(id: Long): Boolean {
        obtainMessageLock()
        val isHandled = handledMessages.contains(id)
        unlockMessages()
        return isHandled
    }

    fun loadData(context: Context, caller: String) {
        obtainGameLock()
        Thread {
            try {
                savedGames.clear()
                loadSavedGames(context, caller)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "games_loading_crash.txt", e.stackTraceToString())
            } finally {
                unlockGames()
            }
        }.start()

        obtainInvitesLock()
        Thread {
            try {
                savedInvites.clear()
                loadInvites(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "invites_loading_crash.txt", e.stackTraceToString())
            } finally {
                unlockInvites()
            }
        }.start()

        obtainOpponentsLock()
        Thread {
            try {
                recentOpponents.clear()
                loadRecentOpponents(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "opponents_loading_crash.txt", e.stackTraceToString())
            } finally {
                unlockOpponents()
            }
        }.start()

        obtainMessageLock()
        Thread {
            try {
                handledMessages.clear()
                loadHandledMessages(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "messages_loading_crash.txt", e.stackTraceToString())
            } finally {
                unlockMessages()
            }
        }.start()
    }

    fun saveData(context: Context, caller: String) {

        Thread {
            try {
                obtainGameLock()
                saveGames(context, caller)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "games_saving_crash.txt", e.stackTraceToString())
            } finally {
                unlockGames()
            }
        }.start()

        obtainInvitesLock()
        Thread {
            try {
                saveInvites(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "invites_saving_crash.txt", e.stackTraceToString())
            } finally {
                unlockInvites()
            }
        }.start()

        obtainOpponentsLock()
        Thread {
            try {
                saveRecentOpponents(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "opponents_saving_crash.txt", e.stackTraceToString())
            } finally {
                unlockOpponents()
            }
        }.start()

        obtainMessageLock()
        Thread {
            try {
                saveHandledMessages(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport(context, "messages_saving_crash.txt", e.stackTraceToString())
            } finally {
                unlockMessages()
            }
        }.start()
    }

    private fun loadSavedGames(context: Context, caller: String) {
        val lines = FileManager.read(context, MULTIPLAYER_GAME_FILE) ?: ArrayList()

        for (gameData in lines) {
            if (gameData.isBlank()) {
                continue
            }

            println("$caller loaded saved game: $gameData")

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
                    val messageData = message.split('~')
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

    private fun loadHandledMessages(context: Context) {
        val lines = FileManager.read(context, HANDLED_MESSAGES_FILE) ?: ArrayList()

        for (messageId in lines) {
            if (messageId.isBlank()) {
                continue
            }

            handledMessages += messageId.toLong()
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

    private fun saveGames(context: Context, caller: String) {
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

        println("$caller Saving games: $content")

        FileManager.write(context, MULTIPLAYER_GAME_FILE, content)
    }

    private fun saveInvites(context: Context) {
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

    fun saveHandledMessages(context: Context) {
        obtainMessageLock()
        var data = ""

        for (messageId in handledMessages) {
            data += "$messageId\n"
        }
        FileManager.write(context, HANDLED_MESSAGES_FILE, data)
        unlockMessages()
    }

    companion object {

        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "received_invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"
        const val HANDLED_MESSAGES_FILE = "handled_messages.txt"

        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            if (instance == null) {
                instance = DataManager(context)
            }
            instance!!.loadData(context, "getInstance()")

            return instance!!
        }

    }

}