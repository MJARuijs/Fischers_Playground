package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.networking.NetworkListener
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.notification.NotificationBuilder
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_CHAT_MESSAGE_RECEIVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_ALL_DATA
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_MULTIPLAYER_GAMES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_RECENT_OPPONENTS
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_INVITE
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_ACCEPTED_DRAW
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_MOVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_OFFERED_DRAW
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_REJECTED_DRAW
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_ACCEPTED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_REJECTED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_REQUESTED
import com.mjaruijs.fischersplayground.services.FirebaseService
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time
import java.lang.ref.WeakReference
import java.util.*

abstract class ClientActivity : AppCompatActivity(), NetworkListener {

    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
    private val opponentMovedReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
    private val inviteReceiver = MessageReceiver(Topic.INFO, "invite", ::onIncomingInvite)
//    private val requestUndoReceiver = MessageReceiver(Topic.GAME_UPDATE, "request_undo", ::onUndoRequested)
//    private val undoAcceptedReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_undo", ::onUndoAccepted)
//    private val undoRejectedReceiver = MessageReceiver(Topic.GAME_UPDATE, "rejected_undo", ::onUndoRejected)
//    private val opponentResignedReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_resigned", ::onOpponentResigned)
//    private val opponentOfferedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_offered_draw", ::onOpponentOfferedDraw)
//    private val opponentAcceptedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_draw", ::onOpponentAcceptedDraw)
//    private val opponentDeclinedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "declined_draw", ::onOpponentDeclinedDraw)
//    private val chatMessageReceiver = MessageReceiver(Topic.CHAT_MESSAGE, "", ::onChatMessageReceived)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    protected var userId: String = DEFAULT_USER_ID
    protected var userName = DEFAULT_USER_NAME

//    protected var appInBackground = false
//    protected val savedGames = HashMap<String, MultiPlayerGame>()
//    private val savedInvites = HashMap<String, InviteData>()
//    private val recentOpponents = Stack<Pair<String, String>>()

    protected lateinit var networkManager: NetworkManager
    protected lateinit var dataManager: DataManager

    //    private var clientMessenger = Messenger(IncomingHandler(this))
    abstract var clientMessenger: Messenger

    open val stayInAppOnBackPress = true

//    private var serviceMessenger: Messenger? = null
//    var serviceBound = false

    val incomingInviteDialog = IncomingInviteDialog()

    var stayingInApp = false

    open var activityName: String = ""

//    private val connection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
//            serviceMessenger = Messenger(service)
//            serviceBound = true
//
////            val registrationMessage = Message.obtain(null, FLAG_REGISTER_CLIENT, activityName)
////            registrationMessage.replyTo = clientMessenger
////            serviceMessenger!!.send(registrationMessage)
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            serviceMessenger = null
//            serviceBound = false
//        }
//    }

//    fun sendMessage(flag: Int, data: Any? = null) {
//        val message = Message.obtain(null, flag, data)
//        message.replyTo = clientMessenger
//        serviceMessenger!!.send(message)
//    }

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

        networkManager = NetworkManager.getInstance(this)
        dataManager = DataManager.getInstance(this)

        incomingInviteDialog.create(this)

//        NotificationBuilder.clearNotifications(this)
        NotificationBuilder.getInstance(this).clearNotifications()
    }

    override fun onStart() {
        super.onStart()
//        println("ON START $activityName")
//        val intent = Intent(this, DataManagerService::class.java)
//        intent.putExtra("caller", activityName)
//        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()

//        println("ON RESUME $activityName")
//        appInBackground = false
        if (!isInitialized()) {
//            println("Initializing!")
            preloadModels()

            networkManager.run(this)

//            println("GONNA TRY TO SEND ID")
            if (userId != DEFAULT_USER_ID) {
//                println("SENDING ID: $userId")
                networkManager.sendMessage(NetworkMessage(Topic.INFO, "id", userId))
//            } else {
//                println("HAS DEFAULT ID")
            }
//        } else {
//            println("NETWORKER ALREADY INITIALIZED")
        }

//        networkManager.clearKeepAlive()

        dataManager.loadData()
        registerReceiver(newGameReceiver, infoFilter)
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(opponentMovedReceiver, gameUpdateFilter)

        if (isUserRegisteredAtServer()) {
            networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$userId|online"))
        }
    }

    override fun onStop() {
        super.onStop()

//        println("ON STOP: $activityName")
        incomingInviteDialog.dismiss()
        unregisterReceiver(newGameReceiver)
        unregisterReceiver(inviteReceiver)
        unregisterReceiver(opponentMovedReceiver)

        if (!stayingInApp) {
//            networkManager.removeListener(this)
            networkManager.stop()
        }

//        unbindService(connection)
//        serviceBound = false
//        dataManager.saveData()

//        if (!stayingInApp) {
//            networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$userId|away"))
//        }
    }

    override fun onDestroy() {
//        println("ON DESTROY CALLED $activityName")
        if (!stayingInApp) {


//            if (appInBackground) {
//                if (networkManager.numberOfListeners() == 1) {
//                    networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$userId|offline"))
//                    networkManager.removeListener(this)
//                    networkManager.stop()
//                }
//            }


//            NetworkManager.stop()
        }
//        val broadcastIntent = Intent()
//        broadcastIntent.action = "restart_service"
//        broadcastIntent.setClass(this, FirebaseService::class.java)
//        sendBroadcast(broadcastIntent)
//        incomingInviteDialog.dismiss()
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
//        println("ON_USER_LEAVE_HINT")
        if (!stayingInApp) {
//            println("SENDING AWAY MESSAGE")
            networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$userId|away"))
//            NetworkManager.stop()
        }

//        appInBackground = true

        super.onUserLeaveHint()
    }

    override fun onBackPressed() {
        println("ON BACK PRESSED")
        stayingInApp = stayInAppOnBackPress
//        appInBackground = true

        if (stayingInApp) {
            super.onBackPressed()
        } else {
            if (!stayingInApp) {
                println("SENDING AWAY MESSAGE")
                networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$userId|away"))
//            NetworkManager.stop()
            }
            moveTaskToBack(true)
        }
//        super.onBackPressed()
    }

    open fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {}

    open fun onInviteReceived(inviteData: Pair<String, InviteData>?) {
        if (inviteData == null) {
            return
        }
        incomingInviteDialog.showInvite(inviteData.second.opponentName, inviteData.first, networkManager)
    }

    open fun restoreSavedData(data: Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>?) {}

    open fun onIncomingInvite(content: String) {
//        if (appInBackground) {
//            val data = processIncomingInvite(content)
//            val notificationData = dataManager.createNotificationData("invite", content.split('|').toTypedArray())
//            NotificationBuilder.build(applicationContext, notificationData)
//        } else {
            processIncomingInvite(content)
//        }
    }

    protected fun processIncomingInvite(content: String): Pair<String, InviteData> {
        val data = content.split('|')

        val opponentName = data[0]
        val inviteId = data[1]
        val timeStamp = data[2].toLong()

        val inviteData = InviteData(opponentName, timeStamp, InviteType.RECEIVED)
        dataManager.savedInvites[inviteId] = inviteData
        dataManager.saveData()
        return Pair(inviteId, inviteData)
    }

    open fun onNewGameStarted(content: String) {
        val gameCard = processNewGameData(content)
        val underscoreIndex = gameCard.id.indexOf('_')
        val opponentId = gameCard.id.substring(0, underscoreIndex)

        dataManager.updateRecentOpponents(Pair(gameCard.opponentName, opponentId))


        // Show popup with new game info
    }

    protected fun processNewGameData(content: String): GameCardItem {
        val data = content.split('|')

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

        dataManager.saveData()
//        updateRecentOpponents(Pair(opponentName, opponentId))

        val hasUpdate = newGameStatus == GameStatus.PLAYER_MOVE
        return GameCardItem(inviteId, timeStamp, opponentName, newGameStatus, playingWhite, hasUpdate)
    }
//
//    open fun onNewGameStarted(gameCard: GameCardItem) {}
//
//    open fun onUndoRequested(gameId: String) {}
//
//    open fun onUndoRequestAccepted(data: Pair<String, Int>?) {}
//
//    open fun onUndoRequestRejected(gameId: String) {}
//
//    open fun onOpponentMoved(data: MoveData?) {}

    open fun onOpponentMoved(content: String) {

//        if (appInBackground) {
//            NetworkManager.keepAlive()

//            val data = processOpponentMoveData(content)
//            val extraData = HashMap<String, String>()
//            val notificationData = dataManager.createNotificationData("move", content.split('|').toTypedArray())
//            NotificationBuilder.build(applicationContext, notificationData)
//
//            val intent = Intent(applicationContext, MultiplayerGameActivity::class.java)
//            intent.putExtra("game_id", data.gameId)
////            for (d in extraData) {
////                intent.putExtra(d.key, d.value)
////            }
//
//            val stackBuilder = TaskStackBuilder.create(applicationContext)
//            stackBuilder.addParentStack(MultiplayerGameActivity::class.java)
//            stackBuilder.addNextIntent(intent)
//            val onClickIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
//
//
//            NotificationBuilder.build(this, "")
//
//            val channelId = getString(R.string.default_notification_channel_id)
//            val notificationBuilder = NotificationCompat.Builder(this, channelId)
//                .setSmallIcon(R.drawable.black_queen)
//                .setContentTitle("Title")
//                .setContentText("Your move!")
//                .setAutoCancel(true)
//                .setContentIntent(onClickIntent)
//
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val channel = NotificationChannel(channelId, "Opponent moved", NotificationManager.IMPORTANCE_DEFAULT)
//                notificationManager.createNotificationChannel(channel)
//            }
//
//            notificationManager.notify(1, notificationBuilder.build())
//        } else {
            val moveData = processOpponentMoveData(content)

            showPopup(moveData)
//        }
    }

    protected fun processOpponentMoveData(content: String): MoveData {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        val timeStamp = data[2].toLong()
        val move = Move.fromChessNotation(moveNotation)
        val game = dataManager[gameId]

        try {
            game.moveOpponent(move, false)
            game.lastUpdated = timeStamp
            dataManager[gameId] = game
            dataManager.saveGames()
        } catch (e: Exception) {
            FileManager.write(applicationContext, "crash_log.txt", e.stackTraceToString())
        }

//        val worker = OneTimeWorkRequestBuilder<StoreDataWorker>()
//            .setInputData(
//                workDataOf(
//                Pair("topic", topic),
//                Pair("data", data)
//            )
//            )
//            .build()
//
//        val workManager = WorkManager.getInstance(applicationContext)
//        workManager.enqueue(worker)
//
        return MoveData(gameId, GameStatus.PLAYER_MOVE, game.lastUpdated, move)
    }

    fun showPopup(moveData: MoveData) {
        val game = dataManager[moveData.gameId]
        Toast.makeText(applicationContext, "${game.opponentName} played ${moveData.move.toChessNotation()}", Toast.LENGTH_SHORT).show()
    }

    open fun onOpponentResigned(gameId: String) {}

    open fun onOpponentOfferedDraw(gameId: String) {}

    open fun onOpponentAcceptedDraw(gameId: String) {}

    open fun onOpponentRejectedDraw(gameId: String) {}

    open fun onChatMessageReceived(data: Triple<String, String, String>?) {}

    open fun setGame(game: MultiPlayerGame) {}

    open fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {}

    class IncomingHandler(activity: ClientActivity) : Handler() {

        private val activityReference = WeakReference(activity)

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
//            val activity = activityReference.get() ?: return
//            println("Received in Activity: ${msg.obj}")

//            when (msg.what) {
//                FLAG_GET_MULTIPLAYER_GAMES -> activity.restoreSavedGames(msg.obj as? HashMap<String, MultiPlayerGame>)
//                FLAG_NEW_INVITE -> activity.onInviteReceived(msg.obj as? Pair<String, InviteData>)
//                FLAG_GET_ALL_DATA -> activity.restoreSavedData(msg.obj as? Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>)
//                FLAG_NEW_GAME -> activity.onNewGameStarted(msg.obj as GameCardItem)
//                FLAG_OPPONENT_MOVED -> activity.onOpponentMoved(msg.obj as MoveData)
//                FLAG_UNDO_REQUESTED -> activity.onUndoRequested(msg.obj as String)
//                FLAG_UNDO_ACCEPTED -> activity.onUndoRequestAccepted(msg.obj as Pair<String, Int>?)
//                FLAG_UNDO_REJECTED -> activity.onUndoRequestRejected(msg.obj as String)
//                FLAG_OPPONENT_OFFERED_DRAW -> activity.onOpponentOfferedDraw(msg.obj as String)
//                FLAG_OPPONENT_ACCEPTED_DRAW -> activity.onOpponentRejectedDraw(msg.obj as String)
//                FLAG_OPPONENT_REJECTED_DRAW -> activity.onOpponentRejectedDraw(msg.obj as String)
//                FLAG_CHAT_MESSAGE_RECEIVED -> activity.onChatMessageReceived(msg.obj as? Triple<String, String, String>)
//                FLAG_GET_GAME -> activity.setGame(msg.obj as MultiPlayerGame)
//                FLAG_GET_RECENT_OPPONENTS -> activity.updateRecentOpponents(msg.obj as Stack<Pair<String, String>>)
//            }

        }
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