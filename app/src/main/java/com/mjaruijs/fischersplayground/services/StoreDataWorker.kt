package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.work.*
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.USER_PREFERENCE_FILE
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.IntNews
import com.mjaruijs.fischersplayground.chess.news.MoveNews
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveData
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
import com.mjaruijs.fischersplayground.parcelable.ParcelableNull
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger

class StoreDataWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

    private lateinit var userId: String
    private lateinit var dataManager: DataManager

    override fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences(USER_PREFERENCE_FILE, Service.MODE_PRIVATE)
        userId = preferences.getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!

        dataManager = DataManager.getInstance(applicationContext)

        val topic = Topic.fromString(inputData.getString("topic")!!)
        val content = inputData.getStringArray("content")!!
        val messageId = inputData.getLong("messageId", -1L)

//        Logger.debug(TAG, "Start doing work on topic: $topic. $content")

        if (messageId == -1L) {
            return Result.failure()
        }

        val output: Any = when (topic) {
            Topic.INVITE -> onIncomingInvite(content)
            Topic.NEW_GAME -> onNewGameStarted(content)
            Topic.MOVE -> onOpponentMoved(content)
            Topic.UNDO_REQUESTED -> onUndoRequested(content)
            Topic.UNDO_ACCEPTED -> onUndoAccepted(content)
            Topic.UNDO_REJECTED -> onUndoRejected(content)
            Topic.RESIGN -> onOpponentResigned(content)
            Topic.DRAW_OFFERED -> onDrawOffered(content)
            Topic.DRAW_ACCEPTED -> onDrawAccepted(content)
            Topic.DRAW_REJECTED -> onDrawRejected(content)
            Topic.CHAT_MESSAGE -> onChatMessageReceived(content)
            Topic.USER_STATUS_CHANGED -> onUserStatusChanged(content)
            Topic.COMPARE_DATA -> onCompareData(content)
            Topic.RESTORE_DATA -> onRestoreData(content)
            else -> throw IllegalArgumentException("Could not parse content with unknown topic: $topic")
        }

//        Logger.debug(TAG, "Finished work on topic: $topic")

        dataManager.handledMessage(messageId)
        dataManager.saveData(applicationContext)

        return if (output is Parcelable) {
            val dataBuilder = Data.Builder().putParcelable("output", output)
            Result.success(dataBuilder.build())
        } else {
            Result.success()
        }
    }

    private fun Data.Builder.putParcelable(key: String, parcelable: Parcelable): Data.Builder {
        val parcel = Parcel.obtain()
        try {
            parcelable.writeToParcel(parcel, 0)
            putByteArray(key, parcel.marshall())
        } catch (e: Exception) {
            NetworkService.sendCrashReport("crash_data_worker_parcelable.txt", e.stackTraceToString(), applicationContext)
        } finally {
            parcel.recycle()
        }
        return this
    }

    private fun sendToDataManager(request: DataManagerService.Request, vararg extraData: Pair<String, *>) {
        val dataBundle = Bundle()
        for (data in extraData) {
            if (data.second is String) {
                dataBundle.putString(data.first, data.second as String)
            } else if (data.second is Parcelable) {
                dataBundle.putParcelable(data.first, data.second as Parcelable)
            }
        }

        val intent = Intent(applicationContext, DataManagerService::class.java)
            .putExtra("request", request.toString())
            .putExtra("data", dataBundle)

        applicationContext.startService(intent)
    }

    private fun onOpponentMoved(data: Array<String>): Parcelable {
        val gameId = data[0]
        val moveNotation = data[1]
        val timeStamp = data[2].toLong()
        val move = Move.fromChessNotation(moveNotation)

        val moveData = MoveData(gameId, GameStatus.PLAYER_MOVE, timeStamp, move)

        try {
            val game = dataManager.getGame(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//            game.moveOpponent(move, true)
            game.addNews(MoveNews(NewsType.OPPONENT_MOVED, moveData))
            Logger.debug(TAG, "Adding news to game: OPPONENT_MOVED")
            game.lastUpdated = timeStamp
            dataManager.setGame(gameId, game)
        } catch (e: Exception) {
            NetworkService.sendCrashReport("crash_data_worker_on_opponent_moved.txt", e.stackTraceToString(), applicationContext)
        }

        return moveData
    }

    private fun onIncomingInvite(data: Array<String>): Parcelable {
        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)

//        dataManager.setInvite(inviteId, inviteData)

        return InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
    }

    private fun onNewGameStarted(data: Array<String>): Parcelable {
        val inviteId = data[0]
        val opponentName = data[1]
        val opponentId = data[2]
        val opponentStatus = data[3]
        val playingWhite = data[4].toBoolean()
        val timeStamp = data[5].toLong()

        val gameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE

        val newGame = MultiPlayerGame(inviteId, opponentId, opponentName, gameStatus, opponentStatus, timeStamp, playingWhite)
        newGame.lastUpdated = timeStamp

        dataManager.setGame(inviteId, newGame)
        dataManager.removeSavedInvite(inviteId)

        dataManager.addRecentOpponent(applicationContext, Pair(opponentName, opponentId))

        val hasUpdate = gameStatus == GameStatus.PLAYER_MOVE

        return GameCardItem(inviteId, timeStamp, opponentName, gameStatus, hasUpdate = hasUpdate)
    }

    private fun onUndoRequested(data: Array<String>): ParcelableString {
        val gameId = data[0]

        val game = dataManager.getGame(gameId)!!
        game.addNews(NewsType.OPPONENT_REQUESTED_UNDO)
        dataManager.setGame(gameId, game)

        return ParcelableString(gameId)
    }

    private fun onUndoAccepted(data: Array<String>): ParcelablePair<ParcelableString, ParcelableInt> {
        val gameId = data[0]
        val numberOfReversedMoves = data[1].toInt()

        val game = dataManager.getGame(gameId)!!

        game.addNews(IntNews(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfReversedMoves))
        game.status = GameStatus.PLAYER_MOVE
        dataManager.setGame(gameId, game)

        return ParcelablePair(ParcelableString(gameId), ParcelableInt(numberOfReversedMoves))
    }

    private fun onUndoRejected(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_REJECTED_UNDO)

        return ParcelableString(gameId)
    }

    private fun onOpponentResigned(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_RESIGNED)

        return ParcelableString(gameId)
    }

    private fun onDrawOffered(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_OFFERED_DRAW)

        return ParcelableString(gameId)
    }

    private fun onDrawAccepted(data: Array<String>): ParcelableString {
        val gameId = data[0]

        val game = dataManager.getGame(gameId)!!
        game.status = GameStatus.GAME_DRAW
        game.addNews(NewsType.OPPONENT_ACCEPTED_DRAW)
        dataManager.setGame(gameId, game)

        return ParcelableString(gameId)
    }

    private fun onDrawRejected(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager.getGame(gameId)!!.addNews(NewsType.OPPONENT_REJECTED_DRAW)

        return ParcelableString(gameId)
    }

    private fun onChatMessageReceived(data: Array<String>): ChatMessage {
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        dataManager.getGame(gameId)!!.addMessage(ChatMessage(gameId, timeStamp, messageContent, MessageType.RECEIVED))
        dataManager.getGame(gameId)!!.addNews(NewsType.CHAT_MESSAGE)

        return ChatMessage(gameId, timeStamp, messageContent, MessageType.RECEIVED)
    }

    private fun onUserStatusChanged(data: Array<String>): ParcelableString {
        val opponentId = data[0]
        val opponentStatus = data[1]

        for (gameEntry in dataManager.getSavedGames()) {
            val gameId = gameEntry.key
            val game = gameEntry.value

            if (game.opponentId == opponentId) {
                dataManager.getGame(gameId)!!.opponentStatus = opponentStatus
//                game.opponentStatus = opponentStatus
//                dataManager[gameId] = game
            }
        }

        return ParcelableString(opponentStatus)
    }

    private fun onCompareData(data: Array<String>): ParcelableString {
        val localFiles = FileManager.listFilesInDirectory()

        val missingData = ArrayList<String>()

        for (serverData in data) {
            if (serverData.startsWith("opening:")) {
                var missingOpeningsString = "opening:"
                val openingFiles = localFiles.filter { fileName -> fileName.startsWith("opening_") }.map { openingName -> openingName.removePrefix("opening_") }

                val serverFiles = parseServerFiles(serverData)
                for (serverFile in serverFiles) {
                    if (serverFile.isBlank()) {
                        continue
                    }

                    if (serverFile.startsWith(".") && serverFile.endsWith(".txt.swp")) {
                        continue
                    }

                    if (!openingFiles.contains(serverFile)) {
                        missingOpeningsString += "$serverFile%"
                    }
                }

                missingData += missingOpeningsString.removeSuffix("%")
            } else if (serverData.startsWith("practice_session:")) {
                var missingPracticeSessionString = "practice_session:"
                val practiceFiles = localFiles.filter { fileName -> fileName.startsWith("practice_session_") }.map { openingName -> openingName.removePrefix("practice_session_") }

                val serverFiles = parseServerFiles(serverData)
                for (serverFile in serverFiles) {
                    if (serverFile.isBlank()) {
                        continue
                    }
                    if (!practiceFiles.contains(serverFile)) {
                        missingPracticeSessionString += "$serverFile%"
                    }
                }

                missingData += missingPracticeSessionString.removeSuffix("%")
            } else if (serverData.startsWith("multiplayer_games:")) {
                var missingGamesString = "multiplayer_games:"
                val serverGames = parseServerFiles(serverData)

                val mpGames = FileManager.readLines(applicationContext, "mp_games.txt") ?: ArrayList()
                val mpGameIds = ArrayList<String>()
                for (gameLine in mpGames) {
                    val gameId = gameLine.split("|").first()
                    mpGameIds += gameId
                }

                for (serverGame in serverGames) {
                    if (!mpGameIds.contains(serverGame)) {
                        missingGamesString += "$serverGame%"
                    }
                }

                missingData += missingGamesString.removeSuffix("%")
            }
        }

        return ParcelableString("$userId|${missingData.joinToString("|")}")
//        NetworkManager.getInstance().sendMessage(NetworkMessage(Topic.RESTORE_DATA, "$userId|${missingData.joinToString("|")}"))
    }

    private fun onRestoreData(data: Array<String>): ParcelableNull {
        for (serverData in data) {
            val separatorIndex = serverData.indexOf(":")
            val dataType = serverData.substring(0, separatorIndex)
            if (dataType == "multiplayer_games") {
                val gamesData = serverData.substring(separatorIndex + 1).split("%")

                for (gameData in gamesData) {
                    if (gameData.isBlank()) {
                        continue
                    }

                    val game = MultiPlayerGame.parseFromServer(gameData, userId)
                    dataManager.setGame(game.gameId, game)
                }
                dataManager.saveGames(applicationContext)
            } else if (dataType == "invites") {
                val invitesData = serverData.substring(separatorIndex + 1).split("%")

                for (inviteData in invitesData) {
                    if (inviteData.isBlank()) {
                        continue
                    }

                    val invite = InviteData.fromString(inviteData)
                    dataManager.setInvite(invite.inviteId, invite)
                }
                dataManager.saveInvites(applicationContext)
            } else if (dataType == "recent_opponents") {
                val opponents = serverData.substring(separatorIndex + 1).split("%")

                val recentOpponents = ArrayList<Pair<String, String>>()
                for (opponentData in opponents) {
                    if (opponentData.isBlank()) {
                        continue
                    }

                    val opponentSeparatorIndex = opponentData.indexOf("@#!")
                    val opponentName = opponentData.substring(0, opponentSeparatorIndex)
                    val opponentId = opponentData.substring(opponentSeparatorIndex + 3)
//                    recentOpponents +=
                    dataManager.addRecentOpponent(applicationContext, Pair(opponentName, opponentId))
                }

//                dataManager.setRecentOpponents(applicationContext, recentOpponents)
            } else {
                val filesData = serverData.substring(separatorIndex + 1).split("%")

                for (fileData in filesData) {
                    if (fileData.isBlank()) {
                        continue
                    }

                    val fileSeparatorIndex = fileData.indexOf("@#!")
                    val fileName = fileData.substring(0, fileSeparatorIndex)
                    val fileContent = fileData.substring(fileSeparatorIndex + 3)
                    FileManager.write(applicationContext, "${dataType}_$fileName.txt", fileContent)
                }
            }
        }

        dataManager.loadData(applicationContext)
        return ParcelableNull()
    }

    private fun parseServerFiles(serverData: String): List<String> {
        val separatorIndex = serverData.indexOf(':')
        val filesString = serverData.substring(separatorIndex + 1)

        return filesString.split("%").toList()
    }

    private fun Data.getParcelable(type: Parcelable.Creator<*>?, key: String): Parcelable? {
        val parcel = Parcel.obtain()
        try {
            val bytes = getByteArray(key) ?: return null
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)

            return type?.createFromParcel(parcel) as Parcelable?
        } finally {
            parcel.recycle()
        }
    }

    companion object {
        private const val TAG = "StoreDataWorker"
    }
}