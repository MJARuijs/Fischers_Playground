package com.mjaruijs.fischersplayground.activities

import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.dialogs.DoubleButtonDialog
import com.mjaruijs.fischersplayground.networking.ConnectivityCallback
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.services.StoreDataWorker
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.util.*
import java.util.prefs.PreferenceChangeListener

abstract class ClientActivity : AppCompatActivity() {

    private val tag = "ClientActivity"

    private val networkReceiver = MessageReceiver(::onMessageReceived)
    private val intentFilter = IntentFilter("mjaruijs.fischers_playground")

    protected var userId: String = DEFAULT_USER_ID
    protected var userName = DEFAULT_USER_NAME

    protected lateinit var networkManager: NetworkManager
    protected lateinit var dataManager: DataManager
    protected lateinit var vibrator: Vibrator

    protected lateinit var incomingInviteDialog: DoubleButtonDialog

    protected var stayingInApp = false

    private var leftApp = false

    open val stayInAppOnBackPress = true

    open var activityName: String = ""

    open val saveGamesOnPause = true

    private fun isUserRegisteredAtServer(): Boolean {
        return getPreference(USER_PREFERENCE_FILE).contains(USER_ID_KEY)
    }

    protected fun getPreference(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileManager.init(applicationContext)

        val preferences = getPreference(USER_PREFERENCE_FILE)
        userId = preferences.getString(USER_ID_KEY, DEFAULT_USER_ID)!!
        userName = preferences.getString(USER_NAME_KEY, DEFAULT_USER_NAME)!!

        networkManager = NetworkManager.getInstance()
        dataManager = DataManager.getInstance(this)

        
    }

    override fun onResume() {
        super.onResume()
        Logger.debug("MyTag", "Registering receiver")
        registerReceiver(networkReceiver, intentFilter)

        NotificationBuilder.getInstance(applicationContext).clearNotifications()

        dataManager.loadData(applicationContext)

        leftApp = false

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        incomingInviteDialog = DoubleButtonDialog(this, "New Invite", "Decline", "Accept")

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        if (!networkManager.isConnected()) {
            val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
            connectivityManager.requestNetwork(networkRequest, ConnectivityCallback(::onNetworkAvailable, ::onNetworkLost))

//            networkManager.run(applicationContext)
//            if (userId != DEFAULT_USER_ID) {
//                networkManager.sendMessage(NetworkMessage(Topic.SET_USER_ID, userId))
//            }
        }
    }

    override fun onStop() {
        if (saveGamesOnPause) {
            dataManager.saveData(applicationContext)
        }

        if (!stayingInApp) {
            leftApp = true
            Logger.debug("MyTag", "Network stopped by onPause()")
            networkManager.stop()
        }

        incomingInviteDialog.destroy()
        Logger.debug("MyTag", "Unregistering receiver")
        unregisterReceiver(networkReceiver)
        super.onStop()
    }

//    override fun onPause() {
//        if (saveGamesOnPause) {
//            dataManager.saveData(applicationContext)
//        }
//
//        if (!stayingInApp) {
//            leftApp = true
//            Logger.debug("MyTag", "Network stopped by onPause()")
//            networkManager.stop()
//        }
//
//        incomingInviteDialog.destroy()
//        Logger.debug("MyTag", "Unregistering receiver")
//        unregisterReceiver(networkReceiver)
//        super.onPause()
//    }

    override fun onBackPressed() {
        stayingInApp = stayInAppOnBackPress

        if (stayingInApp) {
            super.onBackPressed()
        } else {
            moveTaskToBack(true)
        }
    }

    private fun onNetworkAvailable() {
        if (!leftApp) {
            networkManager.run(applicationContext)
            if (userId != DEFAULT_USER_ID) {
                networkManager.sendMessage(NetworkMessage(Topic.ID_LOGIN, userId))
            }
        }
    }

    private fun onNetworkLost() {
        if (!leftApp) {
            Logger.debug("MyTag", "Network Lost")
            networkManager.stop()
        }
    }

    open fun sendResumeStatusToServer() {
//        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|online"))
    }

    private fun sendAwayStatusToServer() {
//        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|away"))
    }

    open fun onMessageReceived(topic: Topic, content: Array<String>, messageId: Long) {
        Logger.debug("MyTag", "ClientActivity: Received message: $topic")

        networkManager.sendMessage(NetworkMessage(Topic.CONFIRM_MESSAGE, "", messageId))
        dataManager.handledMessage(messageId)
        dataManager.lockAndSaveHandledMessages(applicationContext)


        sendDataToWorker(topic, content, messageId, when (topic) {
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
            Topic.COMPARE_OPENINGS -> ::onCompareOpenings
            Topic.RESTORE_OPENINGS -> { _ -> }
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

    private fun onCompareOpenings(output: Parcelable) {
        Logger.debug("MyTag", "Comparing Openings")
        if (output !is ParcelableString) {
            Logger.debug("MyTag", "Returning")
            return
        }

        val missingOpenings = output.value.split(",")

        Logger.debug("MyTag", "Test: $missingOpenings")

        if (missingOpenings.isNotEmpty()) {
            networkManager.sendMessage(NetworkMessage(Topic.RESTORE_OPENINGS, "$userId|${missingOpenings.joinToString(",")}"))
        }
    }

    open fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {}

    private fun sendDataToWorker(topic: Topic, data: Array<String>, messageId: Long, onResult: (Parcelable) -> Unit) {
        val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
            .setInputData(
                workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("content", data),
                    Pair("messageId", messageId)
                )
            )
            .build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(worker)

        workManager.getWorkInfoByIdLiveData(worker.id)
            .observe(this) {
                if (it != null && it.state.isFinished) {
                    Logger.debug("MyTag", "WHAT")
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
        val game = dataManager.getGame(moveData.gameId)!!
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
        const val FIRE_BASE_PREFERENCE_FILE = "fcm_token"
        const val USER_PREFERENCE_FILE = "user_data"

        const val USER_ID_KEY = "user_id"
        const val USER_EMAIL_KEY = "user_email"
        const val USER_NAME_KEY = "user_name"
        const val INITIALIZED_KEY = "initialized"

        const val DEFAULT_USER_ID = "default_user_id"
        const val DEFAULT_USER_NAME = "default_user_name"
    }
}