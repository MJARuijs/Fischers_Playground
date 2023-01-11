//package com.mjaruijs.fischersplayground.services
//
//import android.content.Context
//import android.content.Context.MODE_PRIVATE
//import android.os.Parcel
//import android.os.Parcelable
//import androidx.work.Data
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import androidx.work.hasKeyWithValueOfType
//import com.mjaruijs.fischersplayground.activities.ClientActivity
//import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
//import com.mjaruijs.fischersplayground.activities.opening.PracticeSession
//import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
//import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
//import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
//import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
//import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
//import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
//import com.mjaruijs.fischersplayground.chess.game.Move
//import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
//import com.mjaruijs.fischersplayground.chess.news.IntNews
//import com.mjaruijs.fischersplayground.chess.news.MoveNews
//import com.mjaruijs.fischersplayground.chess.news.News
//import com.mjaruijs.fischersplayground.chess.pieces.Team
//import com.mjaruijs.fischersplayground.networking.NetworkManager
//import com.mjaruijs.fischersplayground.parcelable.ParcelableNull
//import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
//import com.mjaruijs.fischersplayground.parcelable.ParcelableString
//import com.mjaruijs.fischersplayground.util.FileManager
//import com.mjaruijs.fischersplayground.util.Logger
//import java.util.Stack
//import java.util.concurrent.atomic.AtomicBoolean
//import kotlin.collections.ArrayList
//import kotlin.collections.HashMap
//
//class DataManagerWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {
//
//    private val savedPracticeSessions = ArrayList<PracticeSession>()
//    private val savedOpenings = ArrayList<Opening>()
//    private val savedGames = HashMap<String, MultiPlayerGame>()
//    private val savedInvites = HashMap<String, InviteData>()
//    private val recentOpponents = Stack<Pair<String, String>>()
//    private val handledMessages = HashSet<Long>()
//
//    private val practiceLock = AtomicBoolean(false)
//    private val openingLock = AtomicBoolean(false)
//    private val gamesLock = AtomicBoolean(false)
//    private val invitesLock = AtomicBoolean(false)
//    private val recentOpponentsLock = AtomicBoolean(false)
//    private val messageLock = AtomicBoolean(false)
//
//    override fun doWork(): Result {
//        val request = Request.fromString(inputData.getString("request")!!)
//
//        val output: Any = when (request) {
//            Request.LOAD_DATA -> loadData()
//            Request.SAVE_DATA -> saveData()
//            Request.GET_SAVED_OPENINGS -> getSavedOpenings()
//            Request.REMOVE_OPENING -> removeOpening()
//            Request.SET_OPENING -> setOpening()
//            Request.ADD_OPENING -> addOpening()
//            Request.GET_OPENING -> getOpening()
//            Request.GET_PRACTICE_SESSION -> getPracticeSession()
//            Request.SET_PRACTICE_SESSION -> setPracticeSession()
//            Request.REMOVE_PRACTICE_SESSION -> removePracticeSession()
//            Request.GET_SAVED_GAMES -> getSavedGames()
//            Request.REMOVE_GAME -> removeGame()
//            Request.GET_GAME -> getGame()
//            Request.SET_GAME-> setGame()
//            Request.GET_SAVED_INVITES -> getSavedInvites()
//            Request.SET_INVITE -> setInvite()
//            Request.REMOVE_INVITE -> removeSavedInvite()
//            Request.GET_RECENT_OPPONENTS -> getRecentOpponents()
////            Request.SET_RECENT_OPPONENTS -> setRecentOpponents()
//            Request.ADD_RECENT_OPPONENT -> addRecentOpponent()
//            Request.SET_MESSAGE_HANDLED -> setMessageHandled()
//            Request.IS_MESSAGE_HANDLED -> isMessageHandled()
//        }
//
//        return when (output) {
//            is Parcelable -> {
//                val dataBuilder = Data.Builder().putParcelable("output", output)
//                Result.success(dataBuilder.build())
//            }
//            is HashMap<*, *> -> {
//                val dataBuilder = Data.Builder().putMap("output", output)
//                Result.success(dataBuilder.build())
//            }
//            is ArrayList<*> -> {
//                val dataBuilder = Data.Builder().putList("output", output)
//                Result.success(dataBuilder.build())
//            }
//            else -> {
//                Result.success()
//            }
//        }
//    }
//
//    private fun Data.Builder.putParcelable(key: String, parcelable: Parcelable): Data.Builder {
//        val parcel = Parcel.obtain()
//        try {
//            parcelable.writeToParcel(parcel, 0)
//            putByteArray(key, parcel.marshall())
//        } catch (e: Exception) {
//            NetworkManager.getInstance().sendCrashReport("crash_data_manager_parcelable.txt", e.stackTraceToString(), applicationContext)
//        } finally {
//            parcel.recycle()
//        }
//        return this
//    }
//
//    private fun Data.Builder.putMap(key: String, map: HashMap<*, *>): Data.Builder {
//        val parcel = Parcel.obtain()
//        try {
//            parcel.writeMap(map)
//            putByteArray(key, parcel.marshall())
//        } catch (e: Exception) {
//            throw e
//        } finally {
//            parcel.recycle()
//        }
//        return this
//    }
//
//    private fun Data.Builder.putList(key: String, list: ArrayList<*>): Data.Builder {
//        val parcel = Parcel.obtain()
//        try {
//            parcel.writeList(list)
//            putByteArray(key, parcel.marshall())
//        } catch (e: Exception) {
//            throw e
//        } finally {
//            parcel.recycle()
//        }
//        return this
//    }
//
//    private inline fun <reified T> Data.getParcelable(type: Parcelable.Creator<*>?, key: String): T? {
//        val parcel = Parcel.obtain()
//        try {
//            val bytes = getByteArray(key) ?: return null
//            parcel.unmarshall(bytes, 0, bytes.size)
//            parcel.setDataPosition(0)
//
//            return type?.createFromParcel(parcel) as T
//        } finally {
//            parcel.recycle()
//        }
//    }
//
//    private fun getInputString(key: String): String {
//        return inputData.getString(key) ?: throw IllegalArgumentException("DataManagerWorker tried to get string with key $key from input, but it was missing..")
//    }
//
//    @Suppress("SameParameterValue")
//    private fun getInputLong(key: String): Long {
//        if (!inputData.hasKeyWithValueOfType<Long>(key)) {
//            throw IllegalArgumentException("DataManagerWorker tried to get long with key $key from input, but it was missing..")
//        }
//        return inputData.getLong(key, -1L)
//    }
//
//    private inline fun <reified T : Parcelable> getInputParcelable(type: Parcelable.Creator<*>?,  key: String): T {
//        return inputData.getParcelable(type, key) as T? ?: throw IllegalArgumentException("DataManagerWorker tried to get string with key $key from input, but it was missing..")
//    }
//
//    private fun getSavedOpenings(): ArrayList<Opening> {
//        obtainOpeningLock()
//
//        val openings = savedOpenings
//        unlockOpenings()
//
//        return openings
//    }
//
//    private fun removeOpening() {
//        val name = getInputString("opening_name")
//
//        obtainOpeningLock()
//
//        savedOpenings.removeIf { opening -> opening.name == name }
//
//        unlockOpenings()
//    }
//
//    private fun setOpening() {
//        val name = getInputString("opening_name")
//        val team = Team.fromString(getInputString("opening_team"))
//        val opening = getInputParcelable<Opening>(Opening, "opening")
//
//        obtainOpeningLock()
//
//        savedOpenings.removeIf { storedOpening ->
//            storedOpening.name == name && storedOpening.team == team
//        }
//
//        savedOpenings += opening
//        unlockOpenings()
//    }
//
//    private fun addOpening() {
//        val opening = getInputParcelable<Opening>(Opening, "opening")
//
//        obtainOpeningLock()
//        savedOpenings += opening
//        unlockOpenings()
//    }
//
//    private fun getOpening(): Parcelable {
//        val name = getInputString("opening_name")
//        val team = Team.fromString(getInputString("opening_team"))
//
//        obtainOpeningLock()
//
//        val opening = savedOpenings.find { opening -> opening.name == name && opening.team == team }
//        if (opening == null) {
//            val newOpening = Opening(name, team)
//
//            savedOpenings.add(newOpening)
//            unlockOpenings()
//            return newOpening
//        }
//
//        unlockOpenings()
//        return opening
//    }
//
//    private fun getPracticeSession(): Parcelable {
//        val name = getInputString("session_name")
//        val team = Team.fromString(getInputString("session_team"))
//
//        obtainPracticeSessionLock()
//
//        val session = savedPracticeSessions.find { session -> session.openingName == name && session.team == team }
//        unlockPracticeSessions()
//
//        if (session == null) {
//            return ParcelableNull()
//        }
//
//        return session
//    }
//
//    private fun setPracticeSession() {
//        val name = getInputString("session_name")
//        val practiceSession = getInputParcelable<PracticeSession>(PracticeSession, "session")
//
//        obtainPracticeSessionLock()
//
//        savedPracticeSessions.removeIf { session ->
//            session.openingName == name
//        }
//
//        savedPracticeSessions += practiceSession
//        unlockPracticeSessions()
//    }
//
//    private fun removePracticeSession() {
//        val name = getInputString("session_name")
//        val team = Team.fromString(getInputString("session_team"))
//
//        obtainPracticeSessionLock()
//        savedPracticeSessions.removeIf { session ->
//            session.openingName == name && session.team == team
//        }
//        FileManager.delete("practice_session_${name}_$team.txt")
//        unlockPracticeSessions()
//    }
//
//    private fun getSavedGames(): HashMap<String, MultiPlayerGame> {
//        obtainGameLock()
//
//        val games = savedGames
//        unlockGames()
//
//        Logger.debug(TAG,"Number of games at the moment the request came in: ${savedGames.size}")
//
//        return games
//    }
//
//    private fun removeGame() {
//        val id = getInputString("game_id")
//
//        obtainGameLock()
//
//        savedGames.remove(id)
//        unlockGames()
//    }
//
//    private fun getGame(): Parcelable {
//        val id = getInputString("game_id")
//
//        obtainGameLock()
//
//        val game = savedGames[id]
//        unlockGames()
//
//        if (game == null) {
//            return ParcelableNull()
//        }
//
//        return game
//    }
//
//    private fun setGame() {
//        val id = getInputString("game_id")
//        val game = getInputParcelable<MultiPlayerGame>(MultiPlayerGame, "game")
//
//        obtainGameLock()
//        savedGames[id] = game
//        unlockGames()
//    }
//
//    private fun getSavedInvites(): HashMap<String, InviteData> {
//        obtainInvitesLock()
//
//        val invites = savedInvites
//        unlockInvites()
//        return invites
//    }
//
//    private fun setInvite() {
//        val id = getInputString("invite_id")
//        val invite = getInputParcelable<InviteData>(InviteData, "invite")
//
//        obtainInvitesLock()
//        savedInvites[id] = invite
//        unlockInvites()
//    }
//
//    private fun removeSavedInvite() {
//        val id = getInputString("invite_id")
//
//        obtainInvitesLock()
//        savedInvites.remove(id)
//        unlockInvites()
//    }
//
//    private fun getRecentOpponents(): Stack<Pair<String, String>> {
//        obtainOpponentsLock()
//        val opponents = recentOpponents
//        unlockOpponents()
//        return opponents
//    }
//
//    fun setRecentOpponents(opponents: List<Pair<String, String>>) {
//        recentOpponents.clear()
//        for (opponent in opponents) {
//            recentOpponents.push(opponent)
//        }
//        saveRecentOpponents()
//    }
//
//    private fun addRecentOpponent() {
//        val pair = inputData.getParcelable<ParcelablePair<ParcelableString, ParcelableString>>(ParcelablePair, "new_opponent") ?: return
//        addRecentOpponent(Pair(pair.first.value, pair.second.value))
//    }
//
//    private fun addRecentOpponent(newOpponent: Pair<String, String>) {
//        val userId = applicationContext.getSharedPreferences("user_data", MODE_PRIVATE).getString(ClientActivity.USER_ID_KEY, "") ?: DEFAULT_USER_ID
//        if (newOpponent.second == userId) {
//            return
//        }
//
//        val temp = Stack<Pair<String, String>>()
//
//        while (temp.size < 2 && recentOpponents.isNotEmpty()) {
//            val opponent = recentOpponents.pop()
//
//            if (opponent == newOpponent) {
//                continue
//            }
//
//            temp.push(opponent)
//        }
//
//        while (recentOpponents.isNotEmpty()) {
//            recentOpponents.pop()
//        }
//
//        for (i in 0 until temp.size) {
//            recentOpponents.push(temp.pop())
//        }
//
//        recentOpponents.push(newOpponent)
//    }
//
//    fun isLocked() = areGamesLocked() || areInvitesLocked() || areOpponentsLocked() || areOpeningsLocked() || arePracticeSessionsLocked()
//
//    private fun arePracticeSessionsLocked() = practiceLock.get()
//
//    private fun areOpeningsLocked() = openingLock.get()
//
//    private fun areGamesLocked() = gamesLock.get()
//
//    private fun areInvitesLocked() = invitesLock.get()
//
//    private fun areOpponentsLocked() = recentOpponentsLock.get()
//
//    private fun areMessagesLocked() = messageLock.get()
//
//    private fun obtainPracticeSessionLock() {
//        while (arePracticeSessionsLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockPracticeSessions()
//    }
//
//    private fun obtainOpeningLock() {
//        while (areOpeningsLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockOpenings()
//    }
//
//    private fun obtainGameLock() {
//        while (areGamesLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockGames()
//    }
//
//    private fun obtainInvitesLock() {
//        while (areInvitesLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockInvites()
//    }
//
//    private fun obtainOpponentsLock() {
//        while (areOpponentsLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockOpponents()
//    }
//
//    private fun obtainMessageLock() {
//        while (areMessagesLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockMessages()
//    }
//
//    private fun lockPracticeSessions() {
//        practiceLock.set(true)
//    }
//
//    private fun unlockPracticeSessions() {
//        practiceLock.set(false)
//    }
//
//    private fun lockOpenings() {
//        openingLock.set(true)
//    }
//
//    private fun unlockOpenings() {
//        openingLock.set(false)
//    }
//
//    private fun lockGames() {
//        gamesLock.set(true)
//    }
//
//    private fun unlockGames() {
//        gamesLock.set(false)
//    }
//
//    private fun lockInvites() {
//        invitesLock.set(true)
//    }
//
//    private fun unlockInvites() {
//        invitesLock.set(false)
//    }
//
//    private fun lockOpponents() {
//        recentOpponentsLock.set(true)
//    }
//
//    private fun unlockOpponents() {
//        recentOpponentsLock.set(false)
//    }
//
//    private fun lockMessages() {
//        messageLock.set(true)
//    }
//
//    private fun unlockMessages() {
//        messageLock.set(false)
//    }
//
//    private fun setMessageHandled() {
//        val id = getInputLong("message_id")
//
//        obtainMessageLock()
//        handledMessages += id
//        unlockMessages()
//    }
//
//    private fun isMessageHandled(): Boolean {
//        val id = getInputLong("message_id")
//
//        obtainMessageLock()
//        val isHandled = handledMessages.contains(id)
//        unlockMessages()
//        return isHandled
//    }
//
//    fun loadData() {
//        Logger.warn(TAG, "LOADING DATA")
//        loadPracticeSessions()
//        loadSavedOpenings()
//        loadSavedGames()
//        loadInvites()
//        loadRecentOpponents()
//        loadHandledMessages()
//    }
//
//    fun saveData() {
//        saveOpenings()
//        saveGames()
//        saveInvites()
//        saveRecentOpponents()
//        saveHandledMessages()
//        savePracticeSessions()
//    }
//
//    private fun loadPracticeSessions() {
//        obtainPracticeSessionLock()
//        Thread {
//            try {
//                val files = FileManager.listFilesInDirectory()
//
//                val practiceFiles = files.filter { fileName -> fileName.startsWith("practice_session_") }
//
//                savedPracticeSessions.clear()
//
//                for (practiceFileName in practiceFiles) {
//                    val fileContent = FileManager.readText(applicationContext, practiceFileName) ?: continue
//
//                    val practiceSession = PracticeSession.fromString(fileContent)
//                    savedPracticeSessions += practiceSession
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_loading_practice_sessions.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockPracticeSessions()
//            }
//        }.start()
//
//    }
//
//    private fun loadSavedOpenings() {
//        obtainOpeningLock()
//        Thread {
//            try {
//                val files = FileManager.listFilesInDirectory()
//
//                val openingFiles = files.filter { fileName -> fileName.startsWith("opening_") }
//                savedOpenings.clear()
//
//                for (openingFileName in openingFiles) {
//                    val fileContent = FileManager.readText(applicationContext, openingFileName) ?: continue
//
//                    val openingInfo = openingFileName.removePrefix("opening_").removeSuffix(".txt").split("_")
//                    val openingName = openingInfo[0]
//                    val openingTeam = Team.fromString(openingInfo[1])
//                    val opening = Opening(openingName, openingTeam)
//                    opening.addFromString(fileContent)
//
//                    savedOpenings += opening
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_loading_opening.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockOpenings()
//            }
//        }.start()
//
//    }
//
//    private fun loadSavedGames() {
//        obtainGameLock()
//        Thread {
//            try {
//                savedGames.clear()
//                val lines = FileManager.readLines(applicationContext, MULTIPLAYER_GAME_FILE) ?: ArrayList()
//
//                for (gameData in lines) {
//                    if (gameData.isBlank()) {
//                        continue
//                    }
//
//                    val data = gameData.removePrefix("(").removeSuffix(")").split('|')
//                    val gameId = data[0]
//                    val opponentId = data[1]
//                    val opponentName = data[2]
//                    val gameStatus = GameStatus.fromString(data[3])
//                    val opponentStatus = data[4]
//                    val lastUpdated = data[5].toLong()
//                    val isPlayerWhite = data[6].toBoolean()
//                    val moveToBeConfirmed = data[7]
//                    val moveList = data[8].removePrefix("[").removeSuffix("]").split('\\')
//                    val chatMessages = data[9].removePrefix("[").removeSuffix("]").split('\\')
//                    val newsData = data[10].removePrefix("[").removeSuffix("]").split("\\")
//
//                    val moves = ArrayList<Move>()
//
//                    for (move in moveList) {
//                        if (move.isNotBlank()) {
//                            moves += Move.fromChessNotation(move)
//                        }
//                    }
//
//                    val messages = ArrayList<ChatMessage>()
//                    for (message in chatMessages) {
//                        if (message.isNotBlank()) {
//                            val messageData = message.split('~')
//                            val timeStamp = messageData[0]
//                            val messageContent = messageData[1]
//                            val type = MessageType.fromString(messageData[2])
//
//                            messages += ChatMessage(gameId, timeStamp, messageContent, type)
//                        }
//                    }
//
//                    val newsUpdates = ArrayList<News>()
//                    for (news in newsData) {
//                        if (news.isBlank()) {
//                            continue
//                        }
//
//                        when (news.count { char -> char == ',' }) {
//                            0 -> newsUpdates += News.fromString(news)
//                            1 -> newsUpdates += IntNews.fromString(news)
//                            else -> newsUpdates += MoveNews.fromString(news)
//                        }
//                    }
//
//                    val newGame = MultiPlayerGame(gameId, opponentId, opponentName, gameStatus, opponentStatus, lastUpdated, isPlayerWhite, moveToBeConfirmed, moves, messages, newsUpdates)
//                    newGame.status = gameStatus
//                    savedGames[gameId] = newGame
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_games_loading.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockGames()
//                Logger.debug(TAG, "Number of games loaded: ${savedGames.size}")
//            }
//        }.start()
//    }
//
//    private fun loadInvites() {
//        obtainInvitesLock()
//        Thread {
//            try {
//                savedInvites.clear()
//                val lines = FileManager.readLines(applicationContext, INVITES_FILE) ?: ArrayList()
//
//                for (line in lines) {
//                    if (line.isBlank()) {
//                        continue
//                    }
//
//                    val data = line.split('|')
//                    val inviteId = data[0]
//                    val opponentName = data[1]
//                    val timeStamp = data[2].toLong()
//                    val type = InviteType.fromString(data[3])
//
//                    savedInvites[inviteId] = InviteData(inviteId, opponentName, timeStamp, type)
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_invites_loading.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockInvites()
//            }
//        }.start()
//    }
//
//    private fun loadRecentOpponents() {
//        obtainOpponentsLock()
//        Thread {
//            try {
//                recentOpponents.clear()
//                val lines = FileManager.readLines(applicationContext, RECENT_OPPONENTS_FILE) ?: ArrayList()
//
//                for (line in lines) {
//                    if (line.isBlank()) {
//                        continue
//                    }
//
//                    val data = line.split('|')
//                    val opponentName = data[0]
//                    val opponentId = data[1]
//                    addRecentOpponent(Pair(opponentName, opponentId))
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_opponents_loading.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockOpponents()
//            }
//        }.start()
//    }
//
//    private fun loadHandledMessages() {
//        obtainMessageLock()
//        Thread {
//            try {
//                handledMessages.clear()
//                val lines = FileManager.readLines(applicationContext, HANDLED_MESSAGES_FILE) ?: ArrayList()
//
//                for (messageId in lines) {
//                    if (messageId.isBlank()) {
//                        continue
//                    }
//
//                    handledMessages += messageId.toLong()
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_messages_loading.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockMessages()
//            }
//        }.start()
//    }
//
////    fun saveOpening(name: String, team: Team, opening: Opening, context: Context) {
////        obtainOpeningLock()
////
////        savedOpenings.removeIf { storedOpening ->
////            storedOpening.name == name && storedOpening.team == team
////        }
////
////        savedOpenings += opening
////        unlockOpenings()
////    }
//
//    fun savePracticeSessions() {
//        obtainPracticeSessionLock()
//        Thread {
//            try {
//                for (practiceSession in savedPracticeSessions) {
//                    Logger.debug(TAG, "Saving session: $practiceSession")
//                    FileManager.write(applicationContext, "practice_session_${practiceSession.openingName}_${practiceSession.team}.txt", practiceSession.toString())
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_practice_sessions_saving.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockPracticeSessions()
//            }
//        }.start()
//    }
//
//    fun saveOpenings() {
//        obtainOpeningLock()
//        Thread {
//            try {
//                for (opening in savedOpenings) {
//                    Logger.debug(TAG, "Saving opening with name: opening_${opening.name}_${opening.team}.txt")
//                    FileManager.write(applicationContext, "opening_${opening.name}_${opening.team}.txt", opening.toString())
//                }
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_openings_saving.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockOpenings()
//            }
//
//        }.start()
//    }
//
//    fun saveGames() {
//        obtainGameLock()
//        Thread {
//            try {
//                var content = ""
//                for ((gameId, game) in savedGames) {
//                    var moveData = "["
//
//                    for ((i, move) in game.moves.withIndex()) {
//                        moveData += move.toChessNotation()
//                        if (i != game.moves.size - 1) {
//                            moveData += "\\"
//                        }
//                    }
//                    moveData += "]"
//
//                    var chatData = "["
//
//                    for ((i, message) in game.chatMessages.withIndex()) {
//                        chatData += message.toString()
//                        if (i != game.chatMessages.size - 1) {
//                            chatData += "\\"
//                        }
//                    }
//                    chatData += "]"
//
//                    var newsContent = "["
//
//                    for ((i, news) in game.newsUpdates.withIndex()) {
//                        newsContent += news.toString()
//                        if (i != game.newsUpdates.size - 1) {
//                            newsContent += "\\"
//                        }
//                    }
//                    newsContent += "]"
//
//                    val gameContent = "$gameId|${game.opponentId}|${game.opponentName}|${game.status}|${game.opponentStatus}|${game.lastUpdated}|${game.isPlayingWhite}|${game.moveToBeConfirmed}|$moveData|$chatData|$newsContent\n"
//                    content += gameContent
//                }
//
//                FileManager.write(applicationContext, MULTIPLAYER_GAME_FILE, content)
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_games_saving.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockGames()
//            }
//        }.start()
//    }
//
//    fun saveInvites() {
//        obtainInvitesLock()
//        Thread {
//            try {
//                var content = ""
//
//                for ((inviteId, invite) in savedInvites) {
//                    content += "$inviteId|${invite.opponentName}|${invite.timeStamp}|${invite.type}\n"
//                }
//
//                Logger.debug(TAG, "Saving invites: ${savedInvites.size}")
//
//                FileManager.write(applicationContext, INVITES_FILE, content)
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_invites_saving.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockInvites()
//            }
//        }.start()
//    }
//
//    private fun saveRecentOpponents() {
//        obtainOpponentsLock()
//        Thread {
//            try {
//                var data = ""
//                for (recentOpponent in recentOpponents) {
//                    data += "${recentOpponent.first}|${recentOpponent.second}\n"
//                }
//                FileManager.write(applicationContext, RECENT_OPPONENTS_FILE, data)
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_opponents_saving.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockOpponents()
//            }
//        }.start()
//    }
//
//    private fun saveHandledMessages() {
//        obtainMessageLock()
//        Thread {
//            try {
//                var data = ""
//
//                for (messageId in handledMessages) {
//                    data += "$messageId\n"
//                }
//                FileManager.write(applicationContext, HANDLED_MESSAGES_FILE, data)
//            } catch (e: Exception) {
//                NetworkManager.getInstance().sendCrashReport("crash_messages_saving.txt", e.stackTraceToString(), applicationContext)
//            } finally {
//                unlockMessages()
//            }
//        }.start()
//    }
//
//    companion object {
//        private const val TAG = "DataManagerWorker"
//
//        const val MULTIPLAYER_GAME_FILE = "mp_games.txt"
//        const val INVITES_FILE = "invites.txt"
//        const val RECENT_OPPONENTS_FILE = "recent_opponents.txt"
//        const val HANDLED_MESSAGES_FILE = "handled_messages.txt"
//    }
//
//    enum class Request {
//        LOAD_DATA,
//        SAVE_DATA,
//        GET_SAVED_OPENINGS,
//        REMOVE_OPENING,
//        SET_OPENING,
//        ADD_OPENING,
//        GET_OPENING,
//        GET_PRACTICE_SESSION,
//        SET_PRACTICE_SESSION,
//        REMOVE_PRACTICE_SESSION,
//        GET_SAVED_GAMES,
//        REMOVE_GAME,
//        GET_GAME,
//        SET_GAME,
//        GET_SAVED_INVITES,
//        SET_INVITE,
//        REMOVE_INVITE,
//        ADD_RECENT_OPPONENT,
//        GET_RECENT_OPPONENTS,
////        SET_RECENT_OPPONENTS,
//        SET_MESSAGE_HANDLED,
//        IS_MESSAGE_HANDLED;
//
//        companion object {
//
//            fun fromString(content: String): Request {
//                for (value in values()) {
//                    if (value.toString().uppercase() == content.uppercase()) {
//                        return value
//                    }
//                }
//                throw IllegalArgumentException("Failed to create a DataManagerWorker.Request from content: $content")
//            }
//
//        }
//
//    }
//}