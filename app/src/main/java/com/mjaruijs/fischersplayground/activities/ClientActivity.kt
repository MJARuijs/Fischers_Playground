package com.mjaruijs.fischersplayground.activities

import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.dialogs.UndoRequestedDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.services.StoreDataWorker
import java.util.*
import kotlin.collections.ArrayList

abstract class ClientActivity : AppCompatActivity() {

    private val networkReceiver = MessageReceiver(::onMessageReceived)
//    private val newGameReceiver = MessageReceiver(::o nNewGameStarted)
//    private val opponentMovedReceiver = MessageReceiver("move", ::onOpponentMoved)
//    private val inviteReceiver = MessageReceiver("invite", ::onIncomingInvite)
//    private val requestUndoReceiver = MessageReceiver("undo_requested", ::onUndoRequested)
//    private val undoAcceptedReceiver = MessageReceiver("accepted_undo", ::onUndoAccepted)
//    private val undoRejectedReceiver = MessageReceiver("rejected_undo", ::onUndoRejected)
//    private val opponentResignedReceiver = MessageReceiver("opponent_resigned", ::onOpponentResigned)
//    private val opponentOfferedDrawReceiver = MessageReceiver("opponent_offered_draw", ::onOpponentOfferedDraw)
//    private val opponentAcceptedDrawReceiver = MessageReceiver("accepted_draw", ::onOpponentAcceptedDraw)
//    private val opponentDeclinedDrawReceiver = MessageReceiver("declined_draw", ::onOpponentDeclinedDraw)
//    private val chatMessageReceiver = MessageReceiver("", ::onChatMessageReceived)

    protected val intentFilter = IntentFilter("mjaruijs.fischers_playground")
//    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
//    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    protected var userId: String = DEFAULT_USER_ID
    protected var userName = DEFAULT_USER_NAME

    protected lateinit var networkManager: NetworkManager
    protected lateinit var dataManager: DataManager

    open val stayInAppOnBackPress = true

    protected val incomingInviteDialog = IncomingInviteDialog()

    protected var stayingInApp = false

    open var activityName: String = ""

    private fun isInitialized() = networkManager.isRunning()

    private fun isUserRegisteredAtServer(): Boolean {
        return getPreference(USER_PREFERENCE_FILE).contains(USER_ID_KEY)
    }

    protected fun getPreference(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getPreference(USER_PREFERENCE_FILE)
        userId = preferences.getString(USER_ID_KEY, DEFAULT_USER_ID)!!
        userName = preferences.getString(USER_NAME_KEY, DEFAULT_USER_NAME)!!

        networkManager = NetworkManager.getInstance()
        dataManager = DataManager.getInstance(this)

        incomingInviteDialog.create(this)

        NotificationBuilder.getInstance(this).clearNotifications()
    }

    override fun onResume() {
        super.onResume()

        if (!isInitialized()) {
            preloadModels()

            networkManager.run(this)

            if (userId != DEFAULT_USER_ID) {
                networkManager.sendMessage(NetworkMessage(Topic.SET_USER_ID, userId))
            }
        }

        dataManager.loadData(applicationContext)
        registerReceiver(networkReceiver, intentFilter)
//        registerReceiver(newGameReceiver, infoFilter)
//        registerReceiver(inviteReceiver, infoFilter)
//        registerReceiver(opponentMovedReceiver, gameUpdateFilter)
//        registerReceiver(requestUndoReceiver, gameUpdateFilter)

        if (isUserRegisteredAtServer()) {
            networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|online"))
        }
    }

    override fun onStop() {
        super.onStop()

//        println("ON STOP: $activityName")
        incomingInviteDialog.dismiss()
        unregisterReceiver(networkReceiver)
//        unregisterReceiver(newGameReceiver)
//        unregisterReceiver(inviteReceiver)
//        unregisterReceiver(opponentMovedReceiver)
//        unregisterReceiver(requestUndoReceiver)

        if (!stayingInApp) {
            networkManager.stop()
        }

    }

    override fun onUserLeaveHint() {
//        println("ON_USER_LEAVE_HINT")
        if (!stayingInApp) {
//            println("SENDING AWAY MESSAGE")
            networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|away"))
//            NetworkManager.stop()
        }

        super.onUserLeaveHint()
    }

    override fun onBackPressed() {
        stayingInApp = stayInAppOnBackPress

        if (stayingInApp) {
            super.onBackPressed()
        } else {
            if (!stayingInApp) {
                networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|away"))
            }
            moveTaskToBack(true)
        }
    }

    open fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {}

//    open fun onInviteReceived(inviteData: Pair<String, InviteData>?) {
//        if (inviteData == null) {
//            return
//        }
//        incomingInviteDialog.showInvite(inviteData.second.opponentName, inviteData.first, networkManager)
//    }

    open fun restoreSavedData(data: Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>?) {}

    open fun onMessageReceived(topic: Topic, content: Array<String>) {
        when (topic) {
            Topic.NEW_GAME -> onNewGameStarted(topic, content)
            Topic.MOVE -> onOpponentMoved(topic, content)
            Topic.INVITE -> onIncomingInvite(topic, content)
        }
    }

    open fun onNewGameStarted(topic: Topic, content: Array<String>) {
        sendDataToWorker<GameCardItem>(topic, content) { gameCard ->
            val underscoreIndex = gameCard.id.indexOf('_')
            val opponentId = gameCard.id.substring(0, underscoreIndex)

            dataManager.updateRecentOpponents(applicationContext, Pair(gameCard.opponentName, opponentId))
        }
//        processNewGameData(content) { gameCard ->
//            val underscoreIndex = gameCard.id.indexOf('_')
//            val opponentId = gameCard.id.substring(0, underscoreIndex)
//
//            dataManager.updateRecentOpponents(applicationContext, Pair(gameCard.opponentName, opponentId))
//        }

        // Show popup with new game info
    }

    open fun onOpponentMoved(topic: Topic, content: Array<String>) {
        sendDataToWorker<MoveData>(topic, content) {
            showPopup(it)
        }
    }

    open fun onIncomingInvite(topic: Topic, content: Array<String>) {
        sendDataToWorker<InviteData>(topic, content) {
            // TODO: show popup with invite data
        }
    }

    open fun onUndoRequested(content: String) {
        processUndoRequest(content) {

        }
    }

    protected fun processUndoRequest(content: String, onResult: (UndoRequestedDialog.UndoRequestData) -> Unit) {
        val data = content.split('|').toTypedArray()
        println("GONNASEND TO WORKER")
//        sendDataToWorker<UndoRequestedDialog.UndoRequestData>("undo_requested", data) {
//            onResult(it)
//        }
    }



    open fun onOpponentResigned(gameId: String) {}

    open fun onOpponentOfferedDraw(gameId: String) {}

    open fun onOpponentAcceptedDraw(gameId: String) {}

    open fun onOpponentRejectedDraw(gameId: String) {}

    open fun onChatMessageReceived(data: Triple<String, String, String>?) {}

    open fun setGame(game: MultiPlayerGame) {}

    open fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {}

    protected inline fun <reified T : Parcelable> sendDataToWorker(topic: Topic, data: Array<String>, crossinline onResult: (T) -> Unit) {
        val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
            .setInputData(
                workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("data", data)
                )
            )
            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(worker)

        workManager.getWorkInfoByIdLiveData(worker.id)
            .observe(this) {
                if (it != null && it.state.isFinished) {
                    val result = it.outputData.getParcelable<T>("output")!!
                    onResult(result)
                }
            }
    }

    protected inline fun <reified T : Parcelable> Data.getParcelable(key: String): T? {
        val parcel = Parcel.obtain()
        try {
            val bytes = getByteArray(key) ?: return null
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            val creator = T::class.java.getField("CREATOR").get(null) as Parcelable.Creator<T>
            return creator.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    fun showPopup(moveData: MoveData) {
        val game = dataManager[moveData.gameId]
        Toast.makeText(applicationContext, "${game.opponentName} played ${moveData.move.toChessNotation()}", Toast.LENGTH_SHORT).show()
    }

    private fun preloadModels() {
        PieceTextures.init(resources)

        Thread {
            OBJLoader.preload(resources, R.raw.pawn_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.bishop_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.knight_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.rook_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.queen_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.king_bytes)
        }.start()
    }

    companion object {
        const val FIRE_BASE_PREFERENCE_FILE = "fire_base"
        const val USER_PREFERENCE_FILE = "user_data"

        const val USER_ID_KEY = "user_id"
        const val USER_NAME_KEY = "user_name"

        const val DEFAULT_USER_ID = "default_user_id"
        const val DEFAULT_USER_NAME = "default_user_name"

    }
}