package com.mjaruijs.fischersplayground.activities.game

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightObserver
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightProvider
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.dialogs.DialogResult
import com.mjaruijs.fischersplayground.dialogs.OpponentOfferedDrawDialog
import com.mjaruijs.fischersplayground.dialogs.UndoRequestedDialog
import com.mjaruijs.fischersplayground.fragments.ChatFragment
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.PlayerStatus
import com.mjaruijs.fischersplayground.fragments.actionbars.MultiplayerActionButtonsFragment
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString

class MultiplayerGameActivity : GameActivity(), KeyboardHeightObserver {

    override var activityName = "multiplayer_activity"

    private var chatInitialized = false
    private var chatOpened = false
    private var chatTranslation = 0

    private val undoRequestedDialog = UndoRequestedDialog()
    private val opponentOfferedDrawDialog = OpponentOfferedDrawDialog()

    private lateinit var keyboardHeightProvider: KeyboardHeightProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")

            initChatBox()

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.chat_container, ChatFragment(::onChatMessageSent, ::translateChat, ::closeChat))
                replace(R.id.action_buttons_fragment, MultiplayerActionButtonsFragment(gameId, userId, ::isChatOpened, ::onResign, ::requestRender, networkManager))
            }

            opponentOfferedDrawDialog.create(this, userId, ::acceptDraw)
            undoRequestedDialog.create(this, ::undoRequestResult)

            keyboardHeightProvider = KeyboardHeightProvider(this)
            findViewById<View>(R.id.game_layout).post {
                Runnable {
                    keyboardHeightProvider.start()
                }.run()
            }

            setGameParameters(dataManager[gameId])
        } catch (e: Exception) {
            FileManager.append(this,  "mp_game_activity_crash_report.txt", e.stackTraceToString())
        }
    }

    override fun onResume() {
        setGameCallbacks()
        (game as MultiPlayerGame).sendMoveData = {
            val message = NetworkMessage(Topic.MOVE, "$gameId|$userId|$it")
            networkManager.sendMessage(message)
        }
//        NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$playerId|$gameId"))
        keyboardHeightProvider.observer = this
        super.onResume()
    }

    override fun onPause() {
        dataManager[gameId] = (game as MultiPlayerGame)
        keyboardHeightProvider.observer = null
        super.onPause()
    }

    override fun onDestroy() {
        keyboardHeightProvider.close()
        super.onDestroy()
    }

    override fun setGameParameters(game: MultiPlayerGame) {
        this.game = game
//        game.goToLastMove()
//        game.showPreviousMove()
        opponentName = game.opponentName
        isPlayingWhite = game.isPlayingWhite

        if (loadFragments) {
            loadFragments()
        }

        Thread {
            while (getChatFragment() == null || getChatFragment()!!.isResumed) {
                Thread.sleep(1)
            }

            runOnUiThread {
                getChatFragment()?.addMessages(game.chatMessages)
            }
        }.start()

        runOnUiThread {
            getActionBarFragment()?.game = game
            processNews(game)
        }

        super.setGameParameters(game)
    }

    override fun onContextCreated() {
        super.onContextCreated()
//        game.showNextMove()
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
                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show(gameId, opponentName, networkManager)
                NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show(opponentName, ::closeAndSaveGameAsDraw)
                NewsType.OPPONENT_REJECTED_DRAW -> opponentRejectedDrawDialog.show(opponentName)
                NewsType.OPPONENT_REQUESTED_UNDO -> undoRequestedDialog.show(opponentName)
                NewsType.OPPONENT_ACCEPTED_UNDO -> {
                    game.undoMoves(news.data)
                    glView.requestRender()
                }
                NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
                NewsType.NO_NEWS -> {}
            }
        }
        game.clearAllNews()
    }

    private fun getChatFragment(): ChatFragment? {
        return (supportFragmentManager.fragments.find { fragment -> fragment is ChatFragment } as ChatFragment?)
    }

    private fun isChatOpened(): Boolean {
        return chatOpened
    }

    private fun onChatMessageSent(message: ChatMessage) {
        networkManager.sendMessage(NetworkMessage(Topic.CHAT_MESSAGE, "$gameId|$userId|${message.timeStamp}|${message.message}"))
        (game as MultiPlayerGame).chatMessages += message
    }

    override fun onKeyboardHeightChanged(height: Int) {
        getChatFragment()?.translate(height)
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

    override fun finishActivity(status: GameStatus) {
        dataManager[gameId].status = status
        dataManager.saveData(applicationContext)
        super.finishActivity(status)
    }

    private fun onResign() {
        networkManager.sendMessage(NetworkMessage(Topic.RESIGN, "$gameId|$userId"))
        closeAndSaveGameAsLoss()
    }

    override fun onUndoRequested(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            undoRequestedDialog.show(opponentName)
        } else {
            super.onUndoRequested(output)
        }
    }

    override fun onUndoAccepted(output: Parcelable) {
        val data = output as ParcelablePair<ParcelableString, ParcelableInt>
        (game as MultiPlayerGame).undoMoves(data.second.value)
        glView.requestRender()
    }

    override fun onUndoRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            undoRejectedDialog.show(opponentName)
        }
    }

    override fun onDrawOffered(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            opponentOfferedDrawDialog.show(gameId, opponentName, networkManager)
        }
    }

    override fun onDrawAccepted(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            opponentAcceptedDrawDialog.show(opponentName, ::closeAndSaveGameAsDraw)
        }
    }

    override fun onDrawRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            opponentRejectedDrawDialog.show(opponentName)
        }
    }

    override fun onOpponentResigned(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            opponentResignedDialog.show(opponentName, ::closeAndSaveGameAsWin)
        }
    }

    override fun onChatMessageReceived(output: Parcelable) {
        val messageData = output as ChatMessage.Data

        if (messageData.gameId == this.gameId) {
            getChatFragment()?.addReceivedMessage(ChatMessage(messageData))
        }
    }

    override fun onUserStatusChanged(output: Parcelable) {
//        val
    }


    private fun setOpponentStatusIcon(opponentStatus: String) {
//        println("GETTING FRAGMENT")
        val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for player was found..")

//        println("TRYING TO SET STATUS: ${this.gameId} :: $gameId")

        when {
//            this.gameId == gameId -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_GAME)
            opponentStatus == "online" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
            opponentStatus == "away" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.AWAY)
            opponentStatus == "offline" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.OFFLINE)
            else -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
        }
    }


    private fun acceptDraw() {
        networkManager.sendMessage(NetworkMessage(Topic.DRAW_ACCEPTED, "$gameId|$userId"))
        closeAndSaveGameAsDraw()
    }

    private fun undoRequestResult(result: DialogResult) {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_ACCEPTED_UNDO)
        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)

        if (result == DialogResult.ACCEPT) {
            networkManager.sendMessage(NetworkMessage(Topic.UNDO_ACCEPTED, "$gameId|$userId"))
        } else if (result == DialogResult.DECLINE) {
            networkManager.sendMessage(NetworkMessage(Topic.UNDO_REJECTED, "$gameId|$userId"))
        }
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
                getActionBarFragment()?.disableButtons()
            } else {
                getActionBarFragment()?.enableButtons()
            }
        }
    }
}