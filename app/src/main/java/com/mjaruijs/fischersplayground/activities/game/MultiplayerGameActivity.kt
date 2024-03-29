package com.mjaruijs.fischersplayground.activities.game

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Constraints
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightObserver
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightProvider
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity.Companion.GAME_PREFERENCES_KEY
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveData
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.chess.pieces.*
import com.mjaruijs.fischersplayground.dialogs.*
import com.mjaruijs.fischersplayground.fragments.ChatFragment
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.PlayerStatus
import com.mjaruijs.fischersplayground.fragments.actionbars.MultiplayerActionButtonsFragment
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelableInt
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.services.NetworkService
import com.mjaruijs.fischersplayground.userinterface.PopupBar
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

class MultiplayerGameActivity : GameActivity(), KeyboardHeightObserver {

    override var activityName = "multiplayer_activity"

    override var isSinglePlayer = true

    private var chatInitialized = false
    private var chatOpened = false
    private var reviewingFinishedGame = false
    private var contextCreated = false

    private var chatButtonWidth = 0
    private var chatBoxWidth = 0

    private lateinit var resignDialog: DoubleButtonDialog
    private lateinit var drawAcceptedDialog: DoubleButtonDialog

    private lateinit var undoRequestedDialog: DoubleButtonDialog
    private lateinit var undoAcceptedDialog: SingleButtonDialog
    private lateinit var undoRejectedDialog: SingleButtonDialog

    private lateinit var undoRequestConfirmationDialog: SingleButtonDialog

    private lateinit var opponentOfferedDrawDialog: DoubleButtonDialog
    private lateinit var opponentAcceptedDrawDialog: DoubleButtonDialog
    private lateinit var opponentRejectedDrawDialog: SingleButtonDialog
    private lateinit var opponentResignedDialog: DoubleButtonDialog

    private lateinit var confirmResignationDialog: DoubleButtonDialog
    private lateinit var offerDrawDialog: DoubleButtonDialog

    private lateinit var confirmMovesPopupBar: PopupBar

    private lateinit var keyboardHeightProvider: KeyboardHeightProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")

            val lowerFragment = findViewById<FragmentContainerView>(R.id.lower_fragment_container)
            val layoutParams = Constraints.LayoutParams(Constraints.LayoutParams.MATCH_CONSTRAINT, Constraints.LayoutParams.WRAP_CONTENT)
            lowerFragment.layoutParams = layoutParams

            val margin = dpToPx(8)

            val constraints = ConstraintSet()
            constraints.clone(gameLayout)
            constraints.connect(R.id.lower_fragment_container, ConstraintSet.TOP, R.id.opengl_view, ConstraintSet.BOTTOM, margin)
            constraints.connect(R.id.lower_fragment_container, ConstraintSet.BOTTOM, R.id.action_buttons_fragment, ConstraintSet.TOP, margin)
            constraints.connect(R.id.lower_fragment_container, ConstraintSet.LEFT, gameLayout.id, ConstraintSet.LEFT, margin)
            constraints.connect(R.id.lower_fragment_container, ConstraintSet.RIGHT, gameLayout.id, ConstraintSet.RIGHT, margin)

            constraints.applyTo(gameLayout)
            keyboardHeightProvider = KeyboardHeightProvider(this)

            confirmMovesPopupBar = findViewById(R.id.extra_buttons_popup_bar)

            val confirmMoveButton = UIButton2(applicationContext)
            confirmMoveButton.setIcon(R.drawable.check_mark_icon)
                .setIconScale(0.5f)
                .setColorResource(R.color.accent_color)
                .setOnClickListener {
                    confirmMovesPopupBar.hide()
                    confirmMove((game as MultiPlayerGame).moveToBeConfirmed)
                }

            val cancelMoveButton = UIButton2(applicationContext)
            cancelMoveButton.setIcon(R.drawable.close_icon)
                .setIconScale(0.5f)
                .setColorResource(R.color.background_color)
                .setOnClickListener {
                    confirmMovesPopupBar.hide()
                    cancelMove()
                }

            confirmMovesPopupBar.addButton(cancelMoveButton)
            confirmMovesPopupBar.addButton(confirmMoveButton)
            confirmMovesPopupBar.attachToLayout(gameLayout)

            findViewById<View>(R.id.game_layout).post {
                Runnable {
                    keyboardHeightProvider.start()
                }.run()
            }
        } catch (e: Exception) {
            NetworkService.sendCrashReport("crash_mp_game_activity_oncreate.txt", e.stackTraceToString(), applicationContext)
        }
    }

    override fun onResume() {
        super.onResume()
        game = dataManager.getGame(gameId)!!
        opponentName = (game as MultiPlayerGame).opponentName
        isPlayingWhite = game.isPlayingWhite

        initializeFragments()
        setGameCallbacks()
        setGameForRenderer()

        initChatBox()

        undoRequestConfirmationDialog = SingleButtonDialog(this, true, "Undo Requested", "You will be notified when $opponentName responds", R.drawable.check_mark_icon)
        confirmResignationDialog = DoubleButtonDialog(this, true, "No Way Back", "Are you sure you want to resign?", "Cancel", "Yes", ::onResignConfirmed)
        offerDrawDialog = DoubleButtonDialog(this, true, "Offer Draw", "Are you sure you want to offer a draw?", "Cancel", "Yes", ::onOfferDraw)

        resignDialog = DoubleButtonDialog(this, true, "$opponentName won!", "You lost by resignation", "View Board", ::viewBoardAfterFinish, "Exit", ::closeAndSaveGameAsLoss)
        drawAcceptedDialog = DoubleButtonDialog(this, true, "It's A Draw!", "You accepted $opponentName's draw offer", "View Board", ::viewBoardAfterFinish, "Exit", ::closeAndSaveGameAsDraw)
        undoRequestedDialog = DoubleButtonDialog(this, false, "Undo Requested", "$opponentName is requesting to undo their last move!", "Reject", ::rejectUndoRequest, "Accept", ::acceptUndoRequest)
        undoAcceptedDialog = SingleButtonDialog(this, false, "Move Reversed", "Your undo request has been accepted!", "Continue")
        undoRejectedDialog = SingleButtonDialog(this, false, "Undo Rejected", "Your undo request was rejected!", "Continue")

        opponentAcceptedDrawDialog = DoubleButtonDialog(this, false, "It's A Draw!", "$opponentName has accepted your draw offer", "View Board", ::viewBoardAfterFinish, "Exit", ::closeAndSaveGameAsDraw)
        opponentRejectedDrawDialog = SingleButtonDialog(this, false, "Game Must Go On", "$opponentName has rejected your draw offer", "Play on")
        opponentOfferedDrawDialog = DoubleButtonDialog(this, false, "Draw Offered", "$opponentName has offered a draw!", "Decline", ::rejectDrawOffer, "Accept", ::acceptDrawOffer)
        opponentResignedDialog = DoubleButtonDialog(this, false, "You Won!", "$opponentName has resigned!", "View Board", ::viewBoardAfterFinish, "Exit", ::closeAndSaveGameAsWin)

        keyboardHeightProvider.observer = this
    }

    override fun onPause() {
        dataManager.setGame(gameId, game as MultiPlayerGame, applicationContext)
        keyboardHeightProvider.observer = null
        super.onPause()
    }

    override fun onDestroy() {
        keyboardHeightProvider.close()
        super.onDestroy()
    }

    override fun onContextCreated() {
        super.onContextCreated()
//        Logger.debug("test", "ContextCreated")
//        showLatestMove()

        contextCreated = true
    }

    private fun showLatestMove() {
        runOnUiThread {
            if ((game as MultiPlayerGame).newsUpdates.none { news -> news.newsType == NewsType.OPPONENT_MOVED }) {
                redoLastMove()
            }

            processNews()
        }
    }

    private fun redoLastMove() {
        Logger.debug("test", "Redoing last move")
        if (!(game as MultiPlayerGame).hasNews(NewsType.OPPONENT_MOVED)) {
            game.showPreviousMove(true)
            game.showNextMove(false)
        }

        getActionBarFragment()?.disableForwardButton()
    }

    override fun setGameCallbacks() {
        super.setGameCallbacks()
        game.onCheckMate = ::onCheckMate
        game.onPieceTaken = ::onPieceTaken
        game.onPieceRegained = ::onPieceRegained
    }

    private fun initializeFragments() {
        loadFragments()

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
                setOpponentStatusIcon((game as MultiPlayerGame).opponentStatus)

                evaluateNavigationButtons()

                getPlayerFragment()!!.removeAllPieces()
                getOpponentFragment()!!.removeAllPieces()

                for (takenPiece in game.takenPieces) {
                    Logger.debug(activityName, "Adding back taken piece: ${takenPiece.team} ${takenPiece.type}")
                    onPieceTaken(takenPiece)
                }

//                if (contextCreated) {
//                    Logger.debug("test", "InitializeFragments()")
                    showLatestMove()
//                }
            }
        }.start()
    }

    private fun loadFragments() {
        val playerBundle = Bundle()
        playerBundle.putString("player_name", userName)
        playerBundle.putString("team", if (isPlayingWhite) "WHITE" else "BLACK")
        playerBundle.putBoolean("hide_status_icon", true)

        val opponentBundle = Bundle()
        opponentBundle.putString("player_name", opponentName)
        opponentBundle.putString("team", if (isPlayingWhite) "BLACK" else "WHITE")
        opponentBundle.putBoolean("hide_status_icon", isSinglePlayer)

        val chatFragment = ChatFragment()
        chatFragment.gameId = gameId
        chatFragment.onMessageSent = ::onChatMessageSent
        chatFragment.close = ::closeChat

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.lower_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
            replace(R.id.upper_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
            replace(R.id.chat_container, chatFragment)
            replace(R.id.action_buttons_fragment, MultiplayerActionButtonsFragment.getInstance(game, ::evaluateNavigationButtons, ::onRequestUndo, ::onOfferDrawClicked, ::onResignClicked, ::cancelMove, ::confirmMove))
        }
    }

    override fun evaluateNavigationButtons() {
        super.evaluateNavigationButtons()
        if ((game as MultiPlayerGame).isFinished()) {
            val actionBar = (getActionBarFragment() as MultiplayerActionButtonsFragment)
            actionBar.disableUndoButton()
            actionBar.disableDrawButton()
            actionBar.disableResignButton()
        }
    }

    private fun onPieceTaken(piece: Piece) = onPieceTaken(piece.type, piece.team)

    private fun onPieceTaken(pieceType: PieceType, team: Team) {
        if ((isPlayingWhite && team == Team.WHITE) || (!isPlayingWhite && team == Team.BLACK)) {
            getOpponentFragment()?.addTakenPiece(pieceType)
        } else if ((isPlayingWhite && team == Team.BLACK) || (!isPlayingWhite && team == Team.WHITE)) {
            getPlayerFragment()?.addTakenPiece(pieceType)
        }
    }

    private fun onPieceRegained(pieceType: PieceType, team: Team) {
        if ((isPlayingWhite && team == Team.WHITE) || (!isPlayingWhite && team == Team.BLACK)) {
            val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "player" } ?: throw IllegalArgumentException("No fragment for player was found..")
            (opponentFragment as PlayerCardFragment).removeTakenPiece(pieceType)
        } else if ((isPlayingWhite && team == Team.BLACK) || (!isPlayingWhite && team == Team.WHITE)) {
            val playerFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for opponent was found..")
            (playerFragment as PlayerCardFragment).removeTakenPiece(pieceType)
        }
    }

    private fun sendMoveData(moveNotation: String) {
        val showPopup = getPreference(GAME_PREFERENCES_KEY).getBoolean(SettingsActivity.CONFIRM_MOVES_KEY, false)
        if (showPopup) {
            runOnUiThread {
                confirmMovesPopupBar.show()
//                (getActionBarFragment() as MultiplayerActionButtonsFragment).showExtraButtons(moveNotation)
            }
        } else {
            confirmMove(moveNotation)
        }
    }

    override fun onMoveMade(move: Move) {
        super.onMoveMade(move)
        if (move.team == game.getCurrentTeam()) {
            val positionUpdateMessage = move.toChessNotation()
            (game as MultiPlayerGame).moveToBeConfirmed = positionUpdateMessage

            sendMoveData(positionUpdateMessage)
        }

//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
    }

    private fun confirmMove(moveNotation: String) {
        sendNetworkMessage(NetworkMessage(Topic.MOVE, "$gameId|$userId|$moveNotation|${game.lastUpdated}"))
        (game as MultiPlayerGame).confirmMove()
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
    }

    private fun cancelMove() {
        (game as MultiPlayerGame).cancelMove()
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
    }

    override fun onClick(x: Float, y: Float) {
//        if (confirmMovesPopupBar.isShowing) {
//            confirmMovesPopupBar.hide()
//        } else {
//            confirmMovesPopupBar.show()
//        }
        if (isChatOpened()) {
            return
        }
        super.onClick(x, y)
    }

    private fun processNews() {
        for (news in (game as MultiPlayerGame).newsUpdates) {
            when (news.newsType) {
                NewsType.OPPONENT_MOVED -> {
                    val moveData = news.getData<MoveData>()
                    if (moveData.gameId == gameId) {
                        val move = moveData.move
                        (game as MultiPlayerGame).moveOpponent(move)
                        saveGame()
//                        dataManager.setGame(gameId, game as MultiPlayerGame)
                    }
                }
                NewsType.OPPONENT_RESIGNED -> {
                    (game as MultiPlayerGame).status = GameStatus.GAME_WON
                    opponentResignedDialog.show()
                }
                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show()
                NewsType.OPPONENT_ACCEPTED_DRAW -> {
                    (game as MultiPlayerGame).status = GameStatus.GAME_DRAW
                    opponentAcceptedDrawDialog.show()
                }
                NewsType.OPPONENT_REJECTED_DRAW -> opponentRejectedDrawDialog.show()
                NewsType.OPPONENT_REQUESTED_UNDO -> {
                    undoRequestedDialog.setMessage("$opponentName is requesting to undo their last move!")
                    undoRequestedDialog.show()
                }
                NewsType.OPPONENT_ACCEPTED_UNDO -> {
                    undoAcceptedDialog.show {
                        (game as MultiPlayerGame).undoMoves(news.getData())
                        (game as MultiPlayerGame).status = GameStatus.PLAYER_MOVE
                        requestRender()
                    }
                }
                NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
                NewsType.CHAT_MESSAGE -> {
                    findViewById<ImageView>(R.id.chat_update_icon).visibility = View.VISIBLE
                }
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
        (getActionBarFragment() as MultiplayerActionButtonsFragment).disableResignButton()
        (getActionBarFragment() as MultiplayerActionButtonsFragment).disableDrawButton()
        (getActionBarFragment() as MultiplayerActionButtonsFragment).disableUndoButton()
        reviewingFinishedGame = true
        super.viewBoardAfterFinish()
    }

    private fun onOfferDrawClicked() {
        if (isChatOpened()) {
            return
        }

        offerDrawDialog.show()
    }

    private fun onOfferDraw() {
        sendNetworkMessage(NetworkMessage(Topic.DRAW_OFFERED, "$gameId|$userId"))
    }

    private fun onResignClicked() {
        if (isChatOpened()) {
            return
        }

        confirmResignationDialog.show()
    }

    private fun onResignConfirmed() {
        (game as MultiPlayerGame).status = GameStatus.GAME_LOST
        sendNetworkMessage(NetworkMessage(Topic.RESIGN, "$gameId|$userId"))
        resignDialog.show()
    }

    private fun onRequestUndo() {
        if (isChatOpened()) {
            return
        }

        undoRequestConfirmationDialog.show()
        sendNetworkMessage(NetworkMessage(Topic.UNDO_REQUESTED, "$gameId|$userId"))
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
        val gameId = (output.first as ParcelableString).value
        if (gameId != this.gameId) {
            super.onUndoAccepted(output)
        } else {
            if (data.second is ParcelableInt) {
                undoRequestConfirmationDialog.dismiss()
                undoAcceptedDialog.show {
                    (game as MultiPlayerGame).undoMoves((data.second as ParcelableInt).value)
                    (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_ACCEPTED_UNDO)
                    (game as MultiPlayerGame).status = GameStatus.PLAYER_MOVE
                    requestRender()
                }
            }
        }
    }

    override fun onUndoRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            undoRequestConfirmationDialog.dismiss()
            undoRejectedDialog.show {
                (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REJECTED_UNDO)
            }
        }
    }

    override fun onDrawOffered(output: Parcelable) {
        val gameId = (output as ParcelableString).value

        if (gameId == this.gameId) {
            (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
            opponentOfferedDrawDialog.show()
        }
    }

    override fun onDrawAccepted(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_ACCEPTED_DRAW)
            (game as MultiPlayerGame).status = GameStatus.GAME_DRAW
            opponentAcceptedDrawDialog.show()
        }
    }

    override fun onDrawRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_REJECTED_DRAW)
            opponentRejectedDrawDialog.show()
        }
    }

    override fun onOpponentResigned(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        if (gameId == this.gameId) {
            (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_RESIGNED)
            (game as MultiPlayerGame).status = GameStatus.GAME_WON
            opponentResignedDialog.show()
        }
    }

    override fun onChatMessageReceived(output: Parcelable) {
        val message = output as ChatMessage

        if (message.gameId == this.gameId) {
            getChatFragment()!!.addReceivedMessage(message)
            (game as MultiPlayerGame).clearNews(NewsType.CHAT_MESSAGE)
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
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
        sendNetworkMessage(NetworkMessage(Topic.UNDO_REJECTED, "$gameId|$userId"))
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
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
        sendNetworkMessage(NetworkMessage(Topic.UNDO_ACCEPTED, "$gameId|$userId"))
    }

    private fun rejectDrawOffer() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
        sendNetworkMessage(NetworkMessage(Topic.DRAW_REJECTED, "$gameId|$userId"))
    }

    private fun acceptDrawOffer() {
        (game as MultiPlayerGame).clearNews(NewsType.OPPONENT_OFFERED_DRAW)
        (game as MultiPlayerGame).status = GameStatus.GAME_DRAW
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
        sendNetworkMessage(NetworkMessage(Topic.DRAW_ACCEPTED, "$gameId|$userId"))
        drawAcceptedDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isChatOpened()) {
            closeChat()
        } else {
            super.onBackPressed()
        }
    }

    private fun saveGame() {
        dataManager.setGame(gameId, game as MultiPlayerGame, applicationContext)
//        sendToDataManager(DataManagerService.Request.SET_GAME, Pair("game_id", gameId), Pair("game", game))
    }

    override fun sendResumeStatusToServer() {
        sendNetworkMessage(NetworkMessage(Topic.USER_STATUS_CHANGED, "$userId|$gameId"))
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

            Thread {
                while (getActionBarFragment() == null) {
                    Thread.sleep(10)
                }

                runOnUiThread {
//                    val buttonHeight = getActionBarFragment()!!.requireView().measuredHeight
//                    (getActionBarFragment() as MultiplayerActionButtonsFragment).initializeAnimator(buttonHeight)

                    if ((game as MultiPlayerGame).hasPendingMove()) {
                        runOnUiThread {
                            confirmMovesPopupBar.show()
//                            (getActionBarFragment() as MultiplayerActionButtonsFragment).showExtraButtons((game as MultiPlayerGame).moveToBeConfirmed, 0L)
                        }
                    }
                }
            }.start()
        }
    }

    private fun getChatFragment(): ChatFragment? {
        return (supportFragmentManager.fragments.find { fragment -> fragment is ChatFragment } as ChatFragment?)
    }

    private fun isChatOpened(): Boolean {
        return chatOpened
    }

    private fun onChatMessageSent(message: ChatMessage) {
        sendNetworkMessage(NetworkMessage(Topic.CHAT_MESSAGE, "$gameId|$userId|${message.timeStamp}|${message.message}"))
        (game as MultiPlayerGame).addMessage(message)
        saveGame()
//        dataManager.setGame(gameId, game as MultiPlayerGame)
//        dataManager.saveData(applicationContext)
    }

    private fun closeChat() {
        val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", (chatButtonWidth + chatBoxWidth).toFloat())
        val chatButtonAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.open_chat_button), "x", chatBoxWidth.toFloat())

        chatBoxAnimator.duration = 250L
        chatButtonAnimator.duration = 250L

        chatBoxAnimator.start()
        chatButtonAnimator.start()

        evaluateNavigationButtons()

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

            chatBoxAnimator.duration = 250L
            chatButtonAnimator.duration = 250L

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