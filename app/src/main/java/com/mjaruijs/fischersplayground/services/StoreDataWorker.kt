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
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString

class StoreDataWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

    private lateinit var id: String
    private lateinit var dataManager: DataManager

    override fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences("user_data", Service.MODE_PRIVATE)
        id = preferences.getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!

        dataManager = DataManager.getInstance(applicationContext)

        val topic = Topic.fromString(inputData.getString("topic")!!)
        val data = inputData.getStringArray("data")!!

        val output: Any = when (topic) {
            Topic.INVITE -> onIncomingInvite(data)
            Topic.NEW_GAME -> onNewGameStarted(data)
            Topic.MOVE -> onOpponentMoved(data)
            Topic.UNDO_REQUESTED -> onUndoRequested(data)
            Topic.UNDO_ACCEPTED -> onUndoAccepted(data)
            Topic.UNDO_REJECTED -> onUndoRejected(data)
            Topic.RESIGN -> onOpponentResigned(data)
            Topic.DRAW_OFFERED -> onDrawOffered(data)
            Topic.DRAW_ACCEPTED -> onDrawAccepted(data)
            Topic.DRAW_REJECTED -> onDrawRejected(data)
            Topic.CHAT_MESSAGE -> onChatMessageReceived(data)
            Topic.USER_STATUS_CHANGED -> onUserStatusChanged(data)
            Topic.RECONNECT_TO_SERVER -> reconnectToServer(data)
            else -> throw IllegalArgumentException("Could not parse data with unknown topic: $topic")
        }

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
        val game = dataManager[gameId]

        try {
            game.moveOpponent(move, false)
            game.lastUpdated = timeStamp
            dataManager[gameId] = game

            println("SAVING GAME: $gameId ${move.toChessNotation()}")
        } catch (e: Exception) {
            FileManager.write(applicationContext, "crash_log.txt", e.stackTraceToString())
        }

        return MoveData(gameId, GameStatus.PLAYER_MOVE, timeStamp, move)
    }

    private fun onIncomingInvite(data: Array<String>): Parcelable {
        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
        dataManager.savedInvites[inviteId] = inviteData

        return InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
    }

    private fun onNewGameStarted(data: Array<String>): Parcelable {

        val inviteId = data[0]
        val opponentName = data[1]
        val opponentStatus = data[2]
        val playingWhite = data[3].toBoolean()
        val timeStamp = data[4].toLong()

        val underscoreIndex = inviteId.indexOf('_')
        val opponentId = inviteId.substring(0, underscoreIndex)

        val gameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE

        val newGame = MultiPlayerGame(inviteId, opponentId, opponentName, gameStatus, opponentStatus, timeStamp, playingWhite)
        newGame.lastUpdated = timeStamp

        dataManager[inviteId] = newGame
        dataManager.savedInvites.remove(inviteId)
//        updateRecentOpponents(Pair(opponentName, opponentId))

        val hasUpdate = gameStatus == GameStatus.PLAYER_MOVE

        return GameCardItem(inviteId, timeStamp, opponentName, gameStatus, hasUpdate = hasUpdate)
    }

    private fun onUndoRequested(data: Array<String>): ParcelableString {
        val gameId = data[0]

        val game = dataManager[gameId]
        game.addNews(NewsType.OPPONENT_REQUESTED_UNDO)
        dataManager[gameId] = game

        return ParcelableString(gameId)
    }

    private fun onUndoAccepted(data: Array<String>): ParcelablePair<ParcelableString, ParcelableInt> {
        val gameId = data[0]
        val numberOfReversedMoves = data[1].toInt()

//        dataManager[gameId].undoMoves(numberOfReversedMoves)
        val game = dataManager[gameId]
        game.undoMoves(numberOfReversedMoves)
        dataManager[gameId] = game

        return ParcelablePair(ParcelableString(gameId), ParcelableInt(numberOfReversedMoves))
    }

    private fun onUndoRejected(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager[gameId].addNews(NewsType.OPPONENT_REJECTED_UNDO)

        return ParcelableString(gameId)
    }

    private fun onOpponentResigned(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager[gameId].addNews(NewsType.OPPONENT_RESIGNED)

        return ParcelableString(gameId)
    }

    private fun onDrawOffered(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager[gameId].addNews(NewsType.OPPONENT_OFFERED_DRAW)

        return ParcelableString(gameId)
    }

    private fun onDrawAccepted(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager[gameId].status = GameStatus.GAME_DRAW

        return ParcelableString(gameId)
    }

    private fun onDrawRejected(data: Array<String>): ParcelableString {
        val gameId = data[0]

        dataManager[gameId].addNews(NewsType.OPPONENT_REJECTED_DRAW)

        return ParcelableString(gameId)
    }

    private fun onChatMessageReceived(data: Array<String>): ChatMessage.Data {
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        dataManager[gameId].addMessage(ChatMessage(timeStamp, messageContent, MessageType.RECEIVED))

        return ChatMessage.Data(gameId, timeStamp, messageContent, MessageType.RECEIVED)
    }

    private fun onUserStatusChanged(data: Array<String>) {
        val opponentId = data[0]
        val opponentStatus = data[1]

        for (gameEntry in dataManager.getSavedGames()) {
            val gameId = gameEntry.key
            val game = gameEntry.value

            if (game.opponentId == opponentId) {
                game.opponentStatus = opponentStatus
                dataManager[gameId] = game
            }
        }
    }

    private fun reconnectToServer(data: Array<String>) {
        val address = data[0]
        val port = data[1].toInt()

        val networkManager = NetworkManager.getInstance()
        networkManager.run(applicationContext, address, port)

        val userId = applicationContext.getSharedPreferences(ClientActivity.USER_PREFERENCE_FILE, FirebaseMessagingService.MODE_PRIVATE).getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!
        if (userId != DEFAULT_USER_ID) {
            networkManager.sendMessage(NetworkMessage(Topic.SET_USER_ID, userId))
        }
    }
}