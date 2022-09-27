package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessagingService
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
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
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString

class StoreDataWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

    private lateinit var userId: String
    private lateinit var dataManager: DataManager

    override fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences("user_data", Service.MODE_PRIVATE)
        userId = preferences.getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!

        dataManager = DataManager.getInstance(applicationContext)

        val topic = Topic.fromString(inputData.getString("topic")!!)
        val content = inputData.getStringArray("content")!!
//        val runInBackground = inputData.getBoolean("run_in_background", true)
        val messageId = inputData.getLong("messageId", -1L)

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
            Topic.RECONNECT_TO_SERVER -> reconnectToServer(content)
            else -> throw IllegalArgumentException("Could not parse content with unknown topic: $topic")
        }

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
            NetworkManager.getInstance().sendCrashReport("data_worker_parcelable_crash.txt", e.stackTraceToString())
        } finally {
            parcel.recycle()
        }
        return this
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
            game.lastUpdated = timeStamp
            dataManager.setGame(gameId, game)
        } catch (e: Exception) {
            NetworkManager.getInstance().sendCrashReport("data_worker_on_opponent_moved_crash.txt", e.stackTraceToString())
        }

        return moveData
    }

    private fun onIncomingInvite(data: Array<String>): Parcelable {
        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
        dataManager.saveInvite(inviteId, inviteData)

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

        dataManager.updateRecentOpponents(applicationContext, Pair(opponentName, opponentId))

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

    private fun onChatMessageReceived(data: Array<String>): ChatMessage.Data {
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        dataManager.getGame(gameId)!!.addMessage(ChatMessage(timeStamp, messageContent, MessageType.RECEIVED))

        return ChatMessage.Data(gameId, timeStamp, messageContent, MessageType.RECEIVED)
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

    private fun reconnectToServer(data: Array<String>) {
        val address = data[0]
        val port = data[1].toInt()

        val networkManager = NetworkManager.getInstance()
        networkManager.run(applicationContext)

        val userId = applicationContext.getSharedPreferences(ClientActivity.USER_PREFERENCE_FILE, FirebaseMessagingService.MODE_PRIVATE).getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!
        if (userId != DEFAULT_USER_ID) {
            networkManager.sendMessage(NetworkMessage(Topic.SET_USER_ID, userId))
        }
    }
}