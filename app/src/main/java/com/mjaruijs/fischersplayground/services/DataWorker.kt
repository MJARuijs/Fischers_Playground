//package com.mjaruijs.fischersplayground.services
//
//import android.app.Service
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.ServiceConnection
//import android.os.*
//import androidx.work.Data
//import androidx.work.ListenableWorker
//import com.mjaruijs.fischersplayground.activities.ClientActivity
//import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
//import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
//import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
//import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
//import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
//import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
//import com.mjaruijs.fischersplayground.chess.game.Move
//import com.mjaruijs.fischersplayground.chess.game.MoveData
//import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
//import com.mjaruijs.fischersplayground.chess.game.OpponentData
//import com.mjaruijs.fischersplayground.chess.news.IntNews
//import com.mjaruijs.fischersplayground.chess.news.MoveNews
//import com.mjaruijs.fischersplayground.chess.news.NewsType
//import com.mjaruijs.fischersplayground.networking.message.Topic
//import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
//import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
//import com.mjaruijs.fischersplayground.parcelable.ParcelableString
//import com.mjaruijs.fischersplayground.util.FileManager
//import com.mjaruijs.fischersplayground.util.Logger
//import java.util.concurrent.atomic.AtomicBoolean
//
//class DataWorker(val applicationContext: Context, val data: Bundle, val onResult: (Parcelable) -> Unit) : Thread() {
//
//    private lateinit var userId: String
////    private lateinit var dataManager: DataManager
//
//    private val isFinished = AtomicBoolean(false)
//
//    private lateinit var dataMessengerClient: Messenger
//    var dataServiceMessenger: Messenger? = null
//
//    private val dataMessengerConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            if (service == null) {
//                return
//            }
//
//            dataServiceMessenger = Messenger(service)
//
////            val registrationMessage = Message.obtain()
////            registrationMessage.what = 0
////            registrationMessage.replyTo = dataMessengerClient
////            dataServiceMessenger!!.send(registrationMessage)
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            dataServiceMessenger = null
//        }
//    }
//
//    override fun run() {
//        Logger.debug(TAG, "Created worker on thread ${currentThread().id}")
////        try {
////            Looper.prepare()
////        } catch (e: Exception) {
////            Logger.error(TAG, e.stackTraceToString())
////        } finally {
//            dataMessengerClient = Messenger(DataManagerHandler())
////        }
//
//        val preferences = applicationContext.getSharedPreferences(ClientActivity.USER_PREFERENCE_FILE, Service.MODE_PRIVATE)
//        userId = preferences.getString(ClientActivity.USER_ID_KEY, ClientActivity.DEFAULT_USER_ID)!!
//
//        applicationContext.bindService(Intent(applicationContext, DataManagerService::class.java), dataMessengerConnection, Context.BIND_AUTO_CREATE)
//
////        dataManager = DataManager.getInstance(applicationContext)
//
//        val topic = Topic.fromString(data.getString("topic")!!)
//        val content = data.getStringArray("content")!!
//        val messageId = data.getLong("messageId", -1L)
//
////        Looper.prepare()
////        Toast.makeText(applicationContext, "Got message: ${topic}", Toast.LENGTH_SHORT).show()
//
//        Logger.debug(TAG, "Start doing work on topic: $topic on thread ${Thread.currentThread().id}")
//
//        if (messageId == -1L) {
//            return
//        }
//
//        var output: Any = Unit
//
////        Thread {
////            sendToDataManager<ArrayList<MultiPlayerGame>>(DataManagerService.Request.GET_SAVED_GAMES) {
////                Logger.debug(TAG, "Got reply!")
////                sendToDataManager<ArrayList<MultiPlayerGame>>(DataManagerService.Request.GET_SAVED_GAMES) {
////                    Logger.debug(TAG, "Got second reply!")
////
//////                isFinished.set(true)
////                }
//////                isFinished.set(true)
////            }
//
////        }.start()
//
////        Thread {
//            output = when (topic) {
//                Topic.INVITE -> onIncomingInvite(content)
//                Topic.NEW_GAME -> onNewGameStarted(content)
//                Topic.MOVE -> onOpponentMoved(content)
//                Topic.UNDO_REQUESTED -> onUndoRequested(content)
//                Topic.UNDO_ACCEPTED -> onUndoAccepted(content)
//                Topic.UNDO_REJECTED -> onUndoRejected(content)
//                Topic.RESIGN -> onOpponentResigned(content)
//                Topic.DRAW_OFFERED -> onDrawOffered(content)
//                Topic.DRAW_ACCEPTED -> onDrawAccepted(content)
//                Topic.DRAW_REJECTED -> onDrawRejected(content)
//                Topic.CHAT_MESSAGE -> onChatMessageReceived(content)
//                Topic.USER_STATUS_CHANGED -> onUserStatusChanged(content)
//                Topic.COMPARE_DATA -> onCompareData(content)
//                Topic.RESTORE_DATA -> onRestoreData(content)
//                Topic.DEBUG -> {}
//                else -> throw IllegalArgumentException("Could not parse content with unknown topic: $topic")
//            }
////        }.start()
//
////        while (!isFinished.get()) {
////            Thread.sleep(100)
////            Logger.debug(TAG, "Waiting for work to finish: $topic on thread ${Thread.currentThread().id}")
////        }
//
//        if (output is Parcelable) {
//            onResult(output as Parcelable)
//        }
//
//        Logger.debug(TAG, "Finished work on topic: $topic on thread ${Thread.currentThread().id}")
//
//    }
//    private fun Data.Builder.putParcelable(key: String, parcelable: Parcelable): Data.Builder {
//        val parcel = Parcel.obtain()
//        try {
//            parcelable.writeToParcel(parcel, 0)
//            putByteArray(key, parcel.marshall())
//        } catch (e: Exception) {
//            NetworkService.sendCrashReport("crash_data_worker_parcelable.txt", e.stackTraceToString(), applicationContext)
//        } finally {
//            parcel.recycle()
//        }
//        return this
//    }
//
//    private inline fun <reified T>sendToDataManager(request: DataManagerService.Request, vararg extraData: Pair<String, *>, noinline onResult: (T) -> Unit) {
//        val message = Message.obtain()
//        message.data.putString("request", request.toString())
//        message.obj = Pair(T::class.java, onResult)
//        message.what = 1
//        message.replyTo = dataMessengerClient
//        addDataToMessage(message, *extraData)
//
//        sendToDataManager(message)
//    }
//
//    private fun sendToDataManager(request: DataManagerService.Request, vararg extraData: Pair<String, *>) {
//        val message = Message.obtain()
//        message.what = 1
//        message.data.putString("request", request.toString())
//        message.replyTo = dataMessengerClient
//        addDataToMessage(message, *extraData)
//
//        sendToDataManager(message)
//    }
//
//    private fun sendToDataManager(message: Message) {
//        if (dataServiceMessenger != null) {
//            Logger.debug(TAG, "Sending message to dataService: ${message.data.getString("request")}")
//
//            dataServiceMessenger!!.send(message)
//        } else {
//            while (dataServiceMessenger == null) {
//                Thread.sleep(10)
//                Logger.debug(TAG, "Waiting for dataMessenger on thread ${Thread.currentThread().id}")
//            }
//
//            Logger.debug(TAG, "Sending message to dataService: ${message.data.getString("request")}")
//            dataServiceMessenger!!.send(message)
//        }
//    }
//
//    private fun addDataToMessage(message: Message, vararg extraData: Pair<String, *>) {
//        val dataBundle = Bundle()
//        for (data in extraData) {
//            if (data.second is String) {
//                dataBundle.putString(data.first, data.second as String)
//            } else if (data.second is Parcelable) {
//                dataBundle.putParcelable(data.first, data.second as Parcelable)
//            } else if (data.second is ArrayList<*> && (data.second as ArrayList<*>).all { item -> item is Parcelable }) {
//                dataBundle.putParcelableArrayList(data.first, data.second as ArrayList<out Parcelable>)
//            }
//        }
//        message.data.putBundle("data", dataBundle)
//    }
//
//    private fun onOpponentMoved(data: Array<String>): Parcelable {
//        val gameId = data[0]
//        val moveNotation = data[1]
//        val timeStamp = data[2].toLong()
//        val move = Move.fromChessNotation(moveNotation)
//
//        val moveData = MoveData(gameId, GameStatus.PLAYER_MOVE, timeStamp, move)
//
//        try {
//            sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//                Logger.debug(TAG, "Adding news to game: OPPONENT_MOVED")
//                game.addNews(MoveNews(NewsType.OPPONENT_MOVED, moveData))
//                game.lastUpdated = timeStamp
//                try {
//                    sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", game.gameId), Pair("game", game))
//                } catch (e: Exception) {
//                    Logger.error(TAG, e.stackTraceToString())
//                } finally {
//                    isFinished.set(true)
//                }
////                dataManager.setGame(gameId, game)
//            }
//
////            val game = dataManager.getGame(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//////            game.moveOpponent(move, true)
////            game.addNews(MoveNews(NewsType.OPPONENT_MOVED, moveData))
////            Logger.debug(TAG, "Adding news to game: OPPONENT_MOVED")
////            game.lastUpdated = timeStamp
////            dataManager.setGame(gameId, game)
//        } catch (e: Exception) {
//            Logger.error(TAG, e.stackTraceToString())
//            NetworkService.sendCrashReport("crash_data_worker_on_opponent_moved.txt", e.stackTraceToString(), applicationContext)
//        }
//
//        return moveData
//    }
//
//    private fun onIncomingInvite(data: Array<String>): Parcelable {
//        val opponentName = data[0]
//        val inviteId = data[1]
//        val timeStamp = data[2].toLong()
//
//        val inviteData = InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
//
//        sendToDataManager(DataManagerService.Request.SET_INVITE, Pair("invite", inviteData))
////        dataManager.setInvite(inviteId, inviteData)
//        isFinished.set(true)
//
//        return InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
//    }
//
//    private fun onNewGameStarted(data: Array<String>): Parcelable {
//        val inviteId = data[0]
//        val opponentName = data[1]
//        val opponentId = data[2]
//        val opponentStatus = data[3]
//        val playingWhite = data[4].toBoolean()
//        val timeStamp = data[5].toLong()
//
//        val gameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
//
//        val newGame = MultiPlayerGame(inviteId, opponentId, opponentName, gameStatus, opponentStatus, timeStamp, playingWhite)
//        newGame.lastUpdated = timeStamp
//
//        sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", inviteId), Pair("game", newGame))
//        sendToDataManager(DataManagerService.Request.REMOVE_INVITE, Pair("invite_id", inviteId))
//        sendToDataManager(DataManagerService.Request.ADD_RECENT_OPPONENT, Pair("opponent_data", OpponentData(opponentName, opponentId)))
////        dataManager.setGame(inviteId, newGame)
////        dataManager.removeSavedInvite(inviteId)
////        dataManager.addRecentOpponent(applicationContext, Pair(opponentName, opponentId))
//        isFinished.set(true)
//
//        val hasUpdate = gameStatus == GameStatus.PLAYER_MOVE
//
//        return GameCardItem(inviteId, timeStamp, opponentName, gameStatus, hasUpdate = hasUpdate)
//    }
//
//    private fun onUndoRequested(data: Array<String>): ParcelableString {
//        val gameId = data[0]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addNews(NewsType.OPPONENT_REQUESTED_UNDO)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//            isFinished.set(true)
//
//        }
//
////        val game = dataManager.getGame(gameId)!!
////        game.addNews(NewsType.OPPONENT_REQUESTED_UNDO)
////        dataManager.setGame(gameId, game)
//
//        return ParcelableString(gameId)
//    }
//
//    private fun onUndoAccepted(data: Array<String>): ParcelablePair<ParcelableString, ParcelableInt> {
//        val gameId = data[0]
//        val numberOfReversedMoves = data[1].toInt()
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addNews(IntNews(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfReversedMoves))
//            game.status = GameStatus.PLAYER_MOVE
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
////        val game = dataManager.getGame(gameId)!!
//
//
////        dataManager.setGame(gameId, game)
//
//        return ParcelablePair(ParcelableString(gameId), ParcelableInt(numberOfReversedMoves))
//    }
//
//    private fun onUndoRejected(data: Array<String>): ParcelableString {
//        val gameId = data[0]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addNews(NewsType.OPPONENT_REJECTED_UNDO)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
////        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_REJECTED_UNDO)
//
//        return ParcelableString(gameId)
//    }
//
//    private fun onOpponentResigned(data: Array<String>): ParcelableString {
//        val gameId = data[0]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addNews(NewsType.OPPONENT_RESIGNED)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
////        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_RESIGNED)
//
//        return ParcelableString(gameId)
//    }
//
//    private fun onDrawOffered(data: Array<String>): ParcelableString {
//        val gameId = data[0]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addNews(NewsType.OPPONENT_OFFERED_DRAW)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
////        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_OFFERED_DRAW)
//
//        return ParcelableString(gameId)
//    }
//
//    private fun onDrawAccepted(data: Array<String>): ParcelableString {
//        val gameId = data[0]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.status = GameStatus.GAME_DRAW
//            game.addNews(NewsType.OPPONENT_ACCEPTED_DRAW)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
//
////        val game = dataManager.getGame(gameId)!!
////        dataManager.setGame(gameId, game)
//
//        return ParcelableString(gameId)
//    }
//
//    private fun onDrawRejected(data: Array<String>): ParcelableString {
//        val gameId = data[0]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addNews(NewsType.OPPONENT_REJECTED_DRAW)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
//
////        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_REJECTED_DRAW)
//
//        return ParcelableString(gameId)
//    }
//
//    private fun onChatMessageReceived(data: Array<String>): ChatMessage {
//        val gameId = data[0]
//        val timeStamp = data[1]
//        val messageContent = data[2]
//
//        sendToDataManager<MultiPlayerGame>(DataManagerService.Request.GET_GAME, Pair("game_id", gameId)) { game ->
//            game.addMessage(ChatMessage(gameId, timeStamp, messageContent, MessageType.RECEIVED))
//            game.addNews(NewsType.CHAT_MESSAGE)
//            sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
//        }
//
////        dataManager.getGame(gameId)!!.addMessage()
////        dataManager.getGame(gameId)!!.addNews(NewsType.CHAT_MESSAGE)
//
//        return ChatMessage(gameId, timeStamp, messageContent, MessageType.RECEIVED)
//    }
//
//    private fun onUserStatusChanged(data: Array<String>): ParcelableString {
//        val opponentId = data[0]
//        val opponentStatus = data[1]
//
//        sendToDataManager<ArrayList<MultiPlayerGame>>(DataManagerService.Request.GET_SAVED_GAMES) { games ->
//            for (game in games) {
//                if (game.opponentId == opponentId) {
//                    game.opponentStatus = opponentStatus
//                    sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", game.gameId), Pair("game", game))
////                    dataManager.getGame(game.gameId)!!.opponentStatus = opponentStatus
////                game.opponentStatus = opponentStatus
////                dataManager[gameId] = game
//                }
//            }
//        }
//
//        return ParcelableString(opponentStatus)
//    }
//
//    private fun onCompareData(data: Array<String>): ParcelableString {
//        val localFiles = FileManager.listFilesInDirectory()
//
//        val missingData = ArrayList<String>()
//
//        for (serverData in data) {
//            if (serverData.startsWith("opening:")) {
//                var missingOpeningsString = "opening:"
//                val openingFiles = localFiles.filter { fileName -> fileName.startsWith("opening_") }.map { openingName -> openingName.removePrefix("opening_") }
//
//                val serverFiles = parseServerFiles(serverData)
//                for (serverFile in serverFiles) {
//                    if (serverFile.isBlank()) {
//                        continue
//                    }
//
//                    if (serverFile.startsWith(".") && serverFile.endsWith(".txt.swp")) {
//                        continue
//                    }
//
//                    if (!openingFiles.contains(serverFile)) {
//                        missingOpeningsString += "$serverFile%"
//                    }
//                }
//
//                missingData += missingOpeningsString.removeSuffix("%")
//            } else if (serverData.startsWith("practice_session:")) {
//                var missingPracticeSessionString = "practice_session:"
//                val practiceFiles = localFiles.filter { fileName -> fileName.startsWith("practice_session_") }.map { openingName -> openingName.removePrefix("practice_session_") }
//
//                val serverFiles = parseServerFiles(serverData)
//                for (serverFile in serverFiles) {
//                    if (serverFile.isBlank()) {
//                        continue
//                    }
//                    if (!practiceFiles.contains(serverFile)) {
//                        missingPracticeSessionString += "$serverFile%"
//                    }
//                }
//
//                missingData += missingPracticeSessionString.removeSuffix("%")
//            } else if (serverData.startsWith("multiplayer_games:")) {
//                var missingGamesString = "multiplayer_games:"
//                val serverGames = parseServerFiles(serverData)
//
//                val mpGames = FileManager.readLines(applicationContext, "mp_games.txt") ?: ArrayList()
//                val mpGameIds = ArrayList<String>()
//                for (gameLine in mpGames) {
//                    val gameId = gameLine.split("|").first()
//                    mpGameIds += gameId
//                }
//
//                for (serverGame in serverGames) {
//                    if (!mpGameIds.contains(serverGame)) {
//                        missingGamesString += "$serverGame%"
//                    }
//                }
//
//                missingData += missingGamesString.removeSuffix("%")
//            }
//        }
//        isFinished.set(true)
//
//        return ParcelableString(missingData.joinToString("|"))
////        NetworkManager.getInstance().sendMessage(NetworkMessage(Topic.RESTORE_DATA, "$userId|${missingData.joinToString("|")}"))
//    }
//
//    private fun onRestoreData(data: Array<String>) {
//        for (serverData in data) {
//            val separatorIndex = serverData.indexOf(":")
//            val dataType = serverData.substring(0, separatorIndex)
//
//            if (dataType == "multiplayer_games") {
//                val gamesData = serverData.substring(separatorIndex + 1).split("%")
//
//                for (gameData in gamesData) {
//                    if (gameData.isBlank()) {
//                        continue
//                    }
//
//                    val game = MultiPlayerGame.parseFromServer(gameData, userId)
//                    sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", game.gameId), Pair("game", game))
////                    dataManager.setGame(game.gameId, game)
//                }
//
////                dataManager.saveGames(applicationContext)
//            } else if (dataType == "invites") {
//                val invitesData = serverData.substring(separatorIndex + 1).split("%")
//
//                for (inviteData in invitesData) {
//                    if (inviteData.isBlank()) {
//                        continue
//                    }
//
//                    sendToDataManager(DataManagerService.Request.SET_INVITE, Pair("invite", InviteData.fromString(inviteData)))
////                    dataManager.setInvite(invite.inviteId, invite)
//                }
////                dataManager.saveInvites(applicationContext)
//            } else if (dataType == "recent_opponents") {
//                val opponents = serverData.substring(separatorIndex + 1).split("%")
//
//                val recentOpponents = ArrayList<OpponentData>()
//                for (opponentData in opponents) {
//                    if (opponentData.isBlank()) {
//                        continue
//                    }
//
//                    val opponentSeparatorIndex = opponentData.indexOf("@#!")
//                    val opponentName = opponentData.substring(0, opponentSeparatorIndex)
//                    val opponentId = opponentData.substring(opponentSeparatorIndex + 3)
//                    recentOpponents += OpponentData(opponentName, opponentId)
////                    sendToDataManager(DataManagerService.Request.ADD_RECENT_OPPONENT, Pair("opponent_data", OpponentData(opponentName, opponentId)))
////                    dataManager.addRecentOpponent(applicationContext, Pair(opponentName, opponentId))
//                }
//
//                sendToDataManager(DataManagerService.Request.SET_RECENT_OPPONENTS, Pair("opponents", recentOpponents))
////                dataManager.setRecentOpponents(applicationContext, recentOpponents)
//            } else {
//                val filesData = serverData.substring(separatorIndex + 1).split("%")
//
//                for (fileData in filesData) {
//                    if (fileData.isBlank()) {
//                        continue
//                    }
//
//                    val fileSeparatorIndex = fileData.indexOf("@#!")
//                    val fileName = fileData.substring(0, fileSeparatorIndex)
//                    val fileContent = fileData.substring(fileSeparatorIndex + 3)
//                    FileManager.write(applicationContext, "${dataType}_$fileName.txt", fileContent)
//                }
//            }
//        }
//
//        sendToDataManager(DataManagerService.Request.LOAD_DATA)
//        isFinished.set(true)
//
////        dataManager.loadData(applicationContext)
//    }
//
//    private fun parseServerFiles(serverData: String): List<String> {
//        val separatorIndex = serverData.indexOf(':')
//        val filesString = serverData.substring(separatorIndex + 1)
//
//        return filesString.split("%").toList()
//    }
//
//    private fun Data.getParcelable(type: Parcelable.Creator<*>?, key: String): Parcelable? {
//        val parcel = Parcel.obtain()
//        try {
//            val bytes = getByteArray(key) ?: return null
//            parcel.unmarshall(bytes, 0, bytes.size)
//            parcel.setDataPosition(0)
//
//            return type?.createFromParcel(parcel) as Parcelable?
//        } finally {
//            parcel.recycle()
//        }
//    }
//    class DataManagerHandler : Handler(Looper.getMainLooper()) {
//
//        override fun handleMessage(msg: Message) {
//
//            Logger.debug(TAG, "GOT REPLY")
//            val pair = (msg.obj as Pair<Any, (Any?) -> Unit>)
//
//            if (msg.what == 1) {
//                @Suppress("DEPRECATION")
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//                    pair.second.invoke(msg.data.getParcelable("output"))
//                } else {
//                    pair.second.invoke(msg.data.getParcelable("output", Parcelable::class.java))
//                }
//            } else if (msg.what == 2) {
//                @Suppress("DEPRECATION")
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//                    pair.second.invoke(msg.data.getParcelableArrayList<Parcelable>("output"))
//                } else {
//                    pair.second.invoke(msg.data.getParcelableArrayList("output", Parcelable::class.java))
//                }
//            }
//        }
//    }
//
//    companion object {
//        private const val TAG = "DataWorker"
//    }
//
//}