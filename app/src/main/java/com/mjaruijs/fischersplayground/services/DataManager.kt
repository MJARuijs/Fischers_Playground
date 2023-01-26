package com.mjaruijs.fischersplayground.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Looper
import android.widget.Toast
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
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.OpponentData
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.util.Stack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class DataManager(context: Context) {

    private val savedPracticeSessions = ArrayList<PracticeSession>()
    private val savedOpenings = ArrayList<Opening>()
    private val savedGames = ArrayList<MultiPlayerGame>()
    private val savedInvites = ArrayList<InviteData>()
    private val recentOpponents = Stack<OpponentData>()
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
//            NetworkManager.getInstance().sendCrashReport("crash_data_manager_init.txt", e.stackTraceToString(), context)
        }
    }

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
            Logger.debug(TAG, "Waiting for lock")
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

    fun getSavedOpenings(): ArrayList<Opening> {
        obtainOpeningLock()

        val openings = savedOpenings
        unlockOpenings()

        return openings
    }

    fun deleteOpening(name: String, team: Team, context: Context) {
        obtainOpeningLock()

        savedOpenings.removeIf { opening -> opening.name == name && opening.team == team }

        unlockOpenings()
        FileManager.delete("opening_${name}_${team}.txt")

        saveOpenings(context)
    }

    fun setPracticeSession(name: String, practiceSession: PracticeSession, context: Context) {
        obtainPracticeSessionLock()

        savedPracticeSessions.removeIf { session ->
            session.openingName == name
        }

        savedPracticeSessions += practiceSession
        unlockPracticeSessions()

        savePracticeSessions(context)
    }

    fun removePracticeSession(name: String, team: Team, context: Context) {
        obtainPracticeSessionLock()
        savedPracticeSessions.removeIf { session ->
            session.openingName == name && session.team == team
        }
        FileManager.delete("practice_session_${name}_$team.txt")
        unlockPracticeSessions()

        savePracticeSessions(context)
    }

    fun setOpening(name: String, team: Team, opening: Opening, context: Context) {
        obtainOpeningLock()

        savedOpenings.removeIf { storedOpening ->
            storedOpening.name == name && storedOpening.team == team
        }

        savedOpenings += opening
        unlockOpenings()

        saveOpenings(context)
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

    fun addOpening(opening: Opening, context: Context) {
        obtainOpeningLock()
        savedOpenings += opening
        unlockOpenings()

        saveOpenings(context)
    }

    fun getSavedGames(): ArrayList<MultiPlayerGame> {
        obtainGameLock()

        val games = savedGames
        unlockGames()

        return games
    }

    fun removeGame(id: String, context: Context) {
        obtainGameLock()

        savedGames.removeIf { game -> game.gameId == id }
        unlockGames()

        saveGames(context)
    }

    fun getGame(id: String): MultiPlayerGame? {
        obtainGameLock()

        val game = savedGames.find { game -> game.gameId == id }
        unlockGames()
        return game
    }

    fun setGame(id: String, game: MultiPlayerGame, context: Context) {
        obtainGameLock()
        savedGames.removeIf { oldGame -> oldGame.gameId == id }
        savedGames += game

        unlockGames()

        saveGames(context)
    }

    fun getSavedInvites(): ArrayList<InviteData> {
        obtainInvitesLock()

        val invites = savedInvites
        unlockInvites()
        return invites
    }

    fun setInvite(id: String, inviteData: InviteData, context: Context) {
        obtainInvitesLock()
        savedInvites.removeIf { oldInvite -> oldInvite.inviteId == id }
        savedInvites += inviteData
        unlockInvites()
        saveInvites(context)
    }

    fun removeSavedInvite(id: String, context: Context) {
        obtainInvitesLock()
        savedInvites.removeIf { oldInvite -> oldInvite.inviteId == id }
        unlockInvites()

        saveInvites(context)
    }

    fun getRecentOpponents(): ArrayList<OpponentData> {
        obtainOpponentsLock()
        val opponents = ArrayList<OpponentData>()

        for (opponent in recentOpponents) {
            opponents += opponent
        }
        unlockOpponents()
        return opponents
    }

    fun isLocked() = areGamesLocked() || areInvitesLocked() || areOpponentsLocked() || areOpeningsLocked() || arePracticeSessionsLocked()

    fun setMessageHandled(id: Long, context: Context) {
        obtainMessageLock()
        handledMessages += id
        unlockMessages()

        saveHandledMessages(context)
    }

    fun isMessageHandled(id: Long): Boolean {
        obtainMessageLock()
        val isHandled = handledMessages.contains(id)
        unlockMessages()
        return isHandled
    }

    fun loadData(context: Context) {
        Logger.warn(TAG, "LOADING DATA")

        loadPracticeSessions(context)
        loadSavedOpenings(context)
        loadSavedGames(context)
        loadInvites(context)
        loadRecentOpponents(context)
        loadHandledMessages(context)
    }

    fun saveData(context: Context) {
        saveOpenings(context)
        saveGames(context)
        saveInvites(context)
        saveRecentOpponents(context)
        saveHandledMessages(context)
        savePracticeSessions(context)
    }

    private fun loadPracticeSessions(context: Context) {
        obtainPracticeSessionLock()
        Thread {
            try {
                val files = FileManager.listFilesInDirectory()

                val practiceFiles = files.filter { fileName -> fileName.startsWith("practice_session_") }

                savedPracticeSessions.clear()
                for (practiceFileName in practiceFiles) {
                    val fileContent = FileManager.readText(context, practiceFileName) ?: continue

                    val practiceSession = PracticeSession.fromString(fileContent)
                    savedPracticeSessions += practiceSession
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_loading_practice_sessions.txt", e.stackTraceToString(), context)
            } finally {
                unlockPracticeSessions()
            }
        }.start()
    }

    private fun loadSavedOpenings(context: Context) {
        obtainOpeningLock()
        Thread {
            try {
                savedOpenings.clear()
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
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_loading_opening.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpenings()
            }
        }.start()
    }

    private fun loadSavedGames(context: Context) {
        obtainGameLock()
        Thread {
            try {
                savedGames.clear()
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

                            messages += ChatMessage(gameId, timeStamp, messageContent, type)
                        }
                    }

                    val newsUpdates = ArrayList<News>()
                    for (news in newsData) {
                        if (news.isBlank()) {
                            continue
                        }
                        newsUpdates += News.fromString(news)

                    }

        //            val isPlayerWhite = whitePlayerId == user

                    val newGame = MultiPlayerGame(gameId, opponentId, opponentName, gameStatus, opponentStatus, lastUpdated, isPlayerWhite, moveToBeConfirmed, moves, messages, newsUpdates)
//                    if (newsUpdates.any { news -> news.newsType == NewsType.OPPONENT_MOVED }) {
//                        newGame.status =
//                    }
                    newGame.status = gameStatus
                    savedGames += newGame
                }
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_games_loading.txt", e.stackTraceToString(), context)
            } finally {
                unlockGames()
            }
        }.start()
    }

    private fun loadInvites(context: Context) {
        obtainInvitesLock()
        Thread {
            try {
                savedInvites.clear()
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

                    savedInvites += InviteData(inviteId, opponentName, timeStamp, type)
                }
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_invites_loading.txt", e.stackTraceToString(), context)
            } finally {
                unlockInvites()
            }
        }.start()

    }

    private fun loadRecentOpponents(context: Context) {
        obtainOpponentsLock()
        Thread {
            try {
                recentOpponents.clear()
                val lines = FileManager.readLines(context, RECENT_OPPONENTS_FILE) ?: ArrayList()

                for (line in lines) {
                    if (line.isBlank()) {
                        continue
                    }

                    val data = line.split('|')
                    val opponentName = data[0]
                    val opponentId = data[1]
                    addRecentOpponent(context, OpponentData(opponentName, opponentId))
                }
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_opponents_loading.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpponents()
            }
        }.start()
    }

    private fun loadHandledMessages(context: Context) {
        obtainMessageLock()
        Thread {
            try {
                handledMessages.clear()
                val lines = FileManager.readLines(context, HANDLED_MESSAGES_FILE) ?: ArrayList()

                for (messageId in lines) {
                    if (messageId.isBlank()) {
                        continue
                    }

                    handledMessages += messageId.toLong()
                }
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_messages_loading.txt", e.stackTraceToString(), context)
            } finally {
                unlockMessages()
            }
        }.start()
    }

    fun setRecentOpponents(context: Context, opponents: ArrayList<OpponentData>) {
        recentOpponents.clear()
        for (opponent in opponents) {
            recentOpponents.push(opponent)
        }

        saveRecentOpponents(context)
    }

    fun addRecentOpponent(context: Context, newOpponent: OpponentData?) {
        if (newOpponent == null) {
            return
        }
        if (newOpponent.opponentId == userId) {
            return
        }

        val temp = Stack<OpponentData>()

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
//        saveRecentOpponents(context)
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

//                NetworkManager.getInstance().sendCrashReport("crash_practice_sessions_saving.txt", e.stackTraceToString(), context)
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
//                NetworkManager.getInstance().sendCrashReport("crash_openings_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpenings()
            }

        }.start()

    }

    fun saveGames(context: Context) {
        obtainGameLock()
        Thread {
            try {
                var content = ""
                for (game in savedGames) {
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

                    val gameContent = "${game.gameId}|${game.opponentId}|${game.opponentName}|${game.status}|${game.opponentStatus}|${game.lastUpdated}|${game.isPlayingWhite}|${game.moveToBeConfirmed}|$moveData|$chatData|$newsContent\n"
                    content += gameContent
                }

                FileManager.write(context, MULTIPLAYER_GAME_FILE, content)
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_games_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockGames()
            }
        }.start()
    }

    fun saveInvites(context: Context) {
        obtainInvitesLock()
        Thread {
            try {
                var content = ""

                for (invite in savedInvites) {
                    content += "${invite.inviteId}|${invite.opponentName}|${invite.timeStamp}|${invite.type}\n"
                }

//                Logger.debug(TAG, "Saving invites: ${savedInvites.size}")

                FileManager.write(context, INVITES_FILE, content)
            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_invites_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockInvites()
            }
        }.start()
    }

    private fun saveRecentOpponents(context: Context) {
        obtainOpponentsLock()
        Thread {
            try {
                var data = ""
                for (recentOpponent in recentOpponents) {
                    data += "${recentOpponent.opponentName}|${recentOpponent.opponentId}\n"
                }
                FileManager.write(context, RECENT_OPPONENTS_FILE, data)
            } catch (e: Exception) {
                Logger.error(TAG, e.stackTraceToString())
                NetworkService.sendCrashReport("crash_opponents_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockOpponents()
            }
        }.start()
    }

    fun saveHandledMessages(context: Context) {
        obtainMessageLock()
        Thread {
            try {
                var data = ""

                for (messageId in handledMessages) {
                    data += "$messageId\n"
                }
                FileManager.write(context, HANDLED_MESSAGES_FILE, data)
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_messages_saving.txt", e.stackTraceToString(), context)
            } finally {
                unlockMessages()
            }
        }.start()
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