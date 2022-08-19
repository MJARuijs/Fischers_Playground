package com.mjaruijs.fischersplayground.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_CHAT_MESSAGE_RECEIVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAMES_AND_INVITES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_INVITES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_MULTIPLAYER_GAMES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_ACCEPTED_DRAW
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_MOVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_OFFERED_DRAW
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_REJECTED_DRAW
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_REGISTER_CLIENT
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_ACCEPTED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_REJECTED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_UNDO_REQUESTED
import java.lang.ref.WeakReference

abstract class ClientActivity : AppCompatActivity() {

//    private var clientMessenger = Messenger(IncomingHandler(this))
    abstract var clientMessenger: Messenger

    private var serviceMessenger: Messenger? = null
    var serviceBound = false

    val incomingInviteDialog = IncomingInviteDialog()

    open var activityName: String = ""

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
            serviceMessenger = Messenger(service)
            serviceBound = true

            val registrationMessage = Message.obtain(null, FLAG_REGISTER_CLIENT, activityName)
            registrationMessage.replyTo = clientMessenger
            serviceMessenger!!.send(registrationMessage)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceMessenger = null
            serviceBound = false
        }
    }

    fun sendMessage(flag: Int, data: Any? = null) {
        val message = Message.obtain(null, flag, data)
        message.replyTo = clientMessenger
        serviceMessenger!!.send(message)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        incomingInviteDialog.create(this)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, DataManagerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        unbindService(connection)
        serviceBound = false
    }

    open fun updateGames(games: HashMap<String, MultiPlayerGame>?) {}

    open fun updateInvites(invites: HashMap<String, InviteData>?) {}

    open fun updateGamesAndInvites(data: Pair<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>>?) {}

    open fun newGameStarted(gameData: Pair<String, GameCardItem>?) {}

    open fun onUndoRequested(gameId: String) {}

    open fun onUndoRequestAccepted(data: Pair<String, Int>?) {}

    open fun onUndoRequestRejected(gameId: String) {}

    open fun onOpponentMoved(data: Triple<String, GameStatus, Long>?) {}

    open fun onOpponentResigned(gameId: String) {}

    open fun onOpponentOfferedDraw(gameId: String) {}

    open fun onOpponentAcceptedDraw(gameId: String) {}

    open fun onOpponentRejectedDraw(gameId: String) {}

    open fun onChatMessageReceived(data: Triple<String, String, String>?) {}

    open fun setGame(game: MultiPlayerGame) {}

    class IncomingHandler(activity: ClientActivity) : Handler() {

        private val activityReference = WeakReference(activity)

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            val activity = activityReference.get() ?: return
            println("Received in Activity: ${msg.obj}")

            when (msg.what) {
                FLAG_GET_MULTIPLAYER_GAMES -> activity.updateGames(msg.obj as? HashMap<String, MultiPlayerGame>)
                FLAG_GET_INVITES -> activity.updateInvites(msg.obj as? HashMap<String, InviteData>)
                FLAG_GET_GAMES_AND_INVITES -> activity.updateGamesAndInvites(msg.obj as? Pair<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>>)
                FLAG_NEW_GAME -> activity.newGameStarted(msg.obj as? Pair<String, GameCardItem>)
                FLAG_OPPONENT_MOVED -> activity.onOpponentMoved(msg.obj as? Triple<String, GameStatus, Long>)
                FLAG_UNDO_REQUESTED -> activity.onUndoRequested(msg.obj as String)
                FLAG_UNDO_ACCEPTED -> activity.onUndoRequestAccepted(msg.obj as Pair<String, Int>?)
                FLAG_UNDO_REJECTED -> activity.onUndoRequestRejected(msg.obj as String)
                FLAG_OPPONENT_OFFERED_DRAW -> activity.onOpponentOfferedDraw(msg.obj as String)
                FLAG_OPPONENT_ACCEPTED_DRAW -> activity.onOpponentRejectedDraw(msg.obj as String)
                FLAG_OPPONENT_REJECTED_DRAW -> activity.onOpponentRejectedDraw(msg.obj as String)
                FLAG_CHAT_MESSAGE_RECEIVED -> activity.onChatMessageReceived(msg.obj as? Triple<String, String, String>)
                FLAG_GET_GAME -> activity.setGame(msg.obj as MultiPlayerGame)
            }

        }
    }
}