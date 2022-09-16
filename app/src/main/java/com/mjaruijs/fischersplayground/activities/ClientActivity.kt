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
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.DoubleButtonDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.services.StoreDataWorker
import java.util.*

abstract class ClientActivity : AppCompatActivity() {

    private val networkReceiver = MessageReceiver(::onMessageReceived)
    private val intentFilter = IntentFilter("mjaruijs.fischers_playground")

    protected var userId: String = DEFAULT_USER_ID
    protected var userName = DEFAULT_USER_NAME
    private var initialized = false

    protected lateinit var networkManager: NetworkManager
    protected lateinit var dataManager: DataManager

    open val stayInAppOnBackPress = true

    protected lateinit var incomingInviteDialog: DoubleButtonDialog

    protected var stayingInApp = false

    open var activityName: String = ""

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

        NotificationBuilder.getInstance(this).clearNotifications()
    }

    override fun onResume() {
        super.onResume()

        incomingInviteDialog = DoubleButtonDialog(this, "New Invite", "Decline", "Accept")

        initialized = getPreference(USER_PREFERENCE_FILE).getBoolean(INITIALIZED_KEY, false)
        if (!initialized) {
//            preloadModels()

//            networkManager.run(this)
//
//            if (userId != DEFAULT_USER_ID) {
//                networkManager.sendMessage(NetworkMessage(Topic.SET_USER_ID, userId))
//            }
//
//            initialized = true
//            getPreference(USER_PREFERENCE_FILE).edit().putBoolean(INITIALIZED_KEY, true).commit()
        }

        dataManager.loadData(applicationContext)
        registerReceiver(networkReceiver, intentFilter)

        if (isUserRegisteredAtServer()) {
            sendResumeStatusToServer()
        }
    }

    override fun onPause() {

        incomingInviteDialog.destroy()
        unregisterReceiver(networkReceiver)

        if (!stayingInApp) {
            networkManager.stop()
        }

        super.onPause()
    }

    override fun onUserLeaveHint() {
        if (!stayingInApp) {
            getPreference(USER_PREFERENCE_FILE).edit().putBoolean(INITIALIZED_KEY, false).commit()
            sendAwayStatusToServer()
        }

        super.onUserLeaveHint()
    }

    override fun onBackPressed() {
        stayingInApp = stayInAppOnBackPress
        dataManager.saveData(applicationContext, "ClientActivity onBackPressed")

        if (stayingInApp) {
            super.onBackPressed()
        } else {
            sendAwayStatusToServer()
            moveTaskToBack(true)
        }
    }

    open fun sendResumeStatusToServer() {
        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|online"))
    }

    private fun sendAwayStatusToServer() {
        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|away"))
    }

    open fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {}

    open fun onMessageReceived(topic: Topic, content: Array<String>) {
        sendDataToWorker(topic, content, when (topic) {
            Topic.INVITE -> ::onIncomingInvite
            Topic.NEW_GAME -> ::onNewGameStarted
            Topic.MOVE -> ::onOpponentMoved
            Topic.UNDO_REQUESTED -> ::onUndoRequested
            Topic.UNDO_ACCEPTED -> ::onUndoAccepted
            Topic.UNDO_REJECTED -> ::onUndoRejected
            Topic.RESIGN -> ::onOpponentResigned
            Topic.DRAW_OFFERED -> ::onDrawOffered
            Topic.DRAW_ACCEPTED -> ::onDrawAccepted
            Topic.DRAW_REJECTED -> ::onDrawRejected
            Topic.CHAT_MESSAGE -> ::onChatMessageReceived
            Topic.USER_STATUS_CHANGED -> ::onUserStatusChanged
            else -> throw IllegalArgumentException("Failed to handle message with topic: $topic")
        })
    }

    open fun onNewGameStarted(output: Parcelable) {
        val gameCard = output as GameCardItem
        val underscoreIndex = gameCard.id.indexOf('_')
        val opponentId = gameCard.id.substring(0, underscoreIndex)

        dataManager.updateRecentOpponents(applicationContext, Pair(gameCard.opponentName, opponentId))
    }

    open fun onOpponentMoved(output: Parcelable) {
        // TODO: show popup
    }

    open fun onIncomingInvite(output: Parcelable) {
        // TODO: show popup
    }

    open fun onUndoRequested(output: Parcelable) {
        // TODO: show popup
    }

    open fun onUndoAccepted(output: Parcelable) {
        // TODO: show popup
    }

    open fun onUndoRejected(output: Parcelable) {
        // TODO: show popup
    }

    open fun onDrawOffered(output: Parcelable) {
        // TODO: show popup
    }

    open fun onDrawAccepted(output: Parcelable) {
        // TODO: show popup
    }

    open fun onDrawRejected(output: Parcelable) {
        // TODO: show popup
    }

    open fun onOpponentResigned(output: Parcelable) {
        // TODO: show popup
    }

    open fun onChatMessageReceived(output: Parcelable) {
        // TODO: show popup
    }

    open fun onUserStatusChanged(output: Parcelable) {
        // TODO: show popup
    }

    open fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {}

    private fun sendDataToWorker(topic: Topic, data: Array<String>, onResult: (Parcelable) -> Unit) {
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
                    val result = it.outputData.getParcelable(topic.dataType, "output") ?: return@observe
                    onResult(result)
                }
            }
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

    fun showPopup(moveData: MoveData) {
        val game = dataManager[moveData.gameId]
        Toast.makeText(applicationContext, "${game.opponentName} played ${moveData.move.toChessNotation()}", Toast.LENGTH_SHORT).show()
    }

    protected fun preloadModels() {
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
        const val INITIALIZED_KEY = "initialized"

        const val DEFAULT_USER_ID = "default_user_id"
        const val DEFAULT_USER_NAME = "default_user_name"
    }
}