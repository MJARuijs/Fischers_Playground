package com.mjaruijs.fischersplayground.activities.game

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.SettingsActivity
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.GAME_PREFERENCES_KEY
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightObserver
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightProvider
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.dialogs.*
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

    private lateinit var undoRequestedDialog: DoubleButtonDialog
    private lateinit var undoAcceptedDialog: SingleButtonDialog
    private lateinit var undoRejectedDialog: SingleButtonDialog

    private lateinit var opponentOfferedDrawDialog: DoubleButtonDialog
    private lateinit var opponentAcceptedDrawDialog: SingleButtonDialog
    private lateinit var opponentRejectedDrawDialog: SingleButtonDialog
    private lateinit var opponentResignedDialog: SingleButtonDialog

    private lateinit var keyboardHeightProvider: KeyboardHeightProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")

            initChatBox()

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.chat_container, ChatFragment(::onChatMessageSent, ::closeChat))
                replace(R.id.action_buttons_fragment, MultiplayerActionButtonsFragment(gameId, userId, ::isChatOpened, ::onOfferDraw, ::onResign, ::cancelMove, ::confirmMove, ::requestRender, networkManager))
            }

            keyboardHeightProvider = KeyboardHeightProvider(this)
            findViewById<View>(R.id.game_layout).post {
                Runnable {
                    keyboardHeightProvider.start()
                }.run()
            }
        } catch (e: Exception) {
            FileManager.append(this,  "mp_game_activity_crash_report.txt", e.stackTraceToString())
        }
    }

    override fun onResume() {
        setGameParameters(dataManager[gameId])
        setGameCallbacks()

        (game as MultiPlayerGame).sendMoveData = {
            val showPopup = getPreference(GAME_PREFERENCES_KEY).getBoolean(SettingsActivity.CONFIRM_MOVES_KEY, false)
            if (showPopup) {
                runOnUiThread {
                    (getActionBarFragment() as MultiplayerActionButtonsFragment).showExtraButtons(it)
                }
            } else {
                confirmMove(it)
            }
        }

        undoRequestedDialog = DoubleButtonDialog(this, "Undo Requested", "Reject", ::rejectUndoRequest, "Accept", ::acceptUndoRequest)
        undoAcceptedDialog = SingleButtonDialog(this, "Move Reversed", "Your undo request has been accepted!", "Continue")
        undoRejectedDialog = SingleButtonDialog(this, "Undo Rejected", "Your undo request was rejected!", "Continue")

        opponentAcceptedDrawDialog = SingleButtonDialog(this, "It's A Draw!", "$opponentName has accepted your draw offer", "Exit", ::closeAndSaveGameAsDraw)
        opponentRejectedDrawDialog = SingleButtonDialog(this, "Game Must Go On", "$opponentName has rejected your draw offer", "Play on")
        opponentOfferedDrawDialog = DoubleButtonDialog(this, "Draw Offered", "$opponentName has offered a draw!", "Decline", ::rejectDrawOffer, "Accept", ::acceptDrawOffer)
        opponentResignedDialog = SingleButtonDialog(this, "You Won", "$opponentName has resigned!", "Exit", ::closeAndSaveGameAsWin)

        keyboardHeightProvider.observer = this

        runOnUiThread {
            processNews()
        }

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

        Thread {
            while (getActionBarFragment() == null || getActionBarFragment()!!.isResumed) {
                Thread.sleep(1)
            }

            runOnUiThread {
                getActionBarFragment()?.game = game
                setOpponentStatusIcon(game.opponentStatus)
            }
        }.start()

        super.setGameParameters(game)
    }

    override fun onContextCreated() {
        super.onContextCreated()
        game.showPreviousMove(true)
        game.showNextMove()
        getActionBarFragment()?.disableForwardButton()
    }

    private fun confirmMove(moveNotation: String) {
        networkManager.sendMessage(NetworkMessage(Topic.MOVE, "$gameId|$userId|$moveNotation|${game.lastUpdated}"))
        (game as MultiPlayerGame).confirmMove()
        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)
    }

    private fun cancelMove() {
        (game as MultiPlayerGame).cancelMove()
        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)
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

            val buttonHeight = getActionBarFragment()!!.view!!.measuredHeight
            (getActionBarFragment() as MultiplayerActionButtonsFragment).initializeAnimator(buttonHeight)

            if ((game as MultiPlayerGame).hasPendingMove()) {
                runOnUiThread {
                    (getActionBarFragment() as MultiplayerActionButtonsFragment).showExtraButtons((game as MultiPlayerGame).moveToBeConfirmed, 0L)
                }
            }
        }
    }

    override fun onClick(x: Float, y: Float) {
        if (isChatOpened()) {
            return
        }
        super.onClick(x, y)
    }

    private fun processNews() {
        for (news in (game as MultiPlayerGame).newsUpdates) {
            when (news.newsType) {
                NewsType.OPPONENT_RESIGNED -> opponentResignedDialog.show()
                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show()
                NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show()
                NewsType.OPPONENT_REJECTED_DRAW -> opponentRejectedDrawDialog.show(opponentName)
                NewsType.OPPONENT_REQUESTED_UNDO -> {
                    undoRequestedDialog.setMessage("$opponentName is requesting to undo their last move!")
                    undoRequestedDialog.show()
                }
                NewsType.OPPONENT_ACCEPTED_UNDO -> {
                    undoAcceptedDialog.show {
                        (game as MultiPlayerGame).undoMoves(news.data)
                        (game as MultiPlayerGame).status = GameStatus.PLAYER_MOVE
                        requestRender()
                    }
                }
                NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
                NewsType.NO_NEWS -> {}
            }
        }
        (game as MultiPlayerGame).clearAllNews()
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
//        dataManager[gameId].status = status
        (game as MultiPlayerGame).status = status
//        dataManager.saveData(applicationContext)
        super.finishActivity(status)
    }

    private fun onOfferDraw() {
        networkManager.sendMessage(NetworkMessage(Topic.DRAW_OFFERED, "$gameId|$userId"))
    }

    private fun onResign() {
        networkManager.sendMessage(NetworkMessage(Topic.RESIGN, "$gameId|$userId"))
        closeAndSaveGameAsLoss()
    }

    override fun onOpponentMoved(output: Parcelable) {
        val moveData = output as MoveData
        if (moveData.gameId == gameId) {
            (game as MultiPlayerGame).moveOpponent(moveData.move, false)
            requestRender()
        } else {
            super.onOpponentMoved(output)
        }
    }

    override fun onUndoRequested(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REQUESTED_UNDO)
        if (gameId == this.gameId) {
            undoRequestedDialog.setMessage("$opponentName is requesting to undo their last move!")
            undoRequestedDialog.show()
        } else {
            super.onUndoRequested(output)
        }
    }

    override fun onUndoAccepted(output: Parcelable) {
        val data = output as ParcelablePair<*, *>
        if (data.second is ParcelableInt) {
            undoAcceptedDialog.show {
                (game as MultiPlayerGame).undoMoves((data.second as ParcelableInt).value)
                (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_ACCEPTED_UNDO)
                (game as MultiPlayerGame).status = GameStatus.PLAYER_MOVE
                requestRender()
            }
        }
    }

    override fun onUndoRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            undoRejectedDialog.show {
                (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REJECTED_UNDO)
            }
        }
    }

    override fun onDrawOffered(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)

        if (gameId == this.gameId) {
            opponentOfferedDrawDialog.show()
        }
    }

    override fun onDrawAccepted(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_ACCEPTED_DRAW)
        if (gameId == this.gameId) {
            opponentAcceptedDrawDialog.show()
        }
    }

    override fun onDrawRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REJECTED_DRAW)
        if (gameId == this.gameId) {
            opponentRejectedDrawDialog.show(opponentName)
        }
    }

    override fun onOpponentResigned(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_RESIGNED)
        if (gameId == this.gameId) {
            opponentResignedDialog.show()
        }
    }

    override fun onChatMessageReceived(output: Parcelable) {
        val messageData = output as ChatMessage.Data

        if (messageData.gameId == this.gameId) {
            getChatFragment()?.addReceivedMessage(ChatMessage(messageData))
        }
    }

    override fun onUserStatusChanged(output: Parcelable) {
        val opponentStatus = (output as ParcelableString).value
        setOpponentStatusIcon(opponentStatus)
    }

    private fun setOpponentStatusIcon(opponentStatus: String) {
        val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for player was found..")

        when (opponentStatus) {
            gameId -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.ONLINE)
            "online" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.ONLINE)
            "away" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.AWAY)
            "offline" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.OFFLINE)
            else -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
        }
    }

    private fun rejectUndoRequest() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REQUESTED_UNDO)
        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.UNDO_REJECTED, "$gameId|$userId"))
    }

    private fun acceptUndoRequest() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REQUESTED_UNDO)
        val numberOfReversedMoves = when ((game as MultiPlayerGame).status) {
            GameStatus.OPPONENT_MOVE -> 2
            GameStatus.PLAYER_MOVE -> 1
            else -> 0
        }

        (game as MultiPlayerGame).undoMoves(numberOfReversedMoves)
        if (game.getMoveIndex() == -1) {
            (getActionBarFragment() as MultiplayerActionButtonsFragment?)?.disableBackButton()
        }
        requestRender()

        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.UNDO_ACCEPTED, "$gameId|$userId"))

    }

    private fun rejectDrawOffer() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.DRAW_REJECTED, "$gameId|$userId"))
    }

    private fun acceptDrawOffer() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
        dataManager[gameId] = game as MultiPlayerGame
        dataManager.saveData(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.DRAW_ACCEPTED, "$gameId|$userId"))
        closeAndSaveGameAsDraw()
    }

    override fun onBackPressed() {
        if (isChatOpened()) {
            closeChat()
        } else {
            super.onBackPressed()
        }
    }

    override fun sendResumeStatusToServer() {
        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|$gameId"))
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