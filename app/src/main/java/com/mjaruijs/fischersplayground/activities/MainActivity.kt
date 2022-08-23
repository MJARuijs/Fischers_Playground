package com.mjaruijs.fischersplayground.activities

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Messenger
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.iid.FirebaseInstanceId
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.activities.game.PractiseGameActivity
import com.mjaruijs.fischersplayground.adapters.gameadapter.*
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.dialogs.CreateGameDialog
import com.mjaruijs.fischersplayground.dialogs.CreateUsernameDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_DELETE_GAME
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_GET_ALL_DATA
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_SET_ID
import com.mjaruijs.fischersplayground.services.DataManagerService.Companion.FLAG_STORE_SENT_INVITE
import com.mjaruijs.fischersplayground.services.FirebaseService
import com.mjaruijs.fischersplayground.userinterface.UIButton
import java.util.*
import kotlin.collections.HashMap

class MainActivity : ClientActivity() {

    override var activityName = "main_activity"

    override var clientMessenger = Messenger(IncomingHandler(this))

    private var id: String? = null
    private var userName: String? = null

    private val idReceiver = MessageReceiver(Topic.INFO, "id", ::onIdReceived)
    private val playersReceiver = MessageReceiver(Topic.INFO, "search_players_result", ::onPlayersReceived)

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")

    private val createUsernameDialog = CreateUsernameDialog()
    private val createGameDialog = CreateGameDialog(::onInvite)

    private lateinit var gameAdapter: GameAdapter

    private var stayingInApp = false

    private var maxTextSize = Float.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideActivityDecorations()

        registerReceivers()

        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)
        id = preferences.getString("ID", "")
        userName = preferences.getString("USER_NAME", "")

        Intent(this, FirebaseService::class.java).also {
            startService(it)
        }

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val token = result.token
            println("GOT NEW TOKEN: $token")
            getSharedPreferences("test", MODE_PRIVATE).edit().putString("token", token).apply()
        }

        FirebaseService.getToken(this)
//        startDataService()
//        startService(Intent(this, DataManagerService::class.java))

//        Thread {
//            while (!serviceBound) {
//                Thread.sleep(10)
//            }
//            sendMessage(FLAG_SET_ID, id)
//        }.start()

        if (!isInitialized()) {
            preloadModels()

            NetworkManager.run(this)

            if (id != null && id!!.isNotBlank()) {
                NetworkManager.sendMessage(NetworkMessage(Topic.INFO, "id", "$id"))
            }
        }

        createUsernameDialog.create(this)
        createUsernameDialog.setLayout()

        if (userName == null || userName!!.isBlank()) {
            createUsernameDialog.show(::setUserName)
        } else {
            findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        }

        initUIComponents()
    }

    private fun startDataService() {
        Intent(this, DataManagerService::class.java).also {
            it.action = "wtf?"
            startForegroundService(it)
            return
        }
    }

    private fun isInitialized() = NetworkManager.isRunning()

    private fun isUserRegisteredAtServer(): Boolean {
        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)
        return preferences.contains("ID")
    }

    private fun onInvite(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_PENDING, hasUpdate = false)
        sendMessage(FLAG_STORE_SENT_INVITE, Triple(inviteId, opponentId, InviteData(opponentName, timeStamp, InviteType.PENDING)))
    }

    private fun onGameClicked(gameCard: GameCardItem) {
        stayingInApp = true
        gameAdapter.clearUpdate(gameCard, id!!)

        if (gameCard.gameStatus == GameStatus.INVITE_RECEIVED) {
            incomingInviteDialog.showInvite(gameCard.opponentName, gameCard.id)
        } else if (gameCard.gameStatus != GameStatus.INVITE_PENDING) {
            val intent = Intent(this, MultiplayerGameActivity::class.java)
                .putExtra("id", id)
                .putExtra("user_name", userName)
                .putExtra("is_playing_white", gameCard.isPlayingWhite)
                .putExtra("game_id", gameCard.id)
                .putExtra("opponent_name", gameCard.opponentName)
            startActivity(intent)
        }
    }

    private fun onGameDeleted(gameId: String) {
        sendMessage(FLAG_DELETE_GAME, gameId)
    }

    override fun onOpponentMoved(data: MoveData?) {
        if (data == null) {
            return
        }

        gameAdapter.updateCardStatus(data.gameId, data.status, data.timeStamp)
    }

    override fun onUndoRequested(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    override fun onUndoRequestAccepted(data: Pair<String, Int>?) {
        if (data == null) {
            return
        }
        gameAdapter.hasUpdate(data.first)
    }

    override fun onUndoRequestRejected(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    private fun setUserName(userName: String) {
        this.userName = userName

        savePreference("USER_NAME", userName)

        findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        NetworkManager.sendMessage(NetworkMessage(Topic.INFO, "user_name", userName))
    }

    private fun onPlayersReceived(content: String) {
        val playersData = content.split(')')

        createGameDialog.clearPlayers()
        for (playerData in playersData) {
            if (playerData.isBlank()) {
                continue
            }

            val data = playerData.removePrefix("(").removeSuffix(")").split('|')
            val name = data[0]
            val id = data[1]
            createGameDialog.addPlayers(name, id)
        }
    }

    private fun onIdReceived(id: String) {
        this.id = id

        savePreference("ID", id)

        createGameDialog.updateId(id)
    }

    override fun onOpponentResigned(gameId: String) {
        gameAdapter.updateCardStatus(gameId, GameStatus.GAME_WON)
    }

    override fun onOpponentOfferedDraw(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    override fun onOpponentAcceptedDraw(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    override fun onOpponentRejectedDraw(gameId: String) {
        gameAdapter.hasUpdate(gameId)
    }

    private fun registerReceivers() {
        registerReceiver(idReceiver, infoFilter)
        registerReceiver(playersReceiver, infoFilter)
    }

    override fun onResume() {
        super.onResume()

        Thread {
            while (!serviceBound) {
                Thread.sleep(10)
            }
            sendMessage(FLAG_GET_ALL_DATA)
        }.start()

        stayingInApp = false

        if (isUserRegisteredAtServer()) {
            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|online"))
        }

        registerReceivers()
    }

    override fun onStop() {
        unregisterReceiver(idReceiver)
        unregisterReceiver(playersReceiver)

        if (!stayingInApp) {
            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|away"))
        }
        super.onStop()
    }

    override fun onUserLeaveHint() {
        if (!stayingInApp) {
            NetworkManager.sendMessage(NetworkMessage(Topic.USER_STATUS, "status", "$id|away"))
        }

        super.onUserLeaveHint()
    }

    private fun savePreference(key: String, value: String) {
        val preferences = getSharedPreferences("user_data", MODE_PRIVATE)

        with(preferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

    private fun preloadModels() {
        PieceTextures.init(resources)

        Thread {
            OBJLoader.preload(resources, R.raw.pawn_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.bishop_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.knight_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.rook_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.queen_bytes)
        }.start()

        Thread {
            OBJLoader.preload(resources, R.raw.king_bytes)
        }.start()
    }

    private fun onButtonInitialized(textSize: Float) {
        if (textSize < maxTextSize) {
            maxTextSize = textSize
            findViewById<UIButton>(R.id.start_new_game_button).setButtonTextSize(maxTextSize)
            findViewById<UIButton>(R.id.single_player_button).setButtonTextSize(maxTextSize)
        }
    }

    private fun initUIComponents() {
        gameAdapter = GameAdapter(::onGameClicked, ::onGameDeleted)

        val gameRecyclerView = findViewById<RecyclerView>(R.id.game_list)
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter

        incomingInviteDialog.create(this)
        createGameDialog.create(id!!, this)

        findViewById<UIButton>(R.id.settings_button)
            .setColoredDrawable(R.drawable.settings_solid_icon)
            .setColor(Color.TRANSPARENT)
            .setChangeIconColorOnHover(false)
            .setChangeTextColorOnHover(true)
            .setOnClickListener {
                stayingInApp = true
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

        findViewById<UIButton>(R.id.single_player_button)
            .setText("Practice Mode")
            .setButtonTextSize(70.0f)
            .setColor(235, 186, 145)
            .setCornerRadius(45.0f)
            .setChangeTextColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                stayingInApp = true
                val intent = Intent(this, PractiseGameActivity::class.java)
                    .putExtra("id", id)
                    .putExtra("user_name", userName)
                    .putExtra("is_single_player", true)
                    .putExtra("is_playing_white", true)
                    .putExtra("game_id", "test_game")
                    .putExtra("opponent_name", "Opponent")
                startActivity(intent)
            }

        findViewById<UIButton>(R.id.start_new_game_button)
            .setText("Start new game")
            .setButtonTextSize(70.0f)
            .setColor(235, 186, 145)
            .setCornerRadius(45.0f)
            .setChangeTextColorOnHover(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                createGameDialog.show()
            }
    }

    override fun newGameStarted(gameCard: GameCardItem) {
        val doesCardExist = gameAdapter.updateGameCard(gameCard.id, gameCard.gameStatus, gameCard.isPlayingWhite, gameCard.hasUpdate)
        if (!doesCardExist) {
            gameAdapter += gameCard
        }
    }

    override fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {
        for ((gameId, game) in games ?: return) {
            val doesCardExist = gameAdapter.updateGameCard(gameId, game.status, game.isPlayingWhite, false)

            if (!doesCardExist) {
                gameAdapter += GameCardItem(gameId, game.lastUpdated, game.opponentName, game.status, game.isPlayingWhite, false)
            }
        }
    }

    private fun restoreSavedInvites(invites: HashMap<String, InviteData>?) {
        for ((inviteId, inviteData) in invites ?: return) {
            if (gameAdapter.containsCard(inviteId)) {
                continue
            }

            val status = when (inviteData.type) {
                InviteType.PENDING -> GameStatus.INVITE_PENDING
                InviteType.RECEIVED -> GameStatus.INVITE_RECEIVED
            }

            val hasUpdate = when (inviteData.type) {
                InviteType.PENDING -> false
                InviteType.RECEIVED -> true
            }

            gameAdapter += GameCardItem(inviteId, inviteData.timeStamp, inviteData.opponentName, status, hasUpdate = hasUpdate)
        }
    }

    override fun onInviteReceived(inviteData: Pair<String, InviteData>?) {
        super.onInviteReceived(inviteData)

        gameAdapter += GameCardItem(inviteData?.first ?: return, inviteData.second.timeStamp, inviteData.second.opponentName, GameStatus.INVITE_RECEIVED, hasUpdate = true)
    }

    override fun restoreSavedData(data: Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>?) {
        restoreSavedGames(data?.first ?: return)
        restoreSavedInvites(data.second)
        updateRecentOpponents(data.third)
    }

    override fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {
        createGameDialog.setRecentOpponents(opponents ?: return)
    }
}