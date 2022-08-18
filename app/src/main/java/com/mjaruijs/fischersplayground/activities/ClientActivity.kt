package com.mjaruijs.fischersplayground.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import androidx.appcompat.app.AppCompatActivity
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameCardItem
import com.mjaruijs.fischersplayground.adapters.gameadapter.InviteData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAMES_AND_INVITES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_INVITES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_MULTIPLAYER_GAMES
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_NEW_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_OPPONENT_MOVED
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_REGISTER_CLIENT
import java.lang.ref.WeakReference

open class ClientActivity : AppCompatActivity() {

    private var clientMessenger = Messenger(IncomingHandler(this))

    private var serviceMessenger: Messenger? = null
    var serviceBound = false

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


    class IncomingHandler(activity: ClientActivity) : Handler() {

        private val activityReference = WeakReference(activity)

        @Suppress("UNCHECKED_CAST")
        override fun handleMessage(msg: Message) {
            val activity = activityReference.get()
            println("Received in Activity: ${msg.obj}")

            when (msg.what) {
                FLAG_GET_MULTIPLAYER_GAMES -> {
                    activity!!.updateGames(msg.obj as? HashMap<String, MultiPlayerGame>)
//                    if (activity is MainActivity) {
//                        activity.updateGames(>)
//                    }
                }
                FLAG_GET_INVITES -> {
                    if (activity is MainActivity) {
                        activity.updateInvites(msg.obj as? HashMap<String, InviteData>)
                    }
                }
                FLAG_GET_GAMES_AND_INVITES -> {
                    if (activity is MainActivity) {
                        activity.updateGamesAndInvites(msg.obj as? Pair<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>>)
                    }
                }
                FLAG_NEW_GAME -> {
                    if (activity is MainActivity) {
                        activity.newGameStarted(msg.obj as? Pair<String, GameCardItem>)
                    }
                }
                FLAG_OPPONENT_MOVED -> {
                    if (activity is MainActivity) {

                    }
                }
            }

        }
    }
}