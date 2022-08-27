package com.mjaruijs.fischersplayground.activities.game

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Messenger
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightObserver
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightProvider
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.fragments.ChatFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.MultiplayerActionButtonsFragment
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_SAVE_GAME
import com.mjaruijs.fischersplayground.util.FileManager

class MultiplayerGameActivity : GameActivity(), KeyboardHeightObserver {

    override var activityName = "multiplayer_activity"

    override var clientMessenger = Messenger(IncomingHandler(this))

//    private var stayingInApp = false

    private var chatInitialized = false
    private var chatOpened = false
    private var chatTranslation = 0

    private lateinit var keyboardHeightProvider: KeyboardHeightProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")
            initChatBox()

            keyboardHeightProvider = KeyboardHeightProvider(this)
            findViewById<View>(R.id.game_layout).post {
                Runnable {
                    keyboardHeightProvider.start()
                }.run()
            }

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.chat_container, ChatFragment(::onChatMessageSent, ::translateChat, ::closeChat))
                replace(R.id.action_buttons_fragment, MultiplayerActionButtonsFragment(gameId, userId, ::isChatOpened) {
                    glView.requestRender()
                })
            }
        } catch (e: Exception) {
            FileManager.append(this,  "mp_game_activity_crash_report.txt", e.stackTraceToString())
        }
    }

    override fun onResume() {
        Thread {
            while (!serviceBound) {
                Thread.sleep(10)
            }
            sendMessage(FLAG_GET_GAME, gameId)
        }.start()
//        NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$playerId|$gameId"))
        keyboardHeightProvider.observer = this
        super.onResume()
    }

    override fun onPause() {
        if (!stayingInApp) {
//            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$playerId|$gameId|away"))
        }

        sendMessage(FLAG_SAVE_GAME, game)

        keyboardHeightProvider.observer = null
        super.onPause()
    }

    override fun onDestroy() {
        keyboardHeightProvider.close()
        super.onDestroy()
    }

    override fun setGame(game: MultiPlayerGame) {
        this.game = game
        opponentName = game.opponentName
        isPlayingWhite = game.isPlayingWhite

        if (loadFragments) {
            loadFragments()
        }

        runOnUiThread {
            getChatFragment().addMessages(game.chatMessages)
            getActionBarFragment().game = game
        }

        runOnUiThread {
            processNews(game)
        }

        setGameCallbacks()
        super.setGame(game)
    }

    override fun onDisplaySizeChanged(width: Int, height: Int) {
        super.onDisplaySizeChanged(width, height)

        if (!chatInitialized) {
            val chatFragment = findViewById<FragmentContainerView>(R.id.chat_container)

            val openChatButton = findViewById<ImageView>(R.id.open_chat_button)
            val chatButtonWidth = openChatButton.width
            chatTranslation = width - chatButtonWidth

            chatFragment.translationX -= chatTranslation
            openChatButton.translationX -= chatTranslation
            chatInitialized = true
        }
    }

    override fun onClick(x: Float, y: Float) {
        if (isChatOpened()) {
            return
        }
        super.onClick(x, y)
    }

    private fun processNews(game: MultiPlayerGame) {
        for (news in game.newsUpdates) {
            when (news.newsType) {
                NewsType.OPPONENT_RESIGNED -> opponentResignedDialog.show(opponentName, ::closeAndSaveGameAsWin)
                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show(gameId, opponentName, ::acceptDraw)
                NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show(gameId, opponentName, ::closeAndSaveGameAsDraw)
                NewsType.OPPONENT_DECLINED_DRAW -> opponentRejectedDrawDialog.show(opponentName)
                NewsType.OPPONENT_REQUESTED_UNDO -> undoRequestedDialog.show(gameId, opponentName, userId)
                NewsType.OPPONENT_ACCEPTED_UNDO -> {
                    game.undoMoves(news.data)
                    glView.requestRender()
                }
                NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
                NewsType.NO_NEWS -> {}
            }
        }
        game.clearNews()
    }

    private fun getChatFragment(): ChatFragment {
        return (supportFragmentManager.fragments.find { fragment -> fragment is ChatFragment } as ChatFragment)
    }

    private fun isChatOpened(): Boolean {
        return chatOpened
    }

    private fun onChatMessageSent(message: ChatMessage) {
        NetworkManager.sendMessage(NetworkMessage(Topic.CHAT_MESSAGE, "", "$gameId|$userId|${message.timeStamp}|${message.message}"))
        (game as MultiPlayerGame).chatMessages += message
    }

    override fun onKeyboardHeightChanged(height: Int) {
        getChatFragment().translate(height)
    }

    private fun translateChat(translation: Float) {
        val chatContainer = findViewById<FragmentContainerView>(R.id.chat_container)
        val openChatButton = findViewById<ImageView>(R.id.open_chat_button)
        chatContainer.x -= translation
        openChatButton.x -= translation

        if (chatContainer.x > 0.0f) {
            chatContainer.x = 0.0f
        }
        if (openChatButton.x > chatTranslation.toFloat()) {
            openChatButton.x = chatTranslation.toFloat()
        }
    }

    private fun closeChat() {
        val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", -chatTranslation.toFloat())
        val chatButtonAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.open_chat_button), "x", 0.0f)

        chatBoxAnimator.duration = 500L
        chatButtonAnimator.duration = 500L

        chatBoxAnimator.start()
        chatButtonAnimator.start()

        chatOpened = false
    }

    override fun onUndoRequested(gameId: String) {
        if (this.gameId == gameId) {
            undoRequestedDialog.show(gameId, opponentName, userId)
        }
    }

    override fun onUndoRequestAccepted(data: Pair<String, Int>?) {
        if (data == null) {
            return
        }

        if (this.gameId == data.first) {
            (game as MultiPlayerGame).undoMoves(data.second)
            glView.requestRender()
        }
    }

    override fun onUndoRequestRejected(gameId: String) {
        if (this.gameId == gameId) {
            undoRejectedDialog.show(opponentName)
        }
    }

//    override fun onOpponentMoved(data: MoveData?) {
//        if (data == null) {
//            return
//        }
//
//        if (this.gameId != data.first) {
//            return
//        }
//    }

    override fun onOpponentResigned(gameId: String) {
        if (this.gameId == gameId) {
            opponentResignedDialog.show(userName, ::closeAndSaveGameAsWin)
        }
    }

    override fun onOpponentOfferedDraw(gameId: String) {
        if (this.gameId == gameId) {
            opponentOfferedDrawDialog.show(gameId, opponentName, ::acceptDraw)
        }
    }

    private fun acceptDraw() {
        NetworkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "accepted_draw", "$gameId|$userId"))
        closeAndSaveGameAsDraw()
    }

    override fun onOpponentAcceptedDraw(gameId: String) {
        if (this.gameId == gameId) {
            opponentAcceptedDrawDialog.show(gameId, opponentName, ::closeAndSaveGameAsDraw)
        }
    }

    override fun onOpponentRejectedDraw(gameId: String) {
        if (this.gameId == gameId) {
            opponentRejectedDrawDialog.show(opponentName)
        }
    }

    override fun onChatMessageReceived(data: Triple<String, String, String>?) {
        if (data == null) {
            return
        }

        if (this.gameId != data.first) {
            return
        }

        val message = ChatMessage(data.second, data.third, MessageType.RECEIVED)
        getChatFragment().addReceivedMessage(message)
    }

    override fun onBackPressed() {
        if (isChatOpened()) {
            closeChat()
        } else {
//            if (game is MultiPlayerGame) {
//                saveGame()
//            }

//            finish()

//            stayingInApp = true

            //TODO: uncomment status shizzle
//            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$playerId|$gameId|online"))

//            val intent = Intent(applicationContext, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            startActivity(intent)
//            moveTaskToBack(true)
            super.onBackPressed()
        }
    }

    private fun initChatBox() {
        val chatBox = findViewById<ImageView>(R.id.open_chat_button) ?: return

        chatBox.setOnClickListener {
            val chatBoxEndX = if (chatOpened) -chatTranslation else 0
            val chatButtonEndX = if (chatOpened) 0 else chatTranslation

            val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", chatBoxEndX.toFloat())
            val chatButtonAnimator = ObjectAnimator.ofFloat(it, "x", chatButtonEndX.toFloat())

            chatBoxAnimator.duration = 500L
            chatButtonAnimator.duration = 500L

            chatBoxAnimator.start()
            chatButtonAnimator.start()

            chatOpened = !chatOpened

            if (chatOpened) {
                getActionBarFragment().disableButtons()
            } else {
                getActionBarFragment().enableButtons()
            }
        }
    }
}