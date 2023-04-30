package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.work.Data
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.game.OpponentData
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger

class DataWorker(val applicationContext: Context, val data: Bundle, val onResult: (Parcelable) -> Unit = {}) : Thread() {

    private lateinit var userId: String
    private lateinit var dataManager: DataManager

    override fun run() {
        val preferences = applicationContext.getSharedPreferences(ClientActivity.USER_PREFERENCE_FILE, Service.MODE_PRIVATE)
        userId = preferences.getString(ClientActivity.USER_ID_KEY, ClientActivity.DEFAULT_USER_ID)!!

        dataManager = DataManager.getInstance(applicationContext)

        val topic = Topic.fromString(data.getString("topic")!!)
        val content = data.getStringArray("content")!!
        val messageId = data.getLong("messageId", -1L)

        Logger.debug(TAG, "Start doing work on topic: $topic on thread ${currentThread().id}")

        if (messageId == -1L) {
            return
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
            Topic.SERVER_IP_CHANGED -> onServerIpChanged(content)
            Topic.DEBUG -> {}
            else -> throw IllegalArgumentException("Could not parse content with unknown topic: $topic")
        }

        if (output is Parcelable) {
            onResult(output)
        }

        Logger.debug(TAG, "Finished work on topic: $topic on thread ${currentThread().id}")
    }

    private fun onOpponentMoved(data: Array<String>): Parcelable {
        val gameId = data[0]
        val moveNotation = data[1]
        val timeStamp = data[2].toLong()
        val move = Move.fromChessNotation(moveNotation)

        val moveData = MoveData(gameId, GameStatus.PLAYER_MOVE, timeStamp, move)

        try {
            val game = dataManager.getGame(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
            game.status = GameStatus.OPPONENT_MOVE
            game.addNews(News(NewsType.OPPONENT_MOVED, moveData))
            game.lastUpdated = timeStamp
            dataManager.setGame(gameId, game, applicationContext)
        } catch (e: Exception) {
            Logger.error(TAG, e.stackTraceToString())
            NetworkService.sendCrashReport("crash_data_worker_on_opponent_moved.txt", e.stackTraceToString(), applicationContext)
        }

        return moveData
    }

    private fun onIncomingInvite(data: Array<String>): Parcelable {
        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)

        dataManager.setInvite(inviteId, inviteData, applicationContext)

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

        dataManager.setGame(inviteId, newGame, applicationContext)
        dataManager.removeSavedInvite(inviteId, applicationContext)
        dataManager.addRecentOpponent(applicationContext, OpponentData(opponentName, opponentId))

        val hasUpdate = gameStatus == GameStatus.PLAYER_MOVE

        return GameCardItem(inviteId, timeStamp, opponentName, gameStatus, hasUpdate = hasUpdate)
    }

    private fun onUndoRequested(data: Array<String>): ParcelableString {
        val gameId = data[0]

        val game = dataManager.getGame(gameId)!!
        game.addNews(NewsType.OPPONENT_REQUESTED_UNDO)
        dataManager.setGame(gameId, game, applicationContext)

        return ParcelableString(gameId)
    }

    private fun onUndoAccepted(data: Array<String>): ParcelablePair<ParcelableString, ParcelableInt> {
        val gameId = data[0]
        val numberOfReversedMoves = data[1].toInt()

        val game = dataManager.getGame(gameId)!!
        game.status = GameStatus.PLAYER_MOVE
        dataManager.setGame(gameId, game, applicationContext)

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
        dataManager.setGame(gameId, game, applicationContext)

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

        for (game in dataManager.getSavedGames()) {
            if (game.opponentId == opponentId) {
                dataManager.getGame(game.gameId)!!.opponentStatus = opponentStatus
                dataManager.setGame(game.gameId, game, applicationContext)
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

        return ParcelableString(missingData.joinToString("|"))
//        NetworkManager.getInstance().sendMessage(NetworkMessage(Topic.RESTORE_DATA, "$userId|${missingData.joinToString("|")}"))
    }

    private fun onRestoreData(data: Array<String>) {
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
                    dataManager.setGame(game.gameId, game, applicationContext)
                }
            } else if (dataType == "invites") {
                val invitesData = serverData.substring(separatorIndex + 1).split("%")

                for (inviteData in invitesData) {
                    if (inviteData.isBlank()) {
                        continue
                    }

                    val invite = InviteData.fromString(inviteData)
                    dataManager.setInvite(invite.inviteId, invite, applicationContext)
                }
            } else if (dataType == "recent_opponents") {
                val opponents = serverData.substring(separatorIndex + 1).split("%")

                val recentOpponents = ArrayList<OpponentData>()
                for (opponentData in opponents) {
                    if (opponentData.isBlank()) {
                        continue
                    }

                    val opponentSeparatorIndex = opponentData.indexOf("@#!")
                    val opponentName = opponentData.substring(0, opponentSeparatorIndex)
                    val opponentId = opponentData.substring(opponentSeparatorIndex + 3)
                    recentOpponents += OpponentData(opponentName, opponentId)
                }
                dataManager.setRecentOpponents(applicationContext, recentOpponents)
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
    }

    private fun onServerIpChanged(data: Array<String>) {
        val newIp = data[0]
        applicationContext.getSharedPreferences(NetworkService.SERVER_PREFERENCE_FILE, MODE_PRIVATE).edit().putString(NetworkService.SERVER_IP_KEY, newIp).apply()
    }

    private fun parseServerFiles(serverData: String): List<String> {
        val separatorIndex = serverData.indexOf(':')
        val filesString = serverData.substring(separatorIndex + 1)

        return filesString.split("%").toList()
    }

    companion object {
        private const val TAG = "DataWorker"
    }

}