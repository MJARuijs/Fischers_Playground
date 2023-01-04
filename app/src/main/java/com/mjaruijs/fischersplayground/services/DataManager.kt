package com.mjaruijs.fischersplayground.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.activities.opening.PracticeSession
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.IntNews
import com.mjaruijs.fischersplayground.chess.news.MoveNews
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.util.Stack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class DataManager(context: Context) {

    private val savedPracticeSessions = ArrayList<PracticeSession>()
    private val savedOpenings = ArrayList<Opening>()
    private val savedGames = HashMap<String, MultiPlayerGame>()
    private val savedInvites = HashMap<String, InviteData>()
    private val recentOpponents = Stack<Pair<String, String>>()
    private val handledMessages = HashSet<Long>()

    private var userId = DEFAULT_USER_ID

    private val practiceLock = AtomicBoolean(false)
    private val openingLock = AtomicBoolean(false)
    private val gamesLock = AtomicBoolean(false)
    private val invitesLock = AtomicBoolean(false)
    private val recentOpponentsLock = AtomicBoolean(false)
    private val messageLock = AtomicBoolean(false)

    init {
        try {
            val preferences = context.getSharedPreferences("user_data", MODE_PRIVATE)
            userId = preferences.getString(ClientActivity.USER_ID_KEY, "") ?: DEFAULT_USER_ID

            loadData(context)
        } catch (e: Exception) {
            NetworkManager.getInstance().sendCrashReport("crash_data_manager_init.txt", e.stackTraceToString(), context)
        }
    }

    fun getSavedOpenings(): ArrayList<Opening> {
        obtainOpeningLock()

        val openings = savedOpenings
        unlockOpenings()

        return openings
    }

    fun removeOpening(name: String) {
        obtainOpeningLock()

        savedOpenings.removeIf { opening -> opening.name == name }

        unlockOpenings()
    }

    fun setPracticeSession(name: String, practiceSession: PracticeSession) {
        obtainPracticeSessionLock()

        savedPracticeSessions.removeIf { session ->
            session.openingName == name
        }

        savedPracticeSessions += practiceSession
        unlockPracticeSessions()
    }


    fun removePracticeSession(name: String, team: Team) {
        obtainPracticeSessionLock()
        savedPracticeSessions.removeIf { session ->
            session.openingName == name && session.team == team
        }
        FileManager.delete("practice_session_${name}_$team.txt")
        unlockPracticeSessions()
    }

    fun setOpening(name: String, team: Team, opening: Opening) {
        obtainOpeningLock()

        savedOpenings.removeIf { storedOpening ->
            storedOpening.name == name && storedOpening.team == team
        }

        savedOpenings += opening
        unlockOpenings()
    }

    fun getOpening(name: String, team: Team): Opening {
        obtainOpeningLock()

        val opening = savedOpenings.find { opening -> opening.name == name && opening.team == team }
        if (opening == null) {
            val newOpening = Opening(name, team)

            savedOpenings.add(newOpening)
            unlockOpenings()
            return newOpening
        }

        unlockOpenings()
        return opening
    }

    fun getPracticeSession(name: String, team: Team): PracticeSession? {
        obtainPracticeSessionLock()

        val session = savedPracticeSessions.find { session -> session.openingName == name && session.team == team }
        unlockPracticeSessions()
        return session
    }

    fun addOpening(opening: Opening) {
        obtainOpeningLock()
        savedOpenings += opening
        unlockOpenings()
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
        unlockGames()
    }

    fun setInvite(id: String, invite: InviteData) {
        obtainInvitesLock()
        savedInvites[id] = invite
        unlockInvites()
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

    fun isLocked() = areGamesLocked() || areInvitesLocked() || areOpponentsLocked() || areOpeningsLocked() || arePracticeSessionsLocked()

    private fun arePracticeSessionsLocked() = practiceLock.get()

    private fun areOpeningsLocked() = openingLock.get()

    private fun areGamesLocked() = gamesLock.get()

    private fun areInvitesLocked() = invitesLock.get()

    private fun areOpponentsLocked() = recentOpponentsLock.get()

    private fun areMessagesLocked() = messageLock.get()

    private fun obtainPracticeSessionLock() {
        while (arePracticeSessionsLocked()) {
            Thread.sleep(1)
        }

        lockPracticeSessions()
    }

    private fun obtainOpeningLock() {
        while (areOpeningsLocked()) {
            Thread.sleep(1)
        }

        lockOpenings()
    }

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

    private fun lockPracticeSessions() {
        practiceLock.set(true)
    }

    private fun unlockPracticeSessions() {
        practiceLock.set(false)
    }

    private fun lockOpenings() {
        openingLock.set(true)
    }

    private fun unlockOpenings() {
        openingLock.set(false)
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

    fun loadData(context: Context) {
        Logger.warn(TAG, "LOADING DATA")
        obtainPracticeSessionLock()
        Thread {
            try {
                savedPracticeSessions.clear()
                loadPracticeSessions(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_loading_practice_sessions.txt", e.stackTraceToString(), context)
            } finally {
                unlockPracticeSessions()
            }
        }.start()

        obtainOpeningLock()
        Thread {
            try {
                savedOpenings.clear()
                loadSavedOpenings(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_loading_opening.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpenings()
            }
        }.start()

        obtainGameLock()
        Thread {
            try {
                savedGames.clear()
                loadSavedGames(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_games_loading.txt", e.stackTraceToString(), context)
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
                NetworkManager.getInstance().sendCrashReport("crash_invites_loading.txt", e.stackTraceToString(), context)
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
                NetworkManager.getInstance().sendCrashReport("crash_opponents_loading.txt", e.stackTraceToString(), context)
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
                NetworkManager.getInstance().sendCrashReport("crash_messages_loading.txt", e.stackTraceToString(), context)
            } finally {
                unlockMessages()
            }
        }.start()
    }

    fun saveData(context: Context) {
//        obtainOpeningLock()
//        Thread {
//            try {
//                saveOpenings(context)
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("openings_saving.txt", e.stackTraceToString())
//            } finally {
//                unlockOpenings()
//            }
//        }.start()

        obtainGameLock()
        Thread {
            try {
                saveGames(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_games_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockGames()
            }
        }.start()

        obtainInvitesLock()
        Thread {
            try {
                saveInvites(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_invites_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockInvites()
            }
        }.start()

        obtainOpponentsLock()
        Thread {
            try {
                saveRecentOpponents(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_opponents_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpponents()
            }
        }.start()

        obtainMessageLock()
        Thread {
            try {
                saveHandledMessages(context)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_messages_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockMessages()
            }
        }.start()
    }

    private fun loadPracticeSessions(context: Context) {
        val files = FileManager.listFilesInDirectory()

        val practiceFiles = files.filter { fileName -> fileName.startsWith("practice_session_") }

        for (practiceFileName in practiceFiles) {
            val fileContent = FileManager.readText(context, practiceFileName) ?: continue

            val practiceSession = PracticeSession.fromString(fileContent)
            savedPracticeSessions += practiceSession
        }
    }

    private fun loadSavedOpenings(context: Context) {
        val files = FileManager.listFilesInDirectory()

        val openingFiles = files.filter { fileName -> fileName.startsWith("opening_") }

        for (openingFileName in openingFiles) {
            val fileContent = FileManager.readText(context, openingFileName) ?: continue

            val openingInfo = openingFileName.removePrefix("opening_").removeSuffix(".txt").split("_")
            val openingName = openingInfo[0]
            val openingTeam = Team.fromString(openingInfo[1])
            val opening = Opening(openingName, openingTeam)
            opening.addFromString(fileContent)

            savedOpenings += opening
        }
    }

    private fun loadSavedGames(context: Context) {
        val lines = FileManager.readLines(context, MULTIPLAYER_GAME_FILE) ?: ArrayList()

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

                when (news.count { char -> char == ',' }) {
                    0 -> newsUpdates += News.fromString(news)
                    1 -> newsUpdates += IntNews.fromString(news)
                    else -> newsUpdates += MoveNews.fromString(news)
                }
            }

//            val isPlayerWhite = whitePlayerId == user

            val newGame = MultiPlayerGame(gameId, opponentId, opponentName, gameStatus, opponentStatus, lastUpdated, isPlayerWhite, moveToBeConfirmed, moves, messages, newsUpdates)
            newGame.status = gameStatus
            savedGames[gameId] = newGame
        }
    }

    private fun loadInvites(context: Context) {
        val lines = FileManager.readLines(context, INVITES_FILE) ?: ArrayList()

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
        val lines = FileManager.readLines(context, RECENT_OPPONENTS_FILE) ?: ArrayList()

        for (line in lines) {
            if (line.isBlank()) {
                continue
            }

            val data = line.split('|')
            val opponentName = data[0]
            val opponentId = data[1]
            addRecentOpponent(context, Pair(opponentName, opponentId))
        }
    }

    private fun loadHandledMessages(context: Context) {
        val lines = FileManager.readLines(context, HANDLED_MESSAGES_FILE) ?: ArrayList()

        for (messageId in lines) {
            if (messageId.isBlank()) {
                continue
            }

            handledMessages += messageId.toLong()
        }
    }

    fun setRecentOpponents(context: Context, opponents: List<Pair<String, String>>) {
        recentOpponents.clear()
        for (opponent in opponents) {
            recentOpponents.push(opponent)
        }

        saveRecentOpponents(context)
    }

    fun addRecentOpponent(context: Context, newOpponent: Pair<String, String>?) {
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

//    fun saveOpening(name: String, team: Team, opening: Opening, context: Context) {
//        obtainOpeningLock()
//
//        savedOpenings.removeIf { storedOpening ->
//            storedOpening.name == name && storedOpening.team == team
//        }
//
//        savedOpenings += opening
//        unlockOpenings()
//    }

    fun savePracticeSessions(context: Context) {
        obtainPracticeSessionLock()
        Thread {
            try {
                for (practiceSession in savedPracticeSessions) {
                    Logger.debug(TAG, "Saving session: $practiceSession")
                    FileManager.write(context, "practice_session_${practiceSession.openingName}_${practiceSession.team}.txt", practiceSession.toString())
                }
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_practice_sessions_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockPracticeSessions()
            }
        }.start()
    }

    fun saveOpenings(context: Context) {
        obtainOpeningLock()
        Thread {
            try {
                for (opening in savedOpenings) {
                    Logger.debug(TAG, "Saving opening with name: opening_${opening.name}_${opening.team}.txt")
                    FileManager.write(context, "opening_${opening.name}_${opening.team}.txt", opening.toString())
                }
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_openings_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpenings()
            }

        }.start()

    }

    private fun saveGames(context: Context) {
        var content = ""
        Logger.debug(TAG, "Saving Games")
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

            val gameContent = "$gameId|${game.opponentId}|${game.opponentName}|${game.status}|${game.opponentStatus}|${game.lastUpdated}|${game.isPlayingWhite}|${game.moveToBeConfirmed}|$moveData|$chatData|$newsContent\n"
            Logger.debug(TAG, "Adding to games file: $gameContent")
            content += gameContent
        }

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

    private fun saveHandledMessages(context: Context) {
        var data = ""

        for (messageId in handledMessages) {
            data += "$messageId\n"
        }
        FileManager.write(context, HANDLED_MESSAGES_FILE, data)
    }

    fun lockAndSaveHandledMessages(context: Context) {
        obtainMessageLock()
        var data = ""

        for (messageId in handledMessages) {
            data += "$messageId\n"
        }
        try {
            FileManager.write(context, HANDLED_MESSAGES_FILE, data)
        } finally {
            unlockMessages()
        }
    }

    companion object {

        private const val TAG = "DataManager"

        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"
        const val HANDLED_MESSAGES_FILE = "handled_messages.txt"

        private var instance: DataManager? = null

        fun getInstance(context: Context): DataManager {
            if (instance == null) {
                instance = DataManager(context)
            }

            return instance!!
        }

    }

}