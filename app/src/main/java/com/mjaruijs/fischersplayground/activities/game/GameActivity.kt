package com.mjaruijs.fischersplayground.activities.game

import android.app.Activity
import android.content.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.MainActivity.Companion.MULTIPLAYER_GAME_FILE
import com.mjaruijs.fischersplayground.activities.MessageReceiver
import com.mjaruijs.fischersplayground.activities.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.*
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.opengl.surfaceviews.SurfaceView
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.PlayerStatus
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.REGISTER_CLIENT_VALUE
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.util.FileManager

abstract class GameActivity : AppCompatActivity(R.layout.activity_game) {

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
    private val userStatusReceiver = MessageReceiver(Topic.USER_STATUS, "status", ::onUserStatusReceived)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")
    private val chatFilter = IntentFilter("mjaruijs.fischers_playground.CHAT_MESSAGE")
    private val statusFilter = IntentFilter("mjaruijs.fischers_playground.USER_STATUS")

    private val incomingInviteDialog = IncomingInviteDialog()
    val undoRequestedDialog = UndoRequestedDialog()
    val undoRejectedDialog = UndoRejectedDialog()
    val resignDialog = ResignDialog()
    val offerDrawDialog = OfferDrawDialog()
    val opponentResignedDialog = OpponentResignedDialog()
    val opponentOfferedDrawDialog = OpponentOfferedDrawDialog()
    val opponentAcceptedDrawDialog = OpponentAcceptedDrawDialog()
    val opponentDeclinedDrawDialog = OpponentDeclinedDrawDialog()

    private val checkMateDialog = CheckMateDialog()
    private val pieceChooserDialog = PieceChooserDialog(::onPawnUpgraded)

    private var displayWidth = 0
    private var displayHeight = 0

//    private var chatInitialized = false
//    private var chatOpened = false
//    private var chatTranslation = 0

    private var isSinglePlayer = true
    var isPlayingWhite = true

    lateinit var gameId: String
    lateinit var id: String
    lateinit var userName: String
    lateinit var opponentName: String

    open lateinit var game: Game

    lateinit var glView: SurfaceView


//    private var maxTextSize = Float.MAX_VALUE

    private var stayingInApp = false

    lateinit var dataService: DataManagerService
    private var dataServiceMessenger: Messenger? = null
    var serviceBound = false

    internal class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            println("Received in Activity: ${msg.obj}")
//            msg.replyTo.send(Message.obtain(null, 0, "Hello!"))
//            when (msg.what) {
//                0 -> {
//                    println("HELLO")
////                    msg.
//                }
//                else -> super.handleMessage(msg)
//            }
        }


    }

    private var messenger = Messenger(IncomingHandler())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder) {
//            val binder = service as DataManagerService.LocalBinder
            dataServiceMessenger = Messenger(service)
            serviceBound = true

            val message = Message.obtain(null, REGISTER_CLIENT_VALUE)
            message.replyTo = messenger
            dataServiceMessenger!!.send(message)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            dataServiceMessenger = null
            serviceBound = false
        }
    }

//    private val savedGames = HashMap<String, MultiPlayerGame>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        println("GAME_ACTIVITY: onCreate")

        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val fullScreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

        hideActivityDecorations(fullScreen)
        registerReceivers()

        incomingInviteDialog.create(this)
        undoRequestedDialog.create(this)
        undoRejectedDialog.create(this)
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
//        gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")
        isSinglePlayer = intent.getBooleanExtra("is_single_player", true)
        isPlayingWhite = intent.getBooleanExtra("is_playing_white", true)

//        loadSavedGames()

//        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|$gameId"))

        glView = findViewById(R.id.opengl_view)
        glView.init(::onContextCreated, ::onClick, ::onDisplaySizeChanged, isPlayingWhite)

        initUIButtons()
//        initChatBox()

        if (savedInstanceState == null) {
            val playerBundle = Bundle()
            playerBundle.putString("player_name", userName)
            playerBundle.putString("team", if (isPlayingWhite) "WHITE" else "BLACK")
            playerBundle.putBoolean("hide_status_icon", true)

            val opponentBundle = Bundle()
            opponentBundle.putString("player_name", opponentName)
            opponentBundle.putString("team", if (isPlayingWhite) "BLACK" else "WHITE")
            opponentBundle.putBoolean("hide_status_icon", isSinglePlayer)

            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.player_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
                replace(R.id.opponent_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
            }
        }
    }

    open fun onContextCreated() {
//        println("GAME_ACTIVITY: context created")
//        if (isSinglePlayer) {
//            game = SinglePlayerGame()
//        } else {
//            game = savedGames[gameId] ?: MultiPlayerGame(gameId, id, opponentName, isPlayingWhite)

//            runOnUiThread {
//                getChatFragment().addMessages((game as MultiPlayerGame).chatMessages)
//            }
//        }

        game.onPawnPromoted = ::onPawnPromoted
        game.enableBackButton = ::enableBackButton
        game.enableForwardButton = ::enableForwardButton
        game.onPieceTaken = ::onPieceTaken
        game.onPieceRegained = ::onPieceRegained
        game.onCheckMate = ::onCheckMate
        game.onMoveMade = ::onMoveMade

        glView.setGame(game)

        restorePreferences()

//        if (game is MultiPlayerGame) {
//            runOnUiThread {
//                processNews((game as MultiPlayerGame))
//            }
//        }
    }

    fun onMoveMade(move: Move) {
        val message = Message.obtain(null, 0, "Move made!")
        message.replyTo = messenger

        try {
            dataServiceMessenger!!.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        val cameraRotation = preferences.getString(SettingsActivity.CAMERA_ROTATION_KEY, "") ?: ""
        val fov = preferences.getInt(SettingsActivity.FOV_KEY, 45)
        val pieceScale = preferences.getFloat(SettingsActivity.PIECE_SCALE_KEY, 1.0f)

        if (cameraRotation.isNotBlank()) {
            glView.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
        }

        glView.getRenderer().setFoV(fov)
        glView.getRenderer().setPieceScale(pieceScale)
    }

    private fun onPawnUpgraded(square: Vector2, pieceType: PieceType, team: Team) {
        game.upgradePawn(square, pieceType, team)
        Thread {
            Thread.sleep(10)
            glView.invalidate()
            glView.requestRender()
        }.start()
    }

    open fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height
    }

    open fun onClick(x: Float, y: Float) {
        game.onClick(x, y, displayWidth, displayHeight)
    }

    private fun onCheckMate(team: Team) {
        runOnUiThread {
            if ((team == Team.WHITE && isPlayingWhite) || (team == Team.BLACK && !isPlayingWhite)) {
                checkMateDialog.show(userName, ::closeAndSaveGameAsWin)
            } else {
                checkMateDialog.show(opponentName, ::closeAndSaveGameAsLoss)
            }
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

    private fun onPieceRegained(pieceType: PieceType, team: Team) {
        if ((isPlayingWhite && team == Team.WHITE) || (!isPlayingWhite && team == Team.BLACK)) {
            val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "player" } ?: throw IllegalArgumentException("No fragment for player was found..")
            (opponentFragment as PlayerCardFragment).removeTakenPiece(pieceType, team)
        } else if ((isPlayingWhite && team == Team.BLACK) || (!isPlayingWhite && team == Team.WHITE)) {
            val playerFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for opponent was found..")
            (playerFragment as PlayerCardFragment).removeTakenPiece(pieceType, team)
        }
    }

//    private fun processNews(game: MultiPlayerGame) {
//        for (news in game.newsUpdates) {
//            when (news.newsType) {
//                NewsType.OPPONENT_RESIGNED -> opponentResignedDialog.show(opponentName, ::closeAndSaveGameAsWin)
//                NewsType.OPPONENT_OFFERED_DRAW -> opponentOfferedDrawDialog.show(gameId, id, opponentName, ::acceptDraw)
//                NewsType.OPPONENT_ACCEPTED_DRAW -> opponentAcceptedDrawDialog.show(gameId, opponentName, ::closeAndSaveGameAsDraw)
//                NewsType.OPPONENT_DECLINED_DRAW -> opponentDeclinedDrawDialog.show(opponentName)
//                NewsType.OPPONENT_REQUESTED_UNDO -> undoRequestedDialog.show(gameId, opponentName, id)
//                NewsType.OPPONENT_ACCEPTED_UNDO -> {
//                    game.undoMoves(news.data)
//                    glView.requestRender()
//                }
//                NewsType.OPPONENT_REJECTED_UNDO -> undoRejectedDialog.show(opponentName)
//                NewsType.NO_NEWS -> {}
//            }
//        }
//        game.clearNews()
//    }

    private fun onNewGameStarted(content: String) {
        val data = content.split('|')

        val inviteId = data[0]
        val opponentName = data[1]
        val playingWhite = data[2].toBoolean()

//        val newGame = MultiPlayerGame(inviteId, id, opponentName, playingWhite)
//        savedGames[inviteId] = newGame
    }

    private fun onOpponentResigned(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentResignedDialog.show(opponentUsername, ::closeAndSaveGameAsWin)
        } else {
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_RESIGNED))
        }
    }

    private fun onOpponentOfferedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentOfferedDrawDialog.show(gameId, id, opponentUsername, ::acceptDraw)
        } else {
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_OFFERED_DRAW))
        }
    }

    private fun onOpponentAcceptedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentAcceptedDrawDialog.show(gameId, opponentUsername, ::closeAndSaveGameAsDraw)
        } else {
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_DRAW))
        }
    }

    private fun onOpponentDeclinedDraw(content: String) {
        val data = content.split('|')

        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            opponentDeclinedDrawDialog.show(opponentUsername)
        } else {
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_DECLINED_DRAW))
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
//            val game = savedGames[gameId] ?: throw IllegalArgumentException("Could not find game with id: $gameId")
//            game.moveOpponent(move, false)
//            savedGames[gameId] = game
        }
    }

    private fun onIncomingInvite(content: String) {
        val data = content.split('|')

        val invitingUsername = data[0]
        val inviteId = data[1]

        incomingInviteDialog.showInvite(invitingUsername, inviteId)
    }

    private fun onUndoRequested(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val opponentUsername = data[1]

        if (this.gameId == gameId) {
            undoRequestedDialog.show(gameId, opponentUsername, id)
        } else {
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_REQUESTED_UNDO))
        }
    }

    private fun onUndoAccepted(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val numberOfMovesReversed = data[1].toInt()

        if (this.gameId == gameId) {
            (game as MultiPlayerGame).undoMoves(numberOfMovesReversed)
            glView.requestRender()
        } else {
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_ACCEPTED_UNDO, numberOfMovesReversed))
//            savedGames[gameId]?.status = GameStatus.PLAYER_MOVE
        }
    }

    private fun onUndoRejected(gameId: String) {
        if (this.gameId == gameId) {
            undoRejectedDialog.show(opponentName)
        } else {
//            savedGames[gameId]?.addNews(News(NewsType.OPPONENT_REJECTED_UNDO))
        }
    }

    private fun onUserStatusReceived(content: String) {
        setOpponentStatusIcon(content)
    }

    private fun onChatMessageReceived(content: String) {
        val data = content.split('|')
        val gameId = data[0]
        val timeStamp = data[1]
        val messageContent = data[2]

        val message = ChatMessage(timeStamp, messageContent, MessageType.RECEIVED)

        if (this.gameId == gameId) {
//            getChatFragment().addReceivedMessage(message)
        } else {
//            savedGames[gameId]?.chatMessages?.add(message)
        }
    }

//    private fun onChatMessageSent(message: ChatMessage) {
//        NetworkManager.sendMessage(Message(Topic.CHAT_MESSAGE, "", "$gameId|$id|${message.timeStamp}|${message.message}"))
//        if (game is MultiPlayerGame) {
//            (game as MultiPlayerGame).chatMessages += message
//        }
//    }

//    private fun finishActivity() {
//        val resultIntent = Intent()
////        resultIntent.putExtra("saved_games", gamesToString())
//
//        if (game is MultiPlayerGame) {
//            resultIntent.putExtra("gameId", gameId)
//            resultIntent.putExtra("lastUpdated", game.lastUpdated)
//            resultIntent.putExtra("opponentName", opponentName)
//            resultIntent.putExtra("status", (game as MultiPlayerGame).status.toString())
//            resultIntent.putExtra("isPlayingWhite", isPlayingWhite)
//            resultIntent.putExtra("hasUpdate", true)
////            setResult(Activity.RESULT_OK, resultIntent)
//        } else {
////            setResult(Activity.RESULT_CANCELED, resultIntent)
//        }

//        finish()
//    }

    private fun finishActivity(status: GameStatus) {
        if (game is MultiPlayerGame) {
            (game as MultiPlayerGame).status = status
        }
        saveGames()
//        saveGame()

        val resultIntent = Intent()
        resultIntent.putExtra("gameId", gameId)
        resultIntent.putExtra("lastUpdated", game.lastUpdated)
        resultIntent.putExtra("opponentName", opponentName)
        resultIntent.putExtra("status", status)
        resultIntent.putExtra("isPlayingWhite", isPlayingWhite)
        resultIntent.putExtra("hasUpdate", true)
        setResult(Activity.RESULT_OK, resultIntent)

//        println("GAME_ACTIVITY: finish")
        finish()
    }

    fun closeAndSaveGameAsWin() {
        finishActivity(GameStatus.GAME_WON)
//        SavedGames.get(gameId)?.status = GameStatus.GAME_WON
//        finish()
    }

    fun closeAndSaveGameAsDraw() {
        finishActivity(GameStatus.GAME_DRAW)
//        SavedGames.get(gameId)?.status = GameStatus.GAME_DRAW
//        finish()
    }

    fun closeAndSaveGameAsLoss() {
        finishActivity(GameStatus.GAME_LOST)
//        SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
//        finish()
    }

    fun acceptDraw() {
        NetworkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "accepted_draw", "$gameId|$id"))
        closeAndSaveGameAsDraw()
    }

    private fun hideActivityDecorations(isFullscreen: Boolean) {
        supportActionBar?.hide()

        if (isFullscreen) {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }

    }

    private fun setOpponentStatusIcon(gameId: String) {
//        println("GETTING FRAGMENT")
        val opponentFragment = supportFragmentManager.fragments.find { fragment -> fragment.tag == "opponent" } ?: throw IllegalArgumentException("No fragment for player was found..")

//        println("TRYING TO SET STATUS: ${this.gameId} :: $gameId")

        when {
//            this.gameId == gameId -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_GAME)
            gameId == "online" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
            gameId == "away" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.AWAY)
            gameId == "offline" -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.OFFLINE)
            else -> (opponentFragment as PlayerCardFragment).setStatusIcon(PlayerStatus.IN_OTHER_GAME)
        }
    }

//    private fun saveGame() {
//        println("SAVING GAME: $gameId ${(game as MultiPlayerGame).status}")
////        SavedGames.put(gameId, game as MultiPlayerGame)
//        FileManager.write(this, "game.txt", (game as MultiPlayerGame).toString())
//    }

    private fun registerReceivers() {
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
        registerReceiver(userStatusReceiver, statusFilter)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, DataManagerService::class.java)
            .putExtra("id", id)

        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
//        println("GAME_ACTIVITY: onResume")
        super.onResume()

        stayingInApp = false
        registerReceivers()

        pieceChooserDialog.setLayout()
    }

    override fun onPause() {
//        if (game is MultiPlayerGame) {
//            saveGame()
//        }

//        println("GAME ACTIVITY: onPause")
        saveGames()

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
        unregisterReceiver(userStatusReceiver)

        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        serviceBound = false
    }

    override fun onDestroy() {
//        println("ON DESTROY GAME_ACTIVITY $isFinishing")
        glView.destroy()
//        NetworkManager.sendMessage(Message(Topic.USER_STATUS, "status", "$id|$gameId|offline"))

        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|$gameId|away"))
        super.onUserLeaveHint()
    }



//    private fun parseSavedGames(content: String) {
//        val gamesData = content.split(',')
//
//        for (gameData in gamesData) {
//            val data = gameData.split('|')
//            val gameId = data[0]
//            val lastUpdated = data[1].toLong()
//            val opponentName = data[2]
//            val gameStatus = GameStatus.fromString(data[3])
//            val isPlayingWhite = data[4].toBoolean()
//            val hasUpdate = data[5].toBoolean()
//
//
//        }
//    }

//    private fun gamesToString(): String {
//        var data = ""
//
//        for (game in savedGames) {
//            data += "${game.key}|${game.value.lastUpdated}|${game.value.opponentName}|${game.value.status}|${game.value.isPlayingWhite}|true,"
//        }
//
//        return data
//    }



    private fun onPawnPromoted(square: Vector2, team: Team): PieceType {
        runOnUiThread { pieceChooserDialog.show(square, team) }
        return PieceType.QUEEN
    }

    private fun enableBackButton() {
//        findViewById<UIButton>(R.id.back_button).enable()
        glView.requestRender()
    }

    private fun enableForwardButton() {
//        findViewById<UIButton>(R.id.forward_button).enable()
    }

//    private fun translateChat(translation: Float) {
//        val chatContainer = findViewById<FragmentContainerView>(R.id.chat_container)
//        val openChatButton = findViewById<ImageView>(R.id.open_chat_button)
//        chatContainer.x -= translation
//        openChatButton.x -= translation
//
//        if (chatContainer.x > 0.0f) {
//            chatContainer.x = 0.0f
//        }
//        if (openChatButton.x > chatTranslation.toFloat()) {
//            openChatButton.x = chatTranslation.toFloat()
//        }
//    }

//    private fun closeChat() {
//        val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", -chatTranslation.toFloat())
//        val chatButtonAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.open_chat_button), "x", 0.0f)
//
//        chatBoxAnimator.duration = 500L
//        chatButtonAnimator.duration = 500L
//
//        chatBoxAnimator.start()
//        chatButtonAnimator.start()
//
//        chatOpened = false
//    }

//    private fun loadSavedGames() {
//        val lines = FileManager.read(this, MULTIPLAYER_GAME_FILE) ?: ArrayList()
//
//        for (gameData in lines) {
//            if (gameData.isBlank()) {
//                continue
//            }
//
//            val data = gameData.removePrefix("(").removeSuffix(")").split('|')
//            val gameId = data[0]
//            val lastUpdated = data[1].toLong()
//            val opponentName = data[2]
//            val isPlayerWhite = data[3].toBoolean()
//            val gameStatus = GameStatus.fromString(data[4])
//            val moveList = data[5].removePrefix("[").removeSuffix("]").split('\\')
//            val chatMessages = data[6].removePrefix("[").removeSuffix("]").split('\\')
//            val newsData = data[7].removePrefix("[").removeSuffix("]").split("\\")
//
////            val winner = data[7]
//
//            val moves = ArrayList<Move>()
//
//            for (move in moveList) {
//                if (move.isNotBlank()) {
//                    moves += Move.fromChessNotation(move)
//                }
//            }
//
//            val messages = ArrayList<ChatMessage>()
//            for (message in chatMessages) {
//                if (message.isNotBlank()) {
//                    val messageData = message.split(',')
//                    val timeStamp = messageData[0]
//                    val messageContent = messageData[1]
//                    val type = MessageType.fromString(messageData[2])
//
//                    messages += ChatMessage(timeStamp, messageContent, type)
//                }
//            }
//
//            val newsUpdates = ArrayList<News>()
//            for (news in newsData) {
//                if (news.isBlank()) {
//                    continue
//                }
//
//                newsUpdates += News.fromString(news)
//            }
//
//            val newGame = MultiPlayerGame(gameId, id, opponentName, isPlayerWhite, moves, messages, newsUpdates)
//            newGame.status = gameStatus
//
//            savedGames[gameId] = newGame
//        }
//    }

    private fun saveGames() {
//        var content = ""
//
//        for ((gameId, game) in savedGames) {
//            var moveData = "["
//
//            for ((i, move) in game.moves.withIndex()) {
//                moveData += move.toChessNotation()
//                if (i != game.moves.size - 1) {
//                    moveData += "\\"
//                }
//            }
//            moveData += "]"
//
//            var chatData = "["
//
//            for ((i, message) in game.chatMessages.withIndex()) {
//                chatData += message
//                if (i != game.chatMessages.size - 1) {
//                    chatData += "\\"
//                }
//            }
//            chatData += "]"
//
//            var newsContent = "["
//
//            for ((i, news) in game.newsUpdates.withIndex()) {
//                newsContent += news.toString()
//                if (i != game.newsUpdates.size - 1) {
//                    newsContent += "\\"
//                }
//            }
//            newsContent += "]"
//
//            content += "$gameId|${game.lastUpdated}|${game.opponentName}|${game.isPlayingWhite}|${game.status}|$moveData|$chatData|$newsContent\n"
//        }
//
//        FileManager.write(this, MULTIPLAYER_GAME_FILE, content)
    }

//    private fun onButtonInitialized(textSize: Float) {
//        if (textSize < maxTextSize) {
//            maxTextSize = textSize
//            findViewById<UIButton>(R.id.resign_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.offer_draw_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.request_redo_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.back_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.forward_button).setButtonTextSize(maxTextSize)
//        }
//    }

    private fun initUIButtons() {
//        val textOffset = 65
//        val textColor = Color.WHITE
//        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)
//        val resignButton = findViewById<UIButton>(R.id.resign_button)
//        resignButton
//            .setTextYOffset(textOffset)
//            .setText("Resign")
//            .setColoredDrawable(R.drawable.resign)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if (isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                resignDialog.show(gameId, id) {
//                    NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "resign", "$gameId|$id"))
////                    SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
//
//                    finishActivity(GameStatus.GAME_LOST)
//                }
//            }
//
//        val offerDrawButton = findViewById<UIButton>(R.id.offer_draw_button)
//        offerDrawButton
//            .setText("Offer Draw")
//            .setColor(buttonBackgroundColor)
//            .setColoredDrawable(R.drawable.handshake_2)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if (isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                offerDrawDialog.show(gameId, id)
//            }
//
//        val redoButton = findViewById<UIButton>(R.id.request_redo_button)
//        redoButton
//            .setText("Undo")
//            .setColoredDrawable(R.drawable.rewind)
//            .setButtonTextSize(50f)
//            .setColor(buttonBackgroundColor)
//            .setButtonTextColor(textColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if (isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "request_undo", "$gameId|$id"))
//            }
//
//        val backButton = findViewById<UIButton>(R.id.back_button)
//        backButton
//            .setText("Back")
//            .setColoredDrawable(R.drawable.arrow_back)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .disable()
//            .setOnClickListener {
//                if ((it as UIButton).disabled || isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                val buttonStates = game.showPreviousMove()
//                if (buttonStates.first) {
//                    it.disable()
//                }
//                if (buttonStates.second) {
//                    game.clearBoardData()
//                    findViewById<UIButton>(R.id.forward_button)?.enable()
//                }
//                glView.requestRender()
//            }
//
//        val forwardButton = findViewById<UIButton>(R.id.forward_button)
//        forwardButton
//            .setText("Forward")
//            .setColoredDrawable(R.drawable.arrow_forward)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .disable()
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if ((it as UIButton).disabled || isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                val buttonStates = game.showNextMove()
//                if (buttonStates.first) {
//                    it.disable()
//                }
//                if (buttonStates.second) {
//                    findViewById<UIButton>(R.id.back_button)?.enable()
//                }
//                glView.requestRender()
//            }
    }

//    private fun initChatBox() {
//        findViewById<ImageView>(R.id.open_chat_button).setOnClickListener {
//            val chatBoxEndX = if (chatOpened) -chatTranslation else 0
//            val chatButtonEndX = if (chatOpened) 0 else chatTranslation
//
//            val chatBoxAnimator = ObjectAnimator.ofFloat(findViewById<FragmentContainerView>(R.id.chat_container), "x", chatBoxEndX.toFloat())
//            val chatButtonAnimator = ObjectAnimator.ofFloat(it, "x", chatButtonEndX.toFloat())
//
//            chatBoxAnimator.duration = 500L
//            chatButtonAnimator.duration = 500L
//
//            chatBoxAnimator.start()
//            chatButtonAnimator.start()
//
//            chatOpened = !chatOpened
//        }
//    }

}