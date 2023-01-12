package com.mjaruijs.fischersplayground.activities

import android.content.*
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Parcelable.Creator
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.chess.game.MoveData
import com.mjaruijs.fischersplayground.dialogs.DoubleButtonDialog
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.NetworkService
import com.mjaruijs.fischersplayground.services.ProcessIncomingDataWorker
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Logger
import java.lang.ref.WeakReference

abstract class ClientActivity : AppCompatActivity() {

    private val tag = "ClientActivity"

    protected var userId: String = DEFAULT_USER_ID
    protected var userName = DEFAULT_USER_NAME

//    protected lateinit var networkManager: NetworkManager
//    protected lateinit var dataManager: DataManager
    protected lateinit var vibrator: Vibrator

    protected lateinit var incomingInviteDialog: DoubleButtonDialog

    protected var stayingInApp = false

    private var leftApp = false

    open val stayInAppOnBackPress = true

    open var activityName: String = ""

    open val saveGamesOnPause = true

    var networkMessengerClient = Messenger(NetworkMessageHandler(::onMessageReceived))
    var dataMessengerClient = Messenger(DataManagerHandler())

    var networkServiceMessenger: Messenger? = null
    var dataServiceMessenger: Messenger? = null

    private val networkMessengerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null) {
                return
            }

            networkServiceMessenger = Messenger(service)

            val registrationMessage = Message.obtain()
            registrationMessage.what = 0
            registrationMessage.replyTo = networkMessengerClient
            networkServiceMessenger!!.send(registrationMessage)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            networkServiceMessenger = null
        }
    }

    private val dataMessengerConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service == null) {
                return
            }

            dataServiceMessenger = Messenger(service)

//            val registrationMessage = Message.obtain()
//            registrationMessage.what = 0
//            registrationMessage.replyTo = dataMessengerClient
//            dataServiceMessenger!!.send(registrationMessage)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            dataServiceMessenger = null
        }
    }

    private fun isUserRegisteredAtServer(): Boolean {
        return getPreference(USER_PREFERENCE_FILE).contains(USER_ID_KEY)
    }

    protected fun getPreference(name: String): SharedPreferences {
        return getSharedPreferences(name, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.debug(activityName, "Targetting: ${Build.VERSION.SDK_INT}")
        FileManager.init(applicationContext)

        val preferences = getPreference(USER_PREFERENCE_FILE)
        userId = preferences.getString(USER_ID_KEY, DEFAULT_USER_ID)!!
        userName = preferences.getString(USER_NAME_KEY, DEFAULT_USER_NAME)!!

//        networkManager = NetworkManager.getInstance()
//        dataManager = DataManager.getInstance(this)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, NetworkService::class.java), networkMessengerConnection, Context.BIND_AUTO_CREATE)
        bindService(Intent(this, DataManagerService::class.java), dataMessengerConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        val preferences = getPreference(USER_PREFERENCE_FILE)
        userId = preferences.getString(USER_ID_KEY, DEFAULT_USER_ID)!!
        userName = preferences.getString(USER_NAME_KEY, DEFAULT_USER_NAME)!!

        NotificationBuilder.getInstance(applicationContext).clearNotifications()

        leftApp = false

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        incomingInviteDialog = DoubleButtonDialog(this, true,"New Invite", "Decline", "Accept")

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

//        if (!networkManager.isConnected()) {
//            val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
//            connectivityManager.requestNetwork(networkRequest, ConnectivityCallback(::onNetworkAvailable, ::onNetworkLost))
//        }
    }

    override fun onStop() {
        super.onStop()

        unbindService(networkMessengerConnection)
        unbindService(dataMessengerConnection)
    }

    override fun onPause() {
        if (saveGamesOnPause) {
//            dataManager.saveData(applicationContext)
        }

        if (!stayingInApp) {
            leftApp = true
//            networkManager.stop()
        }

        incomingInviteDialog.destroy()
        super.onPause()
    }

    override fun onBackPressed() {
        stayingInApp = stayInAppOnBackPress

        if (stayingInApp) {
            super.onBackPressed()
        } else {
            moveTaskToBack(true)
        }
    }

    protected fun sendNetworkMessage(networkMessage: NetworkMessage) {
        val message = Message.obtain()

        message.what = 1
        message.replyTo = networkMessengerClient
        message.obj = networkMessage
        networkServiceMessenger!!.send(message)
    }

    protected inline fun <reified T>sendToDataManager(request: DataManagerService.Request, noinline onResult: (T) -> Unit = {}) {
        val message = Message.obtain()

        message.data.putString("request", request.toString())
        message.obj = Pair(T::class.java, onResult)
        message.what = 1
        message.replyTo = dataMessengerClient

        if (dataServiceMessenger != null) {
            dataServiceMessenger!!.send(message)
        } else {
            Thread {
                while (dataServiceMessenger == null) {
                    Thread.sleep(10)
                }

                runOnUiThread {
                    dataServiceMessenger!!.send(message)
                }
            }.start()
        }
    }

    protected fun sendToDataManager(request: DataManagerService.Request, vararg extraData: Pair<String, *>) {
        val message = Message.obtain()
        message.data.putString("request", request.toString())
        message.what = 1
        message.replyTo = dataMessengerClient
        addDataToMessage(message, *extraData)

        if (dataServiceMessenger != null) {
            dataServiceMessenger!!.send(message)
        } else {
            Thread {
                while (dataServiceMessenger == null) {
                    Thread.sleep(10)
                }

                runOnUiThread {
                    dataServiceMessenger!!.send(message)
                }
            }.start()
        }
    }

    protected inline fun <reified T>sendToDataManager(request: DataManagerService.Request, vararg extraData: Pair<String, *>, noinline onResult: (T) -> Unit = {}) {
        val message = Message.obtain()

        message.data.putString("request", request.toString())
        message.obj = Pair(T::class.java, onResult)
        message.what = 1
        message.replyTo = dataMessengerClient
        addDataToMessage(message, *extraData)

        if (dataServiceMessenger != null) {
            dataServiceMessenger!!.send(message)
        } else {
            Thread {
                while (dataServiceMessenger == null) {
                    Thread.sleep(10)
                }

                runOnUiThread {
                    dataServiceMessenger!!.send(message)
                }
            }.start()
        }
    }

    protected fun addDataToMessage(message: Message, vararg extraData: Pair<String, *>) {
        val dataBundle = Bundle()
        for (data in extraData) {
            if (data.second is String) {
                dataBundle.putString(data.first, data.second as String)
            } else if (data.second is Parcelable) {
                dataBundle.putParcelable(data.first, data.second as Parcelable)
            }
        }
        message.data.putBundle("data", dataBundle)
    }

    private fun onNetworkAvailable() {
        if (!leftApp) {
//            if (!networkManager.isRunning()) {
////                networkManager.run(applicationContext)
//                if (userId != DEFAULT_USER_ID) {
////                    networkManager.sendMessage(NetworkMessage(Topic.ID_LOGIN, userId))
//                }
//            }
        }
    }

    private fun onNetworkLost() {
        if (!leftApp) {
//            Logger.warn(activityName, "Network Lost")
//            networkManager.stop()
        }
    }

    open fun sendResumeStatusToServer() {
//        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|online"))
    }

    private fun sendAwayStatusToServer() {
//        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|away"))
    }

    open fun onMessageReceived(topic: Topic, content: Array<String>, messageId: Long) {
        Logger.debug(activityName, "Got message: $topic")
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
            Topic.COMPARE_DATA -> ::onCompareData
            Topic.RESTORE_DATA -> ::onRestoreData
            else -> throw IllegalArgumentException("Failed to handle message with topic: $topic")
        })
    }

    open fun onNewGameStarted(output: Parcelable) {
        val gameCard = output as GameCardItem
        val underscoreIndex = gameCard.id.indexOf('_')
        val opponentId = gameCard.id.substring(0, underscoreIndex)

//        dataManager.addRecentOpponent(applicationContext, Pair(gameCard.opponentName, opponentId))
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

    private fun onCompareData(output: Parcelable) {
        val missingData = (output as ParcelableString).value
        if (missingData.isNotBlank()) {
            sendNetworkMessage(NetworkMessage(Topic.RESTORE_DATA, "$userId|$missingData"))
        }
    }

    open fun onRestoreData(output: Parcelable) {
        Logger.debug("client_activity", "RECEIVED ONRESTOREDATA MESSAGE: $activityName")
    }

//    open fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {}

    protected fun sendDataToWorker(topic: Topic, data: Array<String>, messageId: Long, onResult: (Parcelable) -> Unit) {
        Logger.debug("client_activity", "SENDING DATA TO WORKER: $topic $activityName")

//        val workerData = Bundle()
//        workerData.putString("topic", topic.toString())
//        workerData.putStringArray("content", data)
//        workerData.putLong("messageId", messageId)
//
//        DataWorker(applicationContext, workerData) {
//            runOnUiThread {
//                onResult(it)
//            }
//        }.start()

        val worker = OneTimeWorkRequestBuilder<ProcessIncomingDataWorker>()
            .setInputData(
                workDataOf(
                    Pair("topic", topic.toString()),
                    Pair("content", data),
                    Pair("messageId", messageId)
                )
            )
            .build()
//
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

    private fun Data.getParcelable(type: Creator<*>?, key: String): Parcelable? {
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
//        val game = dataManager.getGame(moveData.gameId)!!
//        Toast.makeText(applicationContext, "${game.opponentName} played ${moveData.move.toChessNotation()}", Toast.LENGTH_SHORT).show()
    }

    fun runOnUIThread(runnable: () -> Unit) {
        runOnUiThread {
            runnable()
        }
    }

    companion object {
        const val FIRE_BASE_PREFERENCE_FILE = "fcm_token"
        const val USER_PREFERENCE_FILE = "user_data"

        const val USER_ID_KEY = "user_id"
        const val USER_EMAIL_KEY = "user_email"
        const val USER_NAME_KEY = "user_name"

        const val DEFAULT_USER_ID = "default_user_id"
        const val DEFAULT_USER_NAME = "default_user_name"

        class NetworkMessageHandler(val onMessageReceived: (Topic, Array<String>, Long) -> Unit): Handler() {

//            private val activityReference = WeakReference(activity)

            override fun handleMessage(msg: Message) {
//                val activity = activityReference.get()

                val message = msg.obj as NetworkMessage
                val content = message.content.split('|').toTypedArray()

                onMessageReceived(message.topic, content, message.id)
            }
        }

        class DataManagerHandler : Handler() {

            override fun handleMessage(msg: Message) {
                val pair = (msg.obj as Pair<Any, (Any?) -> Unit>)

                if (msg.what == 1) {
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT < TIRAMISU) {
                        pair.second.invoke(msg.data.getParcelable("output"))
                    } else {
                        pair.second.invoke(msg.data.getParcelable("output", Parcelable::class.java))
                    }
                } else if (msg.what == 2) {
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT < TIRAMISU) {
                        pair.second.invoke(msg.data.getParcelableArrayList<Parcelable>("output"))
                    } else {
                        pair.second.invoke(msg.data.getParcelableArrayList("output", Parcelable::class.java))
                    }
                }
            }
        }
    }
}