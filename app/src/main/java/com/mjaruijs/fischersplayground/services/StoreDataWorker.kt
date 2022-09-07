package com.mjaruijs.fischersplayground.services

import android.app.Service
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.ClientActivity.Companion.DEFAULT_USER_ID
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.data.UndoAcceptedData
import com.mjaruijs.fischersplayground.dialogs.UndoRequestedDialog
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.FileManager

class StoreDataWorker(context: Context, workParams: WorkerParameters) : Worker(context, workParams) {

    private lateinit var id: String
    private lateinit var dataManager: DataManager

    override fun doWork(): Result {
        val preferences = applicationContext.getSharedPreferences("user_data", Service.MODE_PRIVATE)
        id = preferences.getString(ClientActivity.USER_ID_KEY, DEFAULT_USER_ID)!!

        dataManager = DataManager.getInstance(applicationContext)

        val topic = Topic.fromString(inputData.getString("topic")!!)
        val data = inputData.getStringArray("data")!!

        val output = when (topic) {
            Topic.MOVE -> onOpponentMoved(data)
            Topic.INVITE -> onIncomingInvite(data)
            Topic.NEW_GAME -> onNewGameStarted(data)
            Topic.UNDO_REQUESTED -> onUndoRequested(data)
            Topic.UNDO_ACCEPTED -> onUndoAccepted(data)
            else -> throw IllegalArgumentException("Could not parse data with unknown topic: $topic")
        }

        val dataBuilder = Data.Builder().putParcelable("output", output)

        return Result.success(dataBuilder.build())
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
            dataManager.saveGames(applicationContext)

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
        dataManager.saveData(applicationContext)

        return InviteData(inviteId, opponentName, timeStamp, InviteType.RECEIVED)
    }

    private fun onNewGameStarted(data: Array<String>): Parcelable {

        val inviteId = data[0]
        val opponentName = data[1]
        val playingWhite = data[2].toBoolean()
        val timeStamp = data[3].toLong()

        val underscoreIndex = inviteId.indexOf('_')
        val opponentId = inviteId.substring(0, underscoreIndex)

        val newGameStatus = if (playingWhite) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE

        val newGame = MultiPlayerGame(inviteId, opponentName, timeStamp, playingWhite)
        newGame.lastUpdated = timeStamp

        dataManager[inviteId] = newGame
        dataManager.savedInvites.remove(inviteId)
        dataManager.saveData(applicationContext)
//        updateRecentOpponents(Pair(opponentName, opponentId))

        val hasUpdate = newGameStatus == GameStatus.PLAYER_MOVE

        return GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, hasUpdate = hasUpdate)
    }

    private fun onUndoRequested(data: Array<String>): UndoRequestedDialog.UndoRequestData {
        val gameId = data[0]
        val opponentName = data[1]
        val game = dataManager[gameId]
        game.addNews(NewsType.OPPONENT_REQUESTED_UNDO)
        dataManager[gameId] = game
        dataManager.saveData(applicationContext)
        return UndoRequestedDialog.UndoRequestData(gameId, opponentName)
    }

    private fun onUndoAccepted(data: Array<String>): UndoAcceptedData {
        val gameId = data[0]
        val opponentName = data[1]
        val numberOfReversedMoves = data[2].toInt()

//        dataManager[gameId].undoMoves(numberOfReversedMoves)
        val game = dataManager[gameId]
        game.undoMoves(numberOfReversedMoves)
        dataManager[gameId] = game
        dataManager.saveData(applicationContext)

        return UndoAcceptedData(gameId, opponentName, numberOfReversedMoves)
    }

}