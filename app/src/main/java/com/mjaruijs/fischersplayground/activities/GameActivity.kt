package com.mjaruijs.fischersplayground.activities

import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.news.News
import com.mjaruijs.fischersplayground.chess.SavedGames
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.dialogs.*
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.news.NewsType
import com.mjaruijs.fischersplayground.opengl.SurfaceView
import com.mjaruijs.fischersplayground.userinterface.UIButton

class GameActivity : AppCompatActivity() {

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

    private val incomingInviteDialog = IncomingInviteDialog()
    private val undoRequestedDialog = UndoRequestedDialog()
    private val undoRejectedDialog = UndoRejectedDialog()
    private val resignDialog = ResignDialog()
    private val offerDrawDialog = OfferDrawDialog()
    private val opponentResignedDialog = OpponentResignedDialog()
    private val opponentOfferedDrawDialog = OpponentOfferedDrawDialog()
    private val opponentAcceptedDrawDialog = OpponentAcceptedDrawDialog()
    private val opponentDeclinedDrawDialog = OpponentDeclinedDrawDialog()

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")

    private var displayWidth = 0
    private var displayHeight = 0

    private var isSinglePlayer = false
    private var isPlayingWhite = false

    private lateinit var id: String
    private lateinit var gameId: String
    private lateinit var opponentName: String

    private lateinit var board: Board
    private lateinit var game: Game

    private lateinit var glView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActivityDecorations()

        incomingInviteDialog.create(this)
        undoRequestedDialog.create(this)
        undoRejectedDialog.create(this)
        resignDialog.create(this)
        offerDrawDialog.create(this)
        opponentResignedDialog.create(this)
        opponentOfferedDrawDialog.create(this)
        opponentAcceptedDrawDialog.create(this)
        opponentDeclinedDrawDialog.create(this)

        if (!intent.hasExtra("is_playing_white")) {
            throw IllegalArgumentException("Missing essential information: is_player_white")
        }

        id = intent.getStringExtra("id") ?: throw IllegalArgumentException("Missing essential information: id")
        opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information: opponent_name")
        gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")
        isSinglePlayer = intent.getBooleanExtra("is_single_player", false)
        isPlayingWhite = intent.getBooleanExtra("is_playing_white", false)

        setContentView(R.layout.activity_game)
        glView = findViewById(R.id.opengl_view)
        glView.init(::onContextCreated, ::onClick, ::onDisplaySizeChanged)

        initUIButtons()
    }

    private fun onContextCreated() {
        if (isSinglePlayer) {
            game = SinglePlayerGame()
        } else {
            game = SavedGames.get(gameId) ?: MultiPlayerGame(gameId, id, opponentName, isPlayingWhite)
            (game as MultiPlayerGame).init()
        }

        game.enableBackButton = ::enableBackButton
        game.enableForwardButton = ::enableForwardButton

        board = Board { square ->
            val possibleMoves = game.determinePossibleMoves(square, game.getCurrentTeam())
            board.updatePossibleMoves(possibleMoves)
        }

        glView.setGameState(game)
        glView.setBoard(board)

        if (game is MultiPlayerGame) {
            runOnUiThread {
                processNews((game as MultiPlayerGame).news)
                (game as MultiPlayerGame).news = News(NewsType.NO_NEWS)
            }
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

    private fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height
    }

    private fun onClick(x: Float, y: Float) {
        val clickAction = board.onClick(x, y, displayWidth, displayHeight)
        val boardAction = game.processAction(clickAction)

        board.processAction(boardAction)
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
            (game as MultiPlayerGame).moveOpponent(move, true)
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

    private fun closeAndSaveGameAsWin() {
        SavedGames.get(gameId)?.status = GameStatus.GAME_WON
        finish()
    }

    private fun acceptDraw() {
        NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "accepted_draw", "$gameId|$id"))
        closeAndSaveGameAsDraw()
    }

    private fun closeAndSaveGameAsDraw() {
        SavedGames.get(gameId)?.status = GameStatus.GAME_DRAW
        finish()
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

    private fun saveGame() {
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

        if (game is MultiPlayerGame) {
            saveGame()
        }
        super.onStop()
    }

    override fun onDestroy() {
        glView.destroy()
        super.onDestroy()
    }

    private fun enableBackButton() {
        findViewById<UIButton>(R.id.back_button).enable()
        glView.requestRender()
    }

    private fun enableForwardButton() {
        findViewById<UIButton>(R.id.forward_button).enable()
    }

    private fun initUIButtons() {
        val textOffset = 70

        val resignButton = findViewById<UIButton>(R.id.resign_button)
        resignButton
            .setTextYOffset(textOffset)
            .setText("Resign")
            .setDrawable(R.drawable.resign)
            .setButtonTextSize(50f)
            .setButtonTextColor(Color.WHITE)

            .setOnClickListener {
                resignDialog.show(gameId, id) {
                    NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "resign", "$gameId|$id"))
                    SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
                    finish()
                }
            }

        val offerDrawButton = findViewById<UIButton>(R.id.offer_draw_button)
        offerDrawButton
            .setText("Offer Draw")
//            .setDrawable("@drawable/handshake_13359")
            .setDrawable(R.drawable.handshake_13359)
            .setButtonTextSize(50f)
            .setButtonTextColor(Color.WHITE)
            .setTextYOffset(textOffset)
            .setOnClickListener {
                offerDrawDialog.show(gameId, id)
            }

        val redoButton = findViewById<UIButton>(R.id.request_redo_button)
        redoButton
            .setText("Undo")
            .setDrawable(R.drawable.rewind)
            .setButtonTextSize(50f)
            .setButtonTextColor(Color.WHITE)
            .setTextYOffset(textOffset)
            .setOnClickListener {
                NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "request_undo", "$gameId|$id"))
            }

        findViewById<UIButton>(R.id.back_button)
            .setText("Back")
            .setDrawable(R.drawable.back_arrow)
            .setButtonTextSize(50f)
            .setButtonTextColor(Color.WHITE)
            .setDrawablePadding(0)
            .setTextYOffset(textOffset)
            .disable()
            .setOnClickListener {
                if ((it as UIButton).disabled) {
                    return@setOnClickListener
                }

                val buttonStates = game.showPreviousMove()
                if (buttonStates.first) {
                    it.disable()
                }
                if (buttonStates.second) {
                    board.clearPossibleMoves()
                    board.deselectSquare()
                    findViewById<UIButton>(R.id.forward_button)?.enable()
                }
                glView.requestRender()
            }

        findViewById<UIButton>(R.id.forward_button)
            .setText("Forward")
            .setDrawable(R.drawable.forward_arrow)
            .setButtonTextSize(50f)
            .setButtonTextColor(Color.WHITE)
            .setTextYOffset(textOffset)
            .disable()
            .setOnClickListener {
                if ((it as UIButton).disabled) {
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
}