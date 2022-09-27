package com.mjaruijs.fischersplayground.activities.game

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity.Companion.GAME_PREFERENCES_KEY
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightObserver
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightProvider
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.IntNews
import com.mjaruijs.fischersplayground.chess.news.MoveNews
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.chess.pieces.Team
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
    private var reviewingFinishedGame = false
    private var contextCreated = false

    private var chatButtonWidth = 0
    private var chatBoxWidth = 0

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
            game = dataManager.getGame(gameId)!!
            opponentName = (game as MultiPlayerGame).opponentName
            isPlayingWhite = game.isPlayingWhite

            initChatBox()

            val chatFragment = ChatFragment()
            chatFragment.onMessageSent = ::onChatMessageSent
            chatFragment.close = ::closeChat

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.chat_container, chatFragment)
                replace(R.id.action_buttons_fragment, MultiplayerActionButtonsFragment(gameId, userId, opponentName, ::isChatOpened, ::onOfferDraw, ::onResign, ::cancelMove, ::confirmMove, ::requestRender, networkManager))
            }

            keyboardHeightProvider = KeyboardHeightProvider(this)
            findViewById<View>(R.id.game_layout).post {
                Runnable {
                    keyboardHeightProvider.start()
                }.run()
            }
        } catch (e: Exception) {
            FileManager.append(this,  "mp_game_activity_on_create_crash.txt", e.stackTraceToString())
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            game = dataManager.getGame(gameId)!!

            opponentName = (game as MultiPlayerGame).opponentName
            isPlayingWhite = game.isPlayingWhite

            initializeFragments()
            setGameCallbacks()
            setGameForRenderer()

            if (contextCreated) {
                redoLastMove()
            }

            undoRequestedDialog = DoubleButtonDialog(this, "Undo Requested", "Reject", ::rejectUndoRequest, "Accept", ::acceptUndoRequest, 0.5f)
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
        } catch (e: Exception) {
//            FileManager.append(this,  "mp_game_activity_on_resume_crash.txt", e.stackTraceToString())
            throw e
        }
    }

    override fun onPause() {
        dataManager.setGame(gameId, game as MultiPlayerGame)
        keyboardHeightProvider.observer = null
        super.onPause()
    }

    override fun onDestroy() {
        keyboardHeightProvider.close()
        super.onDestroy()
    }

    override fun onContextCreated() {
        super.onContextCreated()

        redoLastMove()

        contextCreated = true
    }

    private fun redoLastMove() {
        if (!(game as MultiPlayerGame).hasNews(NewsType.OPPONENT_MOVED)) {
            game.showPreviousMove(true)
            game.showNextMove()
        }

        getActionBarFragment()?.disableForwardButton()
    }

    private fun initializeFragments() {
        if (loadFragments) {
            loadFragments()
        }

        Thread {
            while (getChatFragment() == null) {
                Thread.sleep(1)
            }

            runOnUiThread {
                getChatFragment()?.clearMessages()
                getChatFragment()?.addMessages((game as MultiPlayerGame).chatMessages)
            }
        }.start()

        Thread {
            while (getActionBarFragment() == null) {
                Thread.sleep(1)
            }

            runOnUiThread {
                getActionBarFragment()?.game = game
                setOpponentStatusIcon((game as MultiPlayerGame).opponentStatus)

                evaluateActionButtons()

                getPlayerFragment()!!.removeAllPieces()
                getOpponentFragment()!!.removeAllPieces()

                for (takenPiece in game.takenPieces) {
                    onPieceTaken(takenPiece)
                }
                Log.i("MP", game.takenPieces.size.toString())
            }
        }.start()
    }

    override fun evaluateActionButtons() {
        super.evaluateActionButtons()
        if ((game as MultiPlayerGame).isFinished()) {
            val actionBar = (getActionBarFragment() as MultiplayerActionButtonsFragment)
            actionBar.disableUndoButton()
            actionBar.disableDrawButton()
            actionBar.disableResignButton()
        }
    }

    private fun sendMoveData(moveNotation: String) {
        val showPopup = getPreference(GAME_PREFERENCES_KEY).getBoolean(SettingsActivity.CONFIRM_MOVES_KEY, false)
        if (showPopup) {
            runOnUiThread {
                (getActionBarFragment() as MultiplayerActionButtonsFragment).showExtraButtons(moveNotation)
            }
        } else {
            confirmMove(moveNotation)
        }
    }

    override fun onMoveMade(move: Move) {
        if (move.team == game.getCurrentTeam()) {
            val positionUpdateMessage = move.toChessNotation()
            (game as MultiPlayerGame).moveToBeConfirmed = positionUpdateMessage
            sendMoveData(positionUpdateMessage)
        }
        dataManager.setGame(gameId, game as MultiPlayerGame)
        dataManager.saveData(applicationContext)
    }

    private fun confirmMove(moveNotation: String) {
        networkManager.sendMessage(NetworkMessage(Topic.MOVE, "$gameId|$userId|$moveNotation|${game.lastUpdated}"))
        (game as MultiPlayerGame).confirmMove()
        dataManager.setGame(gameId, game as MultiPlayerGame)
        dataManager.saveData(applicationContext)
    }

    private fun cancelMove() {
        (game as MultiPlayerGame).cancelMove()
        dataManager.setGame(gameId, game as MultiPlayerGame)
        dataManager.saveData(applicationContext)
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
                NewsType.OPPONENT_MOVED -> {
                    if ((news as MoveNews).moveData.gameId == gameId) {
                        val move = news.moveData.move
                        (game as MultiPlayerGame).moveOpponent(move)
                        dataManager.setGame(gameId, game as MultiPlayerGame)
                    }
                }
                NewsType.OPPONENT_RESIGNED -> opponentResignedDialog.show()
                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show()
                NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show()
                NewsType.OPPONENT_REJECTED_DRAW -> opponentRejectedDrawDialog.show()
                NewsType.OPPONENT_REQUESTED_UNDO -> {
                    undoRequestedDialog.setMessage("$opponentName is requesting to undo their last move!")
                    undoRequestedDialog.show()
                }
                NewsType.OPPONENT_ACCEPTED_UNDO -> {
                    undoAcceptedDialog.show {
                        (game as MultiPlayerGame).undoMoves((news as IntNews).data)
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

    override fun onKeyboardHeightChanged(height: Int) {
        getChatFragment()?.translate(height)
    }

    override fun finishActivity(status: GameStatus) {
        (game as MultiPlayerGame).status = status
        super.finishActivity(status)
    }

    override fun onCheckMate(team: Team) {
        if (!reviewingFinishedGame) {
            super.onCheckMate(team)
        }
    }

    override fun viewBoardAfterFinish() {
        reviewingFinishedGame = true
        super.viewBoardAfterFinish()
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
            (game as MultiPlayerGame).moveOpponent(moveData.move)
            (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_MOVED)
            requestRender()
        } else {
            super.onOpponentMoved(output)
        }
    }

    override fun onUndoRequested(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REQUESTED_UNDO)
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
            opponentRejectedDrawDialog.show()
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
            val message = ChatMessage(messageData)
            (game as MultiPlayerGame).addMessage(message)
            getChatFragment()!!.addReceivedMessage(message)
            if (!chatOpened) {
                findViewById<ImageView>(R.id.chat_update_icon).visibility = View.VISIBLE
            }
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
        dataManager.setGame(gameId, game as MultiPlayerGame)
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

        dataManager.setGame(gameId, game as MultiPlayerGame)
        dataManager.saveData(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.UNDO_ACCEPTED, "$gameId|$userId"))
    }

    private fun rejectDrawOffer() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
        dataManager.setGame(gameId, game as MultiPlayerGame)
        dataManager.saveData(applicationContext)
        networkManager.sendMessage(NetworkMessage(Topic.DRAW_REJECTED, "$gameId|$userId"))
    }

    private fun acceptDrawOffer() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
        dataManager.setGame(gameId, game as MultiPlayerGame)
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
//        networkManager.sendMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|$gameId"))
    }

    override fun onDisplaySizeChanged(width: Int, height: Int) {
        super.onDisplaySizeChanged(width, height)

        if (!chatInitialized) {
            val chatFragment = findViewById<FragmentContainerView>(R.id.chat_container)
            val openChatButton = findViewById<ImageView>(R.id.open_chat_button)
            val chatNotificationIcon = findViewById<ImageView>(R.id.chat_update_icon) ?: return

            chatButtonWidth = openChatButton.width
            chatBoxWidth = chatFragment.width

            chatFragment.translationX = (chatBoxWidth + chatButtonWidth).toFloat()
            openChatButton.translationX = chatBoxWidth.toFloat()
            chatNotificationIcon.x += chatBoxWidth

            chatInitialized = true

            val buttonHeight = getActionBarFragment()!!.requireView().measuredHeight
            (getActionBarFragment() as MultiplayerActionButtonsFragment).initializeAnimator(buttonHeight)

            if ((game as MultiPlayerGame).hasPendingMove()) {
                runOnUiThread {
                    (getActionBarFragment() as MultiplayerActionButtonsFragment).showExtraButtons((game as MultiPlayerGame).moveToBeConfirmed, 0L)
                }
            }
        }
    }

    private fun getChatFragment(): ChatFragment? {
        return (supportFragmentManager.fragments.find { fragment -> fragment is ChatFragment } as ChatFragment?)
    }

    private fun isChatOpened(): Boolean {
        return chatOpened
    }

    private fun onChatMessageSent(message: ChatMessage) {
        networkManager.sendMessage(NetworkMessage(Topic.CHAT_MESSAGE, "$gameId|$userId|${message.timeStamp}|${message.message}"))
        (game as MultiPlayerGame).addMessage(message)
    }

    private fun closeChat() {
        val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", (chatButtonWidth + chatBoxWidth).toFloat())
        val chatButtonAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.open_chat_button), "x", chatBoxWidth.toFloat())

        chatBoxAnimator.duration = 500L
        chatButtonAnimator.duration = 500L

        chatBoxAnimator.start()
        chatButtonAnimator.start()

        evaluateActionButtons()

        chatOpened = false
    }

    private fun initChatBox() {
        val openChatButton = findViewById<ImageView>(R.id.open_chat_button) ?: return

        openChatButton.setOnClickListener {
            findViewById<ImageView>(R.id.chat_update_icon).visibility = View.GONE
            val chatBoxEndX = if (chatOpened) (chatBoxWidth + chatButtonWidth) else chatButtonWidth
            val chatButtonEndX = if (chatOpened) chatBoxWidth else 0

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