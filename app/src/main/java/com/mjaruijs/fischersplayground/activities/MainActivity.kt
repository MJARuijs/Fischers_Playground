package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
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
import com.mjaruijs.fischersplayground.dialogs.CreateGameDialog
import com.mjaruijs.fischersplayground.dialogs.CreateUsernameDialog
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import java.util.*

class MainActivity : ClientActivity() {

    override var activityName = "main_activity"

    override val stayInAppOnBackPress = false

    private val createUsernameDialog = CreateUsernameDialog()
    private val createGameDialog = CreateGameDialog(::onInvite)

    private lateinit var gameAdapter: GameAdapter

    private var hasNewToken = false

    private var maxTextSize = Float.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        hideActivityDecorations()

        createUsernameDialog.create(this)
        createUsernameDialog.setLayout()

        if (userName == DEFAULT_USER_NAME) {
            createUsernameDialog.show(::saveUserName)
        } else {
            findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        }

        initUIComponents()

//        try {
//            val newsTopic = intent.getStringExtra("news_topic")
//            if (newsTopic != null) {
//                Toast.makeText(applicationContext, "Got news: $newsTopic", Toast.LENGTH_SHORT).show()
//                processNotification(newsTopic)
//            }
//        } catch (e: Exception) {
//            Logger.log(this, e.stackTraceToString(), "main_activity_crash_report.txt")
//        }

        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { result ->
            val token = result.token
            val currentToken = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!

            if (token != currentToken) {
                println("GOT NEW TOKEN: $token")
                getPreference(FIRE_BASE_PREFERENCE_FILE).edit().putString("token", token).apply()

                if (userName == DEFAULT_USER_NAME) {
                    hasNewToken = true
                } else {
                    networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$userId|$token"))
                }
            }
        }
    }

    override fun onMessageReceived(topic: Topic, content: Array<String>) {
        when (topic) {
            Topic.SET_USER_ID -> onIdReceived(content)
            Topic.SEARCH_PLAYERS -> onPlayersReceived(content)
            else -> super.onMessageReceived(topic, content)
        }
    }

    private fun onIdReceived(content: Array<String>) {
        val id = content[0]
        this.userId = id

        savePreference(USER_ID_KEY, id)

        if (hasNewToken) {
            val token = getPreference(FIRE_BASE_PREFERENCE_FILE).getString("token", "")!!
            println("SENDING NEW TOKEN: $token")
            networkManager.sendMessage(NetworkMessage(Topic.FIRE_BASE_TOKEN, "$id|$token"))
        }
        createGameDialog.updateId(id)
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

//    private fun processNotification(topic: String) {
//        when (topic) {
//            "invite" -> {
//                val opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information to show invite dialog: opponent_name")
//                val inviteId = intent.getStringExtra("invite_id") ?: throw IllegalArgumentException("Missing essential information to show invite dialog: invite_id")
//                incomingInviteDialog.showInvite(opponentName, inviteId, networkManager)
//            }
//            "move" -> {
//                val data = intent.getStringExtra("data") ?: throw IllegalArgumentException("Missing essential information to show invite dialog: data")
//                processOpponentMoveData(data) {
//                    val multiplayerIntent = Intent(this, MultiplayerGameActivity::class.java)
//                    multiplayerIntent.putExtra("game_id", it.gameId)
//                    startActivity(multiplayerIntent)
//                    Toast.makeText(this, "Opponent made move!", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    private fun onInvite(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_PENDING, hasUpdate = false)
        dataManager.savedInvites[inviteId] = InviteData(inviteId, opponentName, timeStamp, InviteType.PENDING)
        dataManager.saveInvites(applicationContext)
        dataManager.updateRecentOpponents(applicationContext, Pair(opponentName, opponentId))
    }

    private fun onGameClicked(gameCard: GameCardItem) {
        stayingInApp = true
        gameAdapter.clearUpdate(gameCard, userId)

        if (gameCard.gameStatus == GameStatus.INVITE_RECEIVED) {
            incomingInviteDialog.showInvite(gameCard.opponentName, gameCard.id, networkManager)
        } else if (gameCard.gameStatus != GameStatus.INVITE_PENDING) {
            val intent = Intent(this, MultiplayerGameActivity::class.java)
                .putExtra("id", userId)
                .putExtra("user_name", userName)
                .putExtra("is_playing_white", gameCard.isPlayingWhite)
                .putExtra("game_id", gameCard.id)
                .putExtra("opponent_name", gameCard.opponentName)
            startActivity(intent)
        }
    }

    private fun onGameDeleted(gameId: String) {
        dataManager.removeGame(gameId)
        dataManager.savedInvites.remove(gameId)
        dataManager.saveGames(applicationContext)
        dataManager.saveInvites(applicationContext)
    }

    override fun onNewGameStarted(output: Parcelable) {
        val gameCard = output as GameCardItem
        val doesCardExist = gameAdapter.updateGameCard(gameCard.id, gameCard.gameStatus, gameCard.lastUpdated, gameCard.isPlayingWhite, gameCard.hasUpdate)
        if (!doesCardExist) {
            gameAdapter += gameCard
        }
    }

    override fun onIncomingInvite(output: Parcelable) {
        val inviteData = output as InviteData

        gameAdapter += GameCardItem(inviteData.inviteId, inviteData.timeStamp, inviteData.opponentName, GameStatus.INVITE_RECEIVED, hasUpdate = true)
        incomingInviteDialog.showInvite(inviteData.opponentName, inviteData.inviteId, networkManager)
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
            gameAdapter.hasUpdate((pair.first as ParcelableString).value)
        }
    }

    private fun saveUserName(userName: String) {
        this.userName = userName

        savePreference(USER_NAME_KEY, userName)

        findViewById<TextView>(R.id.weclome_text_view).append(", $userName")
        networkManager.sendMessage(NetworkMessage(Topic.SET_USER_NAME, userName))
    }

    override fun onResume() {
        super.onResume()

        Thread {
            if (!dataManager.isDataLoaded()) {
                while (dataManager.isLoadingData()) {
                    Thread.sleep(1)
                }
                runOnUiThread {
                    restoreSavedGames(dataManager.getSavedGames())
                    restoreSavedInvites(dataManager.savedInvites)
                    updateRecentOpponents(dataManager.recentOpponents)
                }
            } else {
                runOnUiThread {
                    restoreSavedGames(dataManager.getSavedGames())
                    restoreSavedInvites(dataManager.savedInvites)
                    updateRecentOpponents(dataManager.recentOpponents)
                }
            }
        }.start()

        stayingInApp = false
    }

    override fun onDestroy() {
        createGameDialog.dismiss()

        super.onDestroy()
    }

    private fun savePreference(key: String, value: String) {
        val preferences = getPreference(USER_PREFERENCE_FILE)

        with(preferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun hideActivityDecorations() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val isFullscreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

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

        createGameDialog.create(userId, this, networkManager)

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
                    .putExtra("id", userId)
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

    override fun restoreSavedGames(games: HashMap<String, MultiPlayerGame>?) {
        for ((gameId, game) in games ?: return) {
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

//    override fun onInviteReceived(inviteData: Pair<String, InviteData>?) {
//        super.onInviteReceived(inviteData)
//
//        gameAdapter += GameCardItem(inviteData?.first ?: return, inviteData.second.timeStamp, inviteData.second.opponentName, GameStatus.INVITE_RECEIVED, hasUpdate = true)
//    }

    override fun restoreSavedData(data: Triple<HashMap<String, MultiPlayerGame>, HashMap<String, InviteData>, Stack<Pair<String, String>>>?) {
        restoreSavedGames(data?.first ?: return)
        restoreSavedInvites(data.second)
        updateRecentOpponents(data.third)
    }

    override fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {
        createGameDialog.setRecentOpponents(opponents ?: return)
    }

}