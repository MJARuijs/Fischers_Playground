package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Intent
import android.os.*
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_ID_KEY
import com.mjaruijs.fischersplayground.activities.opening.PracticeSession
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.game.OpponentData
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.parcelable.ParcelableNull
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

class DataManagerService : Service() {

    private val savedPracticeSessions = ArrayList<PracticeSession>()
    private val savedOpenings = ArrayList<Opening>()
    private val savedGames = ArrayList<MultiPlayerGame>()
    private val savedInvites = ArrayList<InviteData>()
    private val recentOpponents = Stack<OpponentData>()
    private val handledMessages = HashSet<Long>()

    private var userId = DEFAULT_USER_ID

    private val initialized = AtomicBoolean(false)
    private val practiceLock = AtomicBoolean(false)
    private val openingLock = AtomicBoolean(false)
    private val gamesLock = AtomicBoolean(false)
    private val invitesLock = AtomicBoolean(false)
    private val recentOpponentsLock = AtomicBoolean(false)
    private val messageLock = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? {
        if (intent != null) {
            val request = intent.getStringExtra("request")
            if (request != null) {
                val data = intent.getBundleExtra("data") ?: Bundle()
                processRequest(Request.fromString(request), data)
            }
        }

        return Messenger(IncomingHandler(this)).binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.debug(TAG, "Starting service. Number of invites: ${savedInvites.size}")
//        Looper.prepare()
//        Toast.makeText(applicationContext, "Started service!", Toast.LENGTH_SHORT).show()
        if (intent != null) {
            val request = intent.getStringExtra("request")
            if (request != null) {
                val data = intent.getBundleExtra("data") ?: Bundle()
                processRequest(Request.fromString(request), data)
            }
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Logger.debug(TAG, "Creating service")
        userId = getSharedPreferences("user_data", MODE_PRIVATE).getString(USER_ID_KEY, "") ?: DEFAULT_USER_ID

        loadData()

        // TODO: Reimplement this
        val missedNotifications = FileManager.readLines(applicationContext, "notifications.txt") ?: ArrayList()
        for (notification in missedNotifications) {
            if (notification.isBlank()) {
                continue
            }

            val data = notification.split(";")
            val topic = data[0]
            val contentList = data[1].split('|').toTypedArray()
            val messageId = data[2].toLong()
            Logger.debug(TAG, "Start worker for notification: $topic")
            val worker = OneTimeWorkRequestBuilder<ProcessIncomingDataWorker>()
                .setInputData(workDataOf(
                    Pair("topic", topic),
                    Pair("content", contentList),
                    Pair("messageId", messageId)
                ))
                .build()

            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(worker)
        }

        FileManager.write(applicationContext, "notifications.txt", "")
        initialized.set(true)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.debug(TAG, "Unbinding service")
        saveData()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Logger.debug(TAG, "Destroying service")
        saveData()
        super.onDestroy()
    }

    private fun getInputString(data: Bundle, key: String): String {
        return data.getString(key) ?: throw IllegalArgumentException("Tried to get string from bundle but was missing: $key")
    }

    private fun getInputLong(data: Bundle, @Suppress("SameParameterValue") key: String): Long {
        if (!data.containsKey(key)) {
            throw IllegalArgumentException("Tried to get long from bundle but was missing: $key")
        }
        return data.getLong(key)
    }

    private inline fun <reified T : Parcelable>getInputParcelable(data: Bundle, key: String): T {
        return if (Build.VERSION.SDK_INT < TIRAMISU) {
            @Suppress("DEPRECATION")
            data.getParcelable(key)!!
        } else {
            data.getParcelable(key, T::class.java)!!
        }
    }

    private inline fun <reified T : Parcelable>getInputParcelebleList(data: Bundle, key: String): ArrayList<T> {
        return if (Build.VERSION.SDK_INT < TIRAMISU) {
            @Suppress("DEPRECATION")
            data.getParcelableArrayList(key)!!
        } else {
            data.getParcelableArrayList(key, T::class.java)!!
        }
    }

    private fun getSavedOpenings(): ArrayList<Opening> {
        obtainOpeningLock()

        val openings = savedOpenings
        unlockOpenings()

        return openings
    }

    private fun removeOpening(data: Bundle) {
        val name = getInputString(data, "opening_name")

        obtainOpeningLock()

        savedOpenings.removeIf { opening -> opening.name == name }

        unlockOpenings()

        saveOpenings()
    }

    private fun setOpening(data: Bundle) {
        val name = getInputString(data, "opening_name")
        val team = Team.fromString(getInputString(data, "opening_team"))
        val opening = getInputParcelable<Opening>(data, "opening")

        obtainOpeningLock()

        savedOpenings.removeIf { storedOpening ->
            storedOpening.name == name && storedOpening.team == team
        }

        savedOpenings += opening
        unlockOpenings()

        saveOpenings()
    }

    private fun addOpening(data: Bundle) {
        val opening = getInputParcelable<Opening>(data,"opening")

        obtainOpeningLock()
        savedOpenings += opening
        unlockOpenings()

        saveOpenings()
    }

    private fun getOpening(data: Bundle): Parcelable {
        val name = getInputString(data, "opening_name")
        val team = Team.fromString(getInputString(data, "opening_team"))

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

    private fun getPracticeSession(data: Bundle): Parcelable {
        val name = getInputString(data, "session_name")
        val team = Team.fromString(getInputString(data, "session_team"))

        obtainPracticeSessionLock()

        val session = savedPracticeSessions.find { session -> session.openingName == name && session.team == team }
        unlockPracticeSessions()

        if (session == null) {
            return ParcelableNull()
        }

        return session
    }

    private fun setPracticeSession(data: Bundle) {
        val name = getInputString(data,"session_name")
        val practiceSession = getInputParcelable<PracticeSession>(data, "session")

        obtainPracticeSessionLock()

        savedPracticeSessions.removeIf { session ->
            session.openingName == name
        }

        savedPracticeSessions += practiceSession
        unlockPracticeSessions()

        savePracticeSessions()
    }

    private fun removePracticeSession(data: Bundle) {
        val name = getInputString(data,"session_name")
        val team = Team.fromString(getInputString(data,"session_team"))

        obtainPracticeSessionLock()
        savedPracticeSessions.removeIf { session ->
            session.openingName == name && session.team == team
        }
        FileManager.delete("practice_session_${name}_$team.txt")
        unlockPracticeSessions()

        savePracticeSessions()
    }

    private fun getSavedGames(): ArrayList<MultiPlayerGame> {
        obtainGameLock()

        val games = savedGames
        unlockGames()

        return games
    }

    private fun removeGame(data: Bundle) {
        val id = getInputString(data, "game_id")

        obtainGameLock()

        savedGames.removeIf { game -> game.gameId == id }
        unlockGames()

        saveGames()
    }

    private fun getGame(data: Bundle): Parcelable {
        val id = getInputString(data, "game_id")

        obtainGameLock()

        val game = savedGames.find { game -> game.gameId == id }
        unlockGames()

        if (game == null) {
            Logger.debug(TAG, "No game with id $id could be found..")
            return ParcelableNull()
        }

        return game
    }

    private fun setGame(data: Bundle) {
        val id = getInputString(data, "game_id")
        val game = getInputParcelable<MultiPlayerGame>(data, "game")

        obtainGameLock()
        savedGames.removeIf { oldGame -> oldGame.gameId == id }
        savedGames += game
        unlockGames()

        saveGames()
    }

    private fun getSavedInvites(): ArrayList<InviteData> {
        obtainInvitesLock()

        val invites = savedInvites
        unlockInvites()
        return invites
    }

    private fun setInvite(data: Bundle) {
        val invite = getInputParcelable<InviteData>(data, "invite")

        obtainInvitesLock()
        savedInvites.removeIf { oldInvite -> oldInvite.inviteId == invite.inviteId }
        savedInvites += invite
        unlockInvites()

        saveInvites()
    }

    private fun removeSavedInvite(data: Bundle) {
        val id = getInputString(data, "invite_id")

        obtainInvitesLock()
        savedInvites.removeIf { oldInvite -> oldInvite.inviteId == id }
        unlockInvites()

        saveInvites()
    }

    private fun getRecentOpponents(): ArrayList<OpponentData> {
//        Logger.debug(TAG, "Getting recent opponents")
        obtainOpponentsLock()

        val opponents = ArrayList<OpponentData>()

        for (opponent in recentOpponents) {
            opponents += opponent
        }

//        Logger.debug(TAG, "Releasing lock in get()")
        unlockOpponents()
        return opponents
    }

    private fun setRecentOpponents(data: Bundle) {
        val opponents = getInputParcelebleList<OpponentData>(data, "opponents")

        recentOpponents.clear()
        for (opponent in opponents) {
            recentOpponents.push(opponent)
        }
        saveRecentOpponents()
    }

    private fun addRecentOpponent(data: Bundle) {
        val opponentData = getInputParcelable<OpponentData>(data, "opponent_data")
        addRecentOpponent(opponentData)
        saveRecentOpponents()
    }

    private fun addRecentOpponent(newOpponent: OpponentData) {
        val userId = applicationContext.getSharedPreferences("user_data", MODE_PRIVATE).getString(USER_ID_KEY, "") ?: DEFAULT_USER_ID
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
//            Logger.debug(TAG, "Waiting for opponents lock")
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

    private fun setMessageHandled(data: Bundle) {
        val id = getInputLong(data, "message_id")

        obtainMessageLock()
        handledMessages += id
        unlockMessages()

        saveHandledMessages()
    }

    private fun isMessageHandled(data: Bundle): Boolean {
        val id = getInputLong(data, "message_id")

        obtainMessageLock()
        val isHandled = handledMessages.contains(id)
        unlockMessages()
        return isHandled
    }

    private fun loadData() {
        Logger.warn(TAG, "LOADING DATA")
        loadPracticeSessions()
        loadSavedOpenings()
        loadSavedGames()
        loadInvites()
        loadRecentOpponents()
        loadHandledMessages()
    }

    private fun saveData() {
        saveOpenings()
        saveGames()
        saveInvites()
        saveRecentOpponents()
        saveHandledMessages()
        savePracticeSessions()
    }

    private fun loadPracticeSessions() {
        obtainPracticeSessionLock()
        Thread {
            try {
                val files = FileManager.listFilesInDirectory()

                val practiceFiles = files.filter { fileName -> fileName.startsWith("practice_session_") }

                savedPracticeSessions.clear()

                for (practiceFileName in practiceFiles) {
                    val fileContent = FileManager.readText(applicationContext, practiceFileName) ?: continue

                    val practiceSession = PracticeSession.fromString(fileContent)
                    savedPracticeSessions += practiceSession
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_loading_practice_sessions.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockPracticeSessions()
            }
        }.start()

    }

    private fun loadSavedOpenings() {
        obtainOpeningLock()
        Thread {
            try {
                val files = FileManager.listFilesInDirectory()

                val openingFiles = files.filter { fileName -> fileName.startsWith("opening_") }
                savedOpenings.clear()

                for (openingFileName in openingFiles) {
                    val fileContent = FileManager.readText(applicationContext, openingFileName) ?: continue

                    val openingInfo = openingFileName.removePrefix("opening_").removeSuffix(".txt").split("_")
                    val openingName = openingInfo[0]
                    val openingTeam = Team.fromString(openingInfo[1])
                    val opening = Opening(openingName, openingTeam)
                    opening.addFromString(fileContent)

                    savedOpenings += opening
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_loading_opening.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockOpenings()
            }
        }.start()

    }

    private fun loadSavedGames() {
        obtainGameLock()
        Thread {
            try {
                savedGames.clear()
                val lines = FileManager.readLines(applicationContext, MULTIPLAYER_GAME_FILE) ?: ArrayList()

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
//                        when (news.count { char -> char == ',' }) {
//                            0 -> newsUpdates += News.fromString(news)
//                            1 -> newsUpdates += IntNews.fromString(news)
//                            else -> newsUpdates += MoveNews.fromString(news)
//                        }
                    }

                    val newGame = MultiPlayerGame(gameId, opponentId, opponentName, gameStatus, opponentStatus, lastUpdated, isPlayerWhite, moveToBeConfirmed, moves, messages, newsUpdates)
                    newGame.status = gameStatus
                    savedGames += newGame
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_games_loading.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockGames()
            }
        }.start()
    }

    private fun loadInvites() {
        obtainInvitesLock()
        Thread {
            try {
                savedInvites.clear()
                val lines = FileManager.readLines(applicationContext, INVITES_FILE) ?: ArrayList()

//                Logger.debug(TAG, "Going to read ${lines.size} lines from invites.txt")

                for (line in lines) {
//                    Logger.debug(TAG, "Reading line from invites.txt: $line")
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
                NetworkService.sendCrashReport("crash_invites_loading.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockInvites()
//                Logger.debug(TAG, "Number of invites loaded: ${savedInvites.size}")
            }
        }.start()
    }

    private fun loadRecentOpponents() {
//        Logger.debug(TAG, "Loading recent opponents")
        obtainOpponentsLock()
        Thread {
            try {
                recentOpponents.clear()
                val lines = FileManager.readLines(applicationContext, RECENT_OPPONENTS_FILE) ?: ArrayList()

                for (line in lines) {
                    if (line.isBlank()) {
                        continue
                    }

                    val data = line.split('|')
                    val opponentName = data[0]
                    val opponentId = data[1]
                    addRecentOpponent(OpponentData(opponentName, opponentId))
                }
            } catch (e: Exception) {
                Logger.error(TAG, e.stackTraceToString())
                NetworkService.sendCrashReport("crash_opponents_loading.txt", e.stackTraceToString(), applicationContext)
            } finally {
//                Logger.debug(TAG, "Releasing lock in load()")
                unlockOpponents()
            }
        }.start()
    }

    private fun loadHandledMessages() {
        obtainMessageLock()
        Thread {
            try {
                handledMessages.clear()
                val lines = FileManager.readLines(applicationContext, HANDLED_MESSAGES_FILE) ?: ArrayList()

                for (messageId in lines) {
                    if (messageId.isBlank()) {
                        continue
                    }

                    handledMessages += messageId.toLong()
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_messages_loading.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockMessages()
            }
        }.start()
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

    private fun savePracticeSessions() {
        obtainPracticeSessionLock()
        Thread {
            try {
                for (practiceSession in savedPracticeSessions) {
                    FileManager.write(applicationContext, "practice_session_${practiceSession.openingName}_${practiceSession.team}.txt", practiceSession.toString())
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_practice_sessions_saving.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockPracticeSessions()
            }
        }.start()
    }

    private fun saveOpenings() {
        obtainOpeningLock()
        Thread {
            try {
                for (opening in savedOpenings) {
                    FileManager.write(applicationContext, "opening_${opening.name}_${opening.team}.txt", opening.toString())
                }
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_openings_saving.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockOpenings()
            }

        }.start()
    }

    private fun saveGames() {
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

                FileManager.write(applicationContext, MULTIPLAYER_GAME_FILE, content)
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_games_saving.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockGames()
            }
        }.start()
    }

    private fun saveInvites() {
        obtainInvitesLock()
        Thread {
            try {
                var content = ""

                for (invite in savedInvites) {
                    content += "${invite.inviteId}|${invite.opponentName}|${invite.timeStamp}|${invite.type}\n"
                }

                val writeSuccessful = FileManager.write(applicationContext, INVITES_FILE, content)

                Logger.debug(TAG, "Saving invites: $writeSuccessful: $content")

                val fileContent = FileManager.readText(applicationContext, INVITES_FILE)
                Logger.debug(TAG, "Wrote content: ${fileContent}")
            } catch (e: Exception) {
                Logger.error(TAG, e.stackTraceToString())
                NetworkService.sendCrashReport("crash_invites_saving.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockInvites()
            }
        }.start()
    }

    private fun saveRecentOpponents() {
//        Logger.debug(TAG, "Saving opponents")
        obtainOpponentsLock()
        Thread {
            try {
                var data = ""
                for (recentOpponent in recentOpponents) {
                    data += "${recentOpponent.opponentName}|${recentOpponent.opponentId}\n"
                }
                Logger.debug(TAG, "Saving recent opponents: $data")
                FileManager.write(applicationContext, RECENT_OPPONENTS_FILE, data)
            } catch (e: Exception) {
                Logger.error(TAG, e.stackTraceToString())
                NetworkService.sendCrashReport("crash_opponents_saving.txt", e.stackTraceToString(), applicationContext)
            } finally {
//                Logger.debug(TAG, "Releasing lock in save()")
                unlockOpponents()
            }
        }.start()
    }

    private fun saveHandledMessages() {
        obtainMessageLock()
        Thread {
            try {
                var data = ""

                for (messageId in handledMessages) {
                    data += "$messageId\n"
                }
                FileManager.write(applicationContext, HANDLED_MESSAGES_FILE, data)
            } catch (e: Exception) {
                NetworkService.sendCrashReport("crash_messages_saving.txt", e.stackTraceToString(), applicationContext)
            } finally {
                unlockMessages()
            }
        }.start()
    }

    private fun processRequest(request: Request, data: Bundle): Any {
        while (!initialized.get()) {
            Logger.debug(TAG, "WAITING FOR DATAMANAGER TO INITIALIZE")
            Thread.sleep(500)
        }

        return when (request) {
            Request.LOAD_DATA -> loadData()
            Request.SAVE_DATA -> saveData()
            Request.GET_SAVED_OPENINGS -> getSavedOpenings()
            Request.REMOVE_OPENING -> removeOpening(data)
            Request.SET_OPENING -> setOpening(data)
            Request.ADD_OPENING -> addOpening(data)
            Request.GET_OPENING -> getOpening(data)
            Request.GET_PRACTICE_SESSION -> getPracticeSession(data)
            Request.SET_PRACTICE_SESSION -> setPracticeSession(data)
            Request.REMOVE_PRACTICE_SESSION -> removePracticeSession(data)
            Request.GET_SAVED_GAMES -> getSavedGames()
            Request.REMOVE_GAME -> removeGame(data)
            Request.GET_GAME -> getGame(data)
            Request.SET_GAME-> setGame(data)
            Request.GET_SAVED_INVITES -> getSavedInvites()
            Request.SET_INVITE -> setInvite(data)
            Request.REMOVE_INVITE -> removeSavedInvite(data)
            Request.GET_RECENT_OPPONENTS -> getRecentOpponents()
            Request.SET_RECENT_OPPONENTS -> setRecentOpponents(data)
            Request.ADD_RECENT_OPPONENT -> addRecentOpponent(data)
            Request.SET_MESSAGE_HANDLED -> setMessageHandled(data)
            Request.IS_MESSAGE_HANDLED -> isMessageHandled(data)
            Request.SAVE_GAMES -> saveGames()
            Request.SAVE_INVITES -> saveInvites()
            Request.SAVE_HANDLED_MESSAGES -> saveHandledMessages()
            Request.SAVE_OPENINGS -> saveOpenings()
            Request.SAVE_SESSIONS -> savePracticeSessions()
            Request.SAVE_RECENT_OPPONENTS -> saveRecentOpponents()
        }
    }

//    var currentClient: Messenger? = null

    class IncomingHandler(service: DataManagerService): Handler() {

        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = serviceReference.get()!!

//            if (msg.what == 0) {
//                service.currentClient = msg.replyTo
//                return
//            }

            val request = Request.fromString(msg.data.getString("request")!!)
            val data = msg.data.getBundle("data") ?: Bundle()


            val output = service.processRequest(request, data)
            Logger.debug(TAG, "Processing request: $request ${output::class.java} ${output is Parcelable} ${output is ArrayList<*>}")

            if (output is Parcelable) {
                val reply = Message.obtain()
                reply.obj = msg.obj
                reply.what = 1
                reply.data.putParcelable("output", output)
                Logger.debug(TAG, "Sending reply to $request")
                msg.replyTo.send(reply)
//                service.currentClient!!.send(reply)
            } else if (output is ArrayList<*>) {
                if (output.isEmpty() || (output.isNotEmpty() && output.first() is Parcelable)) {
                    val reply = Message.obtain()
                    reply.obj = msg.obj
                    reply.what = 2
                    reply.data.putParcelableArrayList("output", output as ArrayList<out Parcelable>)
                    Logger.debug(TAG, "Sending reply to $request")
                    msg.replyTo.send(reply)
//                    service.currentClient!!.send(reply)
                }
            }
        }
    }

    companion object {
        private const val TAG = "DataManagerService"

        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
        const val INVITES_FILE = "invites.txt"
        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"
        const val HANDLED_MESSAGES_FILE = "handled_messages.txt"
    }

    enum class Request {
        LOAD_DATA,
        SAVE_DATA,
        GET_SAVED_OPENINGS,
        REMOVE_OPENING,
        SET_OPENING,
        ADD_OPENING,
        GET_OPENING,
        GET_PRACTICE_SESSION,
        SET_PRACTICE_SESSION,
        REMOVE_PRACTICE_SESSION,
        GET_SAVED_GAMES,
        REMOVE_GAME,
        GET_GAME,
        SET_GAME,
        GET_SAVED_INVITES,
        SET_INVITE,
        REMOVE_INVITE,
        ADD_RECENT_OPPONENT,
        GET_RECENT_OPPONENTS,
        SET_RECENT_OPPONENTS,
        SET_MESSAGE_HANDLED,
        IS_MESSAGE_HANDLED,
        SAVE_GAMES,
        SAVE_INVITES,
        SAVE_RECENT_OPPONENTS,
        SAVE_HANDLED_MESSAGES,
        SAVE_OPENINGS,
        SAVE_SESSIONS;

        companion object {

            fun fromString(content: String): Request {
                for (value in values()) {
                    if (value.toString().uppercase() == content.uppercase()) {
                        return value
                    }
                }
                throw IllegalArgumentException("Failed to create a DataManagerWorker.Request from content: $content")
            }

        }

    }
}