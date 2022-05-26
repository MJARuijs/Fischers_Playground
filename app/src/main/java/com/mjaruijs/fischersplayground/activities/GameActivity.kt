package com.mjaruijs.fischersplayground.activities

import android.animation.ObjectAnimator
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightObserver
import com.mjaruijs.fischersplayground.activities.keyboard.KeyboardHeightProvider
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.SavedGames
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.*
import com.mjaruijs.fischersplayground.fragments.ChatFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.news.News
import com.mjaruijs.fischersplayground.news.NewsType
import com.mjaruijs.fischersplayground.opengl.SurfaceView
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.userinterface.UIButton

class GameActivity : AppCompatActivity(R.layout.activity_game), KeyboardHeightObserver {

    private val inviteReceiver = MessageReceiver(Topic.INFO, "invite", ::onIncomingInvite)
    private val newGameReceiver = MessageReceiver(Topic.INFO, "new_game", ::onNewGameStarted)
    private val gameUpdateReceiver = MessageReceiver(Topic.GAME_UPDATE, "move", ::onOpponentMoved)
    private val requestUndoReceiver = MessageReceiver(Topic.GAME_UPDATE, "request_undo", ::onUndoRequested)
    private val undoAcceptedReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_undo", ::onUndoAccepted)
    private val undoRejectedReceiver = MessageReceiver(Topic.GAME_UPDATE, "rejected_undo", ::onUndoRejected)
    private val opponentResignedReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_resigned", ::onOpponentResigned)
    private val opponentOfferedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "opponent_offered_draw", ::onOpponentOfferedDraw)
    private val opponentAcceptedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "accepted_draw", ::onOpponentAcceptedDraw)
    private val opponentDeclinedDrawReceiver = MessageReceiver(Topic.GAME_UPDATE, "declined_draw", ::onOpponentDeclinedDraw)
    private val chatMessageReceiver = MessageReceiver(Topic.CHAT_MESSAGE, "", ::onChatMessageReceived)

    private val incomingInviteDialog = IncomingInviteDialog()
    private val undoRequestedDialog = UndoRequestedDialog()
    private val undoRejectedDialog = UndoRejectedDialog()
    private val resignDialog = ResignDialog()
    private val offerDrawDialog = OfferDrawDialog()
    private val opponentResignedDialog = OpponentResignedDialog()
    private val opponentOfferedDrawDialog = OpponentOfferedDrawDialog()
    private val opponentAcceptedDrawDialog = OpponentAcceptedDrawDialog()
    private val opponentDeclinedDrawDialog = OpponentDeclinedDrawDialog()
    private val checkMateDialog = CheckMateDialog()
    private val pieceChooserDialog = PieceChooserDialog(::onPawnUpgraded)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")

    private var displayWidth = 0
    private var displayHeight = 0

    private var chatInitialized = false
    private var chatOpened = false
    private var chatTranslation = 0

    private var isSinglePlayer = false
    private var isPlayingWhite = false

    private lateinit var id: String
    private lateinit var userName: String
    private lateinit var gameId: String
    private lateinit var opponentName: String
//    private lateinit var opponentId: String

    //    private lateinit var board: Board
    private lateinit var game: Game

    private lateinit var glView: SurfaceView

    private lateinit var keyboardHeightProvider: KeyboardHeightProvider

//    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActivityDecorations()

        println("HELLOOOO???")
        keyboardHeightProvider = KeyboardHeightProvider(this)
        findViewById<View>(R.id.game_layout).post {
            Runnable {
                keyboardHeightProvider.start()
            }.run()
        }

        incomingInviteDialog.create(this)
        undoRequestedDialog.create(this)
        undoRejectedDialog.create(this)
        resignDialog.create(this)
        offerDrawDialog.create(this)
        opponentResignedDialog.create(this)
        opponentOfferedDrawDialog.create(this)
        opponentAcceptedDrawDialog.create(this)
        opponentDeclinedDrawDialog.create(this)
        checkMateDialog.create(this)
        pieceChooserDialog.create(this)

        if (!intent.hasExtra("is_playing_white")) {
            throw IllegalArgumentException("Missing essential information: is_player_white")
        }

        id = intent.getStringExtra("id") ?: throw IllegalArgumentException("Missing essential information: id")
        userName = intent.getStringExtra("user_name") ?: throw IllegalArgumentException("Missing essential information: user_name")
        opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information: opponent_name")
//        opponentId = intent.getStringExtra("opponent_id") ?: throw IllegalArgumentException("Missing essential information: opponent_id")
        gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")
        isSinglePlayer = intent.getBooleanExtra("is_single_player", false)
        isPlayingWhite = intent.getBooleanExtra("is_playing_white", false)

        glView = findViewById(R.id.opengl_view)
        glView.init(::onContextCreated, ::onClick, ::onDisplaySizeChanged)

        initUIButtons()
        initChatBox()

        if (savedInstanceState == null) {
            val playerBundle = Bundle()
            playerBundle.putString("player_name", userName)

            val opponentBundle = Bundle()
            opponentBundle.putString("player_name", opponentName)

            val chatBundle = Bundle()
            chatBundle.putString("game_id", gameId)
            chatBundle.putString("user_id", id)

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.player_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
                replace(R.id.opponent_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
                replace(R.id.chat_container, ChatFragment(::onChatMessageSent))
            }
        }
    }

    private fun onContextCreated() {
        if (isSinglePlayer) {
            game = SinglePlayerGame()
        } else {
            game = SavedGames.get(gameId) ?: MultiPlayerGame(gameId, id, opponentName, isPlayingWhite)

            runOnUiThread {
                getChatFragment().addMessages((game as MultiPlayerGame).chatMessages)
            }
        }

        game.onPawnPromoted = ::onPawnPromoted
        game.enableBackButton = ::enableBackButton
        game.enableForwardButton = ::enableForwardButton
        game.onPieceTaken = ::onPieceTaken
        game.onCheckMate = ::onCheckMate
//        game.onCheck = ::onCheck
//        game.onCheckCleared = ::onCheckCleared

//        board = Board { square ->
//            val possibleMoves = game.determinePossibleMoves(square, game.getCurrentTeam())
//            board.updatePossibleMoves(possibleMoves)
//        }

        glView.setGameState(game)
        glView.setBoard(game.board)

        if (game is MultiPlayerGame) {
            runOnUiThread {
                processNews((game as MultiPlayerGame).news)
                (game as MultiPlayerGame).news = News(NewsType.NO_NEWS)
            }
        }
    }

    private fun onPawnUpgraded(square: Vector2, pieceType: PieceType, team: Team) {
        game.upgradePawn(square, pieceType, team)
        Thread {
            Thread.sleep(10)
            glView.invalidate()
            glView.requestRender()
        }.start()
    }

    private fun getChatFragment(): ChatFragment {
        return (supportFragmentManager.fragments.find { fragment -> fragment is ChatFragment } as ChatFragment)
    }

    private fun isChatOpened(): Boolean {
        return chatOpened
    }

    private fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height

        if (!chatInitialized) {
            val chatFragment = findViewById<FragmentContainerView>(R.id.chat_container)

            val openChatButton = findViewById<ImageView>(R.id.open_chat_button)
            val chatButtonWidth = openChatButton.width
            chatTranslation = displayWidth - chatButtonWidth

            chatFragment.translationX -= chatTranslation
            openChatButton.translationX -= chatTranslation
            chatInitialized = true
        }
    }

    private fun onClick(x: Float, y: Float) {
        if (isChatOpened()) {
            return
        }

        game.onClick(x, y, displayWidth, displayHeight)

//        val clickAction = board.onClick(x, y, displayWidth, displayHeight)
//        val boardAction = game.processAction(clickAction)

//        board.processAction(boardAction)
    }

    private fun onCheck(square: Vector2) {
//        board.checkedKingSquare = square
    }

    private fun onCheckCleared() {
//        board.checkedKingSquare = Vector2(-1, 1)
    }

    private fun onCheckMate(team: Team) {
        if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
            checkMateDialog.show(userName, ::closeAndSaveGameAsWin)
        } else {
            checkMateDialog.show(opponentName, ::closeAndSaveGameAsLoss)
        }
    }

    private fun onPieceTaken(pieceType: PieceType, team: Team) {
        if ((isPlayingWhite && team == Team.WHITE) || (!isPlayingWhite && team == Team.BLACK)) {
            val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for player was found..")
            (opponentFragment as PlayerCardFragment).addTakenPiece(pieceType, team)
        } else if ((isPlayingWhite && team == Team.BLACK) || (!isPlayingWhite && team == Team.WHITE)) {
            val playerFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "player" } ?: throw IllegalArgumentException("No fragment for opponent was found..")
            (playerFragment as PlayerCardFragment).addTakenPiece(pieceType, team)
        }
    }

    private fun processNews(news: News) {
        when (news.newsType) {
            NewsType.OPPONENT_RESIGNED -> opponentResignedDialog.show(opponentName, ::closeAndSaveGameAsWin)
            NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show(gameId, id, opponentName, ::acceptDraw)
            NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show(gameId, opponentName, ::closeAndSaveGameAsDraw)
            NewsType.OPPONENT_DECLINED_DRAW -> opponentDeclinedDrawDialog.show(opponentName)
            NewsType.OPPONENT_REQUESTED_UNDO -> undoRequestedDialog.show(gameId, opponentName, id)
            NewsType.OPPONENT_ACCEPTED_UNDO -> {
                (game as MultiPlayerGame).reverseMoves(news.data)
                glView.requestRender()
            }
            NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
            NewsType.NO_NEWS -> {}
        }
    }

    private fun onNewGameStarted(content: String) {
        val data = content.split('|')

        val inviteId = data[0]
        val opponentName = data[1]
        val playingWhite = data[2].toBoolean()

        val underscoreIndex = inviteId.indexOf('_')
        val invitingUserId = inviteId.substring(0, underscoreIndex)

        val newGameStatus = if (playingWhite) {
            GameStatus.PLAYER_MOVE
        } else {
            GameStatus.OPPONENT_MOVE
        }

        SavedGames.put(inviteId, MultiPlayerGame(inviteId, id, opponentName, playingWhite))
    }

    private fun onOpponentResigned(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentResignedDialog.show(opponentUsername, ::closeAndSaveGameAsWin)
        } else {
            SavedGames.get(gameId)?.status = GameStatus.PLAYER_MOVE
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_RESIGNED)
        }
    }

    private fun onOpponentOfferedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentOfferedDrawDialog.show(gameId, id, opponentUsername, ::acceptDraw)
        } else {
            SavedGames.get(gameId)?.status = GameStatus.PLAYER_MOVE
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_OFFERED_DRAW)
        }
    }

    private fun onOpponentAcceptedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentAcceptedDrawDialog.show(gameId, opponentUsername, ::closeAndSaveGameAsDraw)
        } else {
            SavedGames.get(gameId)?.status = GameStatus.PLAYER_MOVE
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_ACCEPTED_DRAW)
        }
    }

    private fun onOpponentDeclinedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentDeclinedDrawDialog.show(opponentUsername)
        } else {
            SavedGames.get(gameId)?.status = GameStatus.PLAYER_MOVE
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_DECLINED_DRAW)
        }
    }

    private fun onOpponentMoved(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val moveNotation = data[1]
        val move = Move.fromChessNotation(moveNotation)

        if (this.gameId == gameId) {
            (game as MultiPlayerGame).moveOpponent(move, false)
            glView.requestRender()
        } else {
            val game = SavedGames.get(gameId) ?: throw IllegalArgumentException("Could not find game with id: $gameId")
            game.moveOpponent(move, false)

            SavedGames.put(gameId, game)
        }
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val invitingUsername = data[0]
        val invitingUserId = data[1]
        val inviteId = data[2]

        incomingInviteDialog.showInvite(invitingUsername, inviteId)
    }

    private fun onUndoRequested(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]
        val opponentUserId = data[2]

        if (this.gameId == gameId) {
            undoRequestedDialog.show(gameId, opponentUsername, id)
        } else {
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_REQUESTED_UNDO)
        }
    }

    private fun onUndoAccepted(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val numberOfMovesReversed = data[1].toInt()

        if (this.gameId == gameId) {
            (game as MultiPlayerGame).reverseMoves(numberOfMovesReversed)
            glView.requestRender()
        } else {
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed)
            SavedGames.get(gameId)?.status = GameStatus.PLAYER_MOVE
        }
    }

    private fun onUndoRejected(gameId: String) {
        if (this.gameId == gameId) {
            undoRejectedDialog.show(opponentName)
        } else {
            SavedGames.get(gameId)?.news = News(NewsType.OPPONENT_REJECTED_UNDO)
        }
    }

    private fun onChatMessageReceived(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        val message = ChatMessage(timeStamp, messageContent, MessageType.RECEIVED)

        if (this.gameId == gameId) {
            getChatFragment().addReceivedMessage(message)
        } else {
            SavedGames.get(gameId)?.chatMessages?.add(message)
        }
    }

    private fun onChatMessageSent(message: ChatMessage) {
        NetworkManager.sendMessage(Message(Topic.CHAT_MESSAGE, "", "$gameId|$id|${message.timeStamp}|${message.message}"))
        if (game is MultiPlayerGame) {
            (game as MultiPlayerGame).chatMessages += message
        }
    }

    private fun closeAndSaveGameAsWin() {
        SavedGames.get(gameId)?.status = GameStatus.GAME_WON
        finish()
    }

    private fun closeAndSaveGameAsDraw() {
        SavedGames.get(gameId)?.status = GameStatus.GAME_DRAW
        finish()
    }

    private fun closeAndSaveGameAsLoss() {
        SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
        finish()
    }

    private fun acceptDraw() {
        NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "accepted_draw", "$gameId|$id"))
        closeAndSaveGameAsDraw()
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

    private fun saveGame() {
        println("SAVING GAME: ${(game as MultiPlayerGame).chatMessages.size}")
        SavedGames.put(gameId, game as MultiPlayerGame)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(newGameReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
        registerReceiver(requestUndoReceiver, gameUpdateFilter)
        registerReceiver(undoAcceptedReceiver, gameUpdateFilter)
        registerReceiver(undoRejectedReceiver, gameUpdateFilter)
        registerReceiver(opponentResignedReceiver, gameUpdateFilter)
        registerReceiver(opponentOfferedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentAcceptedDrawReceiver, gameUpdateFilter)
        registerReceiver(opponentDeclinedDrawReceiver, gameUpdateFilter)
        registerReceiver(chatMessageReceiver, chatFilter)

        keyboardHeightProvider.observer = this
    }

    override fun onStop() {
        unregisterReceiver(inviteReceiver)
        unregisterReceiver(newGameReceiver)
        unregisterReceiver(gameUpdateReceiver)
        unregisterReceiver(requestUndoReceiver)
        unregisterReceiver(undoAcceptedReceiver)
        unregisterReceiver(undoRejectedReceiver)
        unregisterReceiver(opponentResignedReceiver)
        unregisterReceiver(opponentOfferedDrawReceiver)
        unregisterReceiver(opponentAcceptedDrawReceiver)
        unregisterReceiver(opponentDeclinedDrawReceiver)
        unregisterReceiver(chatMessageReceiver)

        keyboardHeightProvider.observer = null

        if (game is MultiPlayerGame) {
            saveGame()
        }
        super.onStop()
    }

    override fun onDestroy() {
        glView.destroy()
        keyboardHeightProvider.close()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (isChatOpened()) {
            closeChat()
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyboardHeightChanged(height: Int) {
        println("KEYBOARD HEIGHT: $height")
//        if (height > 0) {
        getChatFragment().translate(height)
//        }
    }

    private fun onPawnPromoted(square: Vector2, team: Team): PieceType {
        runOnUiThread { pieceChooserDialog.show(square, team) }
        return PieceType.QUEEN
    }

    private fun enableBackButton() {
        findViewById<UIButton>(R.id.back_button).enable()
        glView.requestRender()
    }

    private fun enableForwardButton() {
        findViewById<UIButton>(R.id.forward_button).enable()
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

    private fun initUIButtons() {
        val textOffset = 70
        val textColor = Color.WHITE
//        val buttonBackgroundColor = Color.rgb(235, 186, 145)
        val buttonBackgroundColor = Color.DKGRAY
        val resignButton = findViewById<UIButton>(R.id.resign_button)
        resignButton
            .setTextYOffset(textOffset)
            .setText("Resign")
            .setColoredDrawable(R.drawable.resign)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)

            .setOnClickListener {
                if (isChatOpened()) {
                    return@setOnClickListener
                }

                resignDialog.show(gameId, id) {
                    NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "resign", "$gameId|$id"))
                    SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
                    finish()
                }
            }

        val offerDrawButton = findViewById<UIButton>(R.id.offer_draw_button)
        offerDrawButton
            .setText("Offer Draw")
            .setColor(buttonBackgroundColor)
            .setColoredDrawable(R.drawable.handshake_13359)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setTextYOffset(textOffset)
            .setOnClickListener {
                if (isChatOpened()) {
                    return@setOnClickListener
                }

                offerDrawDialog.show(gameId, id)
            }

        val redoButton = findViewById<UIButton>(R.id.request_redo_button)
        redoButton
            .setText("Undo")
            .setColoredDrawable(R.drawable.rewind)
            .setButtonTextSize(50f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setTextYOffset(textOffset)
            .setOnClickListener {
                if (isChatOpened()) {
                    return@setOnClickListener
                }

                NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "request_undo", "$gameId|$id"))
            }

        findViewById<UIButton>(R.id.back_button)
            .setText("Back")
            .setColoredDrawable(R.drawable.back_arrow)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setTextYOffset(textOffset)
            .disable()
            .setOnClickListener {
                if ((it as UIButton).disabled || isChatOpened()) {
                    return@setOnClickListener
                }

                val buttonStates = game.showPreviousMove()
                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
//                    board.clearPossibleMoves()
//                    board.deselectSquare()
                    game.clearBoardData()
                    findViewById<UIButton>(R.id.forward_button)?.enable()
                }
                glView.requestRender()
            }

        findViewById<UIButton>(R.id.forward_button)
            .setText("Forward")
            .setColoredDrawable(R.drawable.forward_arrow)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setTextYOffset(textOffset)
            .disable()
            .setOnClickListener {
                if ((it as UIButton).disabled || isChatOpened()) {
                    return@setOnClickListener
                }

                val buttonStates = game.showNextMove()
                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
                    findViewById<UIButton>(R.id.back_button)?.enable()
                }
                glView.requestRender()
            }

    }

    private fun initChatBox() {
        findViewById<ImageView>(R.id.open_chat_button).setOnClickListener {
            val chatBoxEndX = if (chatOpened) -chatTranslation else 0
            val chatButtonEndX = if (chatOpened) 0 else chatTranslation

            val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", chatBoxEndX.toFloat())
            val chatButtonAnimator = ObjectAnimator.ofFloat(it, "x", chatButtonEndX.toFloat())

            chatBoxAnimator.duration = 500L
            chatButtonAnimator.duration = 500L

            chatBoxAnimator.start()
            chatButtonAnimator.start()

            chatOpened = !chatOpened
        }
    }
}