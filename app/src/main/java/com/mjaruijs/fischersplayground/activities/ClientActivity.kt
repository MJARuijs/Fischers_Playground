package com.mjaruijs.fischersplayground.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_CHAT_MESSAGE_RECEIVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_ALL_DATA
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_INVITE
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_MULTIPLAYER_GAMES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_RECENT_OPPONENTS
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
import java.util.*
import kotlin.collections.HashMap

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

//            val registrationMessage = Message.obtain(null, FLAG_REGISTER_CLIENT, activityName)
//            registrationMessage.replyTo = clientMessenger
//            serviceMessenger!!.send(registrationMessage)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingInviteDialog.create(this)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, DataManagerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        incomingInviteDialog.dismiss()
        unbindService(connection)
        serviceBound = false
    }

    override fun onDestroy() {
        super.onDestroy()
        incomingInviteDialog.dismiss()
    }

    open fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {}

    open fun onInviteReceived(inviteData: Pair<String, InviteData>?) {
        if (inviteData == null) {
            return
        }
        incomingInviteDialog.showInvite(inviteData.second.opponentName, inviteData.first)
    }

    open fun restoreSavedData(data: Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>?) {}

    open fun newGameStarted(gameCard: GameCardItem) {}

    open fun onUndoRequested(gameId: String) {}

    open fun onUndoRequestAccepted(data: Pair<String, Int>?) {}

    open fun onUndoRequestRejected(gameId: String) {}

    open fun onOpponentMoved(data: MoveData?) {}

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
            val activity = activityReference.get() ?: return
            println("Received in Activity: ${msg.obj}")

            when (msg.what) {
                FLAG_GET_MULTIPLAYER_GAMES -> activity.restoreSavedGames(msg.obj as? HashMap<String, MultiPlayerGame>)
                FLAG_NEW_INVITE -> activity.onInviteReceived(msg.obj as? Pair<String, InviteData>)
                FLAG_GET_ALL_DATA -> activity.restoreSavedData(msg.obj as? Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>)
                FLAG_NEW_GAME -> activity.newGameStarted(msg.obj as GameCardItem)
                FLAG_OPPONENT_MOVED -> activity.onOpponentMoved(msg.obj as MoveData)
                FLAG_UNDO_REQUESTED -> activity.onUndoRequested(msg.obj as String)
                FLAG_UNDO_ACCEPTED -> activity.onUndoRequestAccepted(msg.obj as Pair<String, Int>?)
                FLAG_UNDO_REJECTED -> activity.onUndoRequestRejected(msg.obj as String)
                FLAG_OPPONENT_OFFERED_DRAW -> activity.onOpponentOfferedDraw(msg.obj as String)
                FLAG_OPPONENT_ACCEPTED_DRAW -> activity.onOpponentRejectedDraw(msg.obj as String)
                FLAG_OPPONENT_REJECTED_DRAW -> activity.onOpponentRejectedDraw(msg.obj as String)
                FLAG_CHAT_MESSAGE_RECEIVED -> activity.onChatMessageReceived(msg.obj as? Triple<String, String, String>)
                FLAG_GET_GAME -> activity.setGame(msg.obj as MultiPlayerGame)
                FLAG_GET_RECENT_OPPONENTS -> activity.updateRecentOpponents(msg.obj as Stack<Pair<String, String>>)
            }

        }
    }
}