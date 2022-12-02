package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.MultiplayerGameActivity
import com.mjaruijs.fischersplayground.activities.game.SinglePlayerGameActivity
import com.mjaruijs.fischersplayground.activities.opening.OpeningMenuActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.*
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.MoveData
import com.mjaruijs.fischersplayground.dialogs.CreateGameDialog
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.services.LoadResourcesWorker
import com.mjaruijs.fischersplayground.userinterface.RippleEffect
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import java.util.*

class MainActivity : ClientActivity() {

    override var activityName = "main_activity"

    override val stayInAppOnBackPress = false

    private val createGameDialog = CreateGameDialog(::onInvite)

    private lateinit var gameAdapter: GameAdapter

    private var hasNewToken = false
    private var showFinishedGames = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.welcome_text_view).append(", $userName")
//        loadResources()

        initUIComponents()
//        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
//            val token = result.token
//            val currentToken = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!
//
//            if (token != currentToken) {
//                getPreference(FIRE_BASE_PREFERENCE_FILE).edit().putString("token", token).apply()
//
//                if (userName == DEFAULT_USER_NAME) {
//                    hasNewToken = true
//                } else {
//                    networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$userId|$token"))
//                }
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        hideActivityDecorations()
        manageGameVisibility()

        Thread {
            while (dataManager.isLocked()) {
                Thread.sleep(1)
            }
            runOnUiThread {
                restoreSavedGames(dataManager.getSavedGames())
                restoreSavedInvites(dataManager.getSavedInvites())
                updateRecentOpponents(dataManager.getRecentOpponents())
            }
        }.start()

        stayingInApp = false
    }

    override fun onDestroy() {
        createGameDialog.dismiss()
        super.onDestroy()
    }

    override fun onMessageReceived(topic: Topic, content: Array<String>, messageId: Long) {
        when (topic) {
            Topic.SEARCH_PLAYERS -> onPlayersReceived(content)
            else -> super.onMessageReceived(topic, content, messageId)
        }
    }

    private fun onPlayersReceived(content: Array<String>) {
        createGameDialog.clearPlayers()
        for (playerData in content) {
            if (playerData.isBlank()) {
                continue
            }

            val data = playerData.removePrefix("(").removeSuffix(")").split(',')
            val name = data[0]
            val id = data[1]
            createGameDialog.addPlayers(name, id)
        }
    }

    private fun onInvite(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_PENDING, hasUpdate = false)
        dataManager.saveInvite(inviteId, InviteData(inviteId, opponentName, timeStamp, InviteType.PENDING))
        dataManager.saveData(applicationContext)
        dataManager.updateRecentOpponents(applicationContext, Pair(opponentName, opponentId))
    }

    private fun onGameClicked(gameCard: GameCardItem) {
        gameAdapter.clearUpdate(gameCard)

        if (gameCard.gameStatus == GameStatus.INVITE_RECEIVED) {
            showNewInviteDialog(gameCard.id, gameCard.opponentName)
        } else if (gameCard.gameStatus != GameStatus.INVITE_PENDING) {
            stayingInApp = true
            launchMultiplayerActivity(gameCard)
        }
    }

    private fun launchMultiplayerActivity(gameCard: GameCardItem) {
        val intent = Intent(this, MultiplayerGameActivity::class.java)
            .putExtra("id", userId)
            .putExtra("user_name", userName)
            .putExtra("is_playing_white", gameCard.isPlayingWhite)
            .putExtra("game_id", gameCard.id)
            .putExtra("opponent_name", gameCard.opponentName)
        startActivity(intent)
    }

    private fun showNewInviteDialog(inviteId: String, opponentName: String) {
//        incomingInviteDialog.setMessage("$opponentName is inviting you for a game!")
//        incomingInviteDialog.setRightOnClick {
//            networkManager.sendMessage(NetworkMessage(Topic.INVITE_ACCEPTED, inviteId))
//        }
//        incomingInviteDialog.setLeftOnClick {
//            networkManager.sendMessage(NetworkMessage(Topic.INVITE_REJECTED, inviteId))
//        }
//        incomingInviteDialog.show()
    }

    override fun onNewGameStarted(output: Parcelable) {
        val gameCard = output as GameCardItem
        val doesCardExist = gameAdapter.updateGameCard(gameCard.id, gameCard.gameStatus, gameCard.lastUpdated, gameCard.isPlayingWhite, gameCard.hasUpdate)
        if (!doesCardExist) {
            gameAdapter += gameCard
        }
    }

    private fun onGameDeleted(gameId: String) {
        dataManager.removeGame(gameId)
        dataManager.removeSavedInvite(gameId)
        dataManager.saveData(applicationContext)
    }

    override fun onIncomingInvite(output: Parcelable) {
        val inviteData = output as InviteData

        gameAdapter += GameCardItem(inviteData.inviteId, inviteData.timeStamp, inviteData.opponentName, GameStatus.INVITE_RECEIVED, hasUpdate = true)

        showNewInviteDialog(inviteData.inviteId, inviteData.opponentName)
    }

    override fun onOpponentMoved(output: Parcelable) {
        val moveData = output as MoveData
        gameAdapter.updateCardStatus(moveData.gameId, moveData.status, moveData.time)
    }

    override fun onUndoRequested(output: Parcelable) {
        gameAdapter.hasUpdate((output as ParcelableString).value)
    }

    override fun onUndoAccepted(output: Parcelable) {
        val pair = output as ParcelablePair<*, *>
        if (pair.first is ParcelableString) {
            val gameId = (pair.first as ParcelableString).value
            gameAdapter.updateCardStatus(gameId, GameStatus.PLAYER_MOVE)
            gameAdapter.hasUpdate(gameId)
        }
    }

    override fun onUndoRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        gameAdapter.hasUpdate(gameId)
    }

    override fun onDrawOffered(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        gameAdapter.hasUpdate(gameId)
    }

    override fun onDrawAccepted(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        gameAdapter.hasUpdate(gameId)
    }

    override fun onDrawRejected(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        gameAdapter.hasUpdate(gameId)
    }

    override fun onOpponentResigned(output: Parcelable) {
        val gameId = (output as ParcelableString).value
        gameAdapter.hasUpdate(gameId)
    }

    override fun onChatMessageReceived(output: Parcelable) {
        val messageData = output as ChatMessage.Data
        val gameId = messageData.gameId
        gameAdapter.hasUpdate(gameId)
    }

    private fun manageGameVisibility() {
        val preferences = getSharedPreferences(SettingsActivity.GAME_PREFERENCES_KEY, MODE_PRIVATE)
        showFinishedGames = preferences.getBoolean(SettingsActivity.SHOW_FINISHED_GAMES_KEY, false)

        if (!showFinishedGames) {
            gameAdapter.removeFinishedGames()
        }
    }

    private fun hideActivityDecorations() {
        val preferences = getSharedPreferences(SettingsActivity.GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)
        val isFullscreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

        supportActionBar?.hide()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isFullscreen) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun initUIComponents() {
        gameAdapter = GameAdapter(::onGameClicked, ::onGameDeleted)

        val gameRecyclerView = findViewById<RecyclerView>(R.id.game_list)
        gameRecyclerView.layoutManager = LinearLayoutManager(this)
        gameRecyclerView.adapter = gameAdapter

        createGameDialog.create(userId, this, networkManager)

        findViewById<TextView>(R.id.welcome_text_view)
            .setOnClickListener { textView ->
//                createUsernameDialog.show {
//                    (textView as TextView).text = "Welcome, $it"
//                    getPreference(USER_PREFERENCE_FILE).edit().putString(USER_NAME_KEY, it).apply()
//                    networkManager.sendMessage(NetworkMessage(Topic.CHANGE_USER_NAME, "$userId|$it"))
//                }
            }

        findViewById<UIButton2>(R.id.settings_button)
            .setIconScale(0.65f)
            .setIcon(R.drawable.settings_solid_icon)
            .setRippleEffect(RippleEffect.OVAL)
            .setOnClickListener {
                stayingInApp = true
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

//        val settingsButton = findViewById<ImageView>(R.id.settings_button)
//        settingsButton.setBackgroundResource(R.drawable.settings_solid_icon)
//        settingsButton.setOnClickListener {
//            stayingInApp = true
//            val intent = Intent(this, SettingsActivity::class.java)
//            startActivity(intent)
//        }


        findViewById<UIButton2>(R.id.practice_button)
            .setText("Practice Mode")
            .setColor(Color.rgb(235, 186, 145))
            .setCornerRadius(45.0f)
            .setTextSize(28f)
            .setOnClickListener {
                stayingInApp = true
                startActivity(Intent(this, OpeningMenuActivity::class.java))
            }

        findViewById<UIButton2>(R.id.start_new_game_button)
            .setText("Start new game")
            .setColor(Color.rgb(235, 186, 145))
            .setCornerRadius(45f)
            .setTextSize(28f)
            .setOnClickListener {
//                createGameDialog.show()
                stayingInApp = true
                startActivity(Intent(this, SinglePlayerGameActivity::class.java))
            }
    }

    private fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {
        for ((gameId, game) in games ?: return) {
            if (!showFinishedGames && game.isFinished()) {
                continue
            }

            val doesCardExist = gameAdapter.updateGameCard(gameId, game.status, game.lastUpdated, game.isPlayingWhite, false)

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

    override fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {
        createGameDialog.setRecentOpponents(opponents ?: return)
    }

    private fun loadResources() {
        val textures = arrayOf(
            R.drawable.wood_diffuse_texture,
            R.drawable.white_pawn,
            R.drawable.white_knight,
            R.drawable.white_bishop,
            R.drawable.white_rook,
            R.drawable.white_queen,
            R.drawable.white_king,
            R.drawable.black_pawn,
            R.drawable.black_knight,
            R.drawable.black_bishop,
            R.drawable.black_rook,
            R.drawable.black_queen,
            R.drawable.black_king,
            R.drawable.diffuse_map_pawn,
            R.drawable.diffuse_map_knight,
            R.drawable.diffuse_map_bishop,
            R.drawable.diffuse_map_rook,
            R.drawable.diffuse_map_queen,
            R.drawable.diffuse_map_king,
            R.drawable.king_checked,
        )

        val models = arrayOf(
            R.raw.pawn_bytes,
            R.raw.knight_bytes,
            R.raw.bishop_bytes,
            R.raw.rook_bytes,
            R.raw.queen_bytes,
            R.raw.king_bytes
        )
//
//        val textureLoader = TextureLoader.getInstance()
//
//        for (textureId in textures) {
//            textureLoader.load(applicationContext.resources, textureId)
//        }
//
//        for (modelId in models) {
//            OBJLoader.preload(applicationContext.resources, modelId)
//        }
        val worker = OneTimeWorkRequestBuilder<LoadResourcesWorker>()
            .setInputData(
                workDataOf(
                    Pair("texture_resources", textures),
                    Pair("model_resources", models)
                )
            ).build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(worker)
    }

}