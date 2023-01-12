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
import com.mjaruijs.fischersplayground.activities.opening.OpeningMenuActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.gameadapter.*
import com.mjaruijs.fischersplayground.chess.game.MultiPlayerGame
import com.mjaruijs.fischersplayground.chess.game.MoveData
import com.mjaruijs.fischersplayground.chess.game.OpponentData
import com.mjaruijs.fischersplayground.dialogs.SearchPlayersDialog
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.parcelable.ParcelablePair
import com.mjaruijs.fischersplayground.parcelable.ParcelableString
import com.mjaruijs.fischersplayground.services.DataManagerService
import com.mjaruijs.fischersplayground.services.LoadResourcesWorker
import com.mjaruijs.fischersplayground.userinterface.RippleEffect
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger
import java.util.Stack

class MainActivity : ClientActivity() {

    override var activityName = "main_activity"

    override val stayInAppOnBackPress = false

    private val searchPlayersDialog = SearchPlayersDialog(::onSearchForPlayers, ::onPlayerInvited)

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


//        sendDataToWorker(Topic.DEBUG, arrayOf(""), 1) {
//
//        }

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

//        Thread {
//            while (dataManager.isLocked()) {
//                Thread.sleep(1)
//            }
//            runOnUiThread {

//        TODO uncomment these 2 lines
        sendToDataManager<ArrayList<MultiPlayerGame>>(DataManagerService.Request.GET_SAVED_GAMES, ::restoreSavedGames)
        sendToDataManager<ArrayList<InviteData>>(DataManagerService.Request.GET_SAVED_INVITES, ::restoreSavedInvites)

//        getData(DataManagerWorker.Request.GET_SAVED_GAMES, {
//            restoreSavedGames(it)
//        })
//
//        getData(DataManagerWorker.Request.GET_SAVED_INVITES, {
//            restoreSavedInvites(it)
//        })

//                restoreSavedGames(dataManager.getSavedGames())
//                restoreSavedInvites(dataManager.getSavedInvites())
//                updateRecentOpponents(dataManager.getRecentOpponents())
//            }
//        }.start()

        stayingInApp = false
    }

    override fun onDestroy() {
        searchPlayersDialog.dismiss()
        super.onDestroy()
    }

    override fun onMessageReceived(topic: Topic, content: Array<String>, messageId: Long) {
        when (topic) {
            Topic.SEARCH_PLAYERS -> onPlayersReceived(content)
            else -> super.onMessageReceived(topic, content, messageId)
        }
    }

    private fun onPlayersReceived(content: Array<String>) {
        searchPlayersDialog.clearPlayers()
        for (playerData in content) {
            if (playerData.isBlank()) {
                continue
            }

            val data = playerData.removePrefix("(").removeSuffix(")").split(',')
            val name = data[0]
            val id = data[1]
            searchPlayersDialog.addPlayers(name, id)
        }
    }

    private fun onSearchForPlayers(query: String) {
        sendNetworkMessage(NetworkMessage(Topic.SEARCH_PLAYERS, "$userId|$query"))
    }

    private fun onPlayerInvited(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        gameAdapter += GameCardItem(inviteId, timeStamp, opponentName, GameStatus.INVITE_PENDING, hasUpdate = false)
        sendToDataManager(DataManagerService.Request.SET_INVITE, Pair("invite", InviteData(inviteId, opponentName, timeStamp, InviteType.PENDING)))
        sendToDataManager(DataManagerService.Request.ADD_RECENT_OPPONENT, Pair("opponent_data", OpponentData(opponentName, opponentId)))
//        dataManager.setInvite(inviteId, InviteData(inviteId, opponentName, timeStamp, InviteType.PENDING))
//        dataManager.saveData(applicationContext)
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
        incomingInviteDialog.setMessage("$opponentName is inviting you for a game!")
        incomingInviteDialog.setRightOnClick {
            sendNetworkMessage(NetworkMessage(Topic.INVITE_ACCEPTED, inviteId))
        }
        incomingInviteDialog.setLeftOnClick {
            sendNetworkMessage(NetworkMessage(Topic.INVITE_REJECTED, inviteId))
        }
        incomingInviteDialog.show()
    }

    override fun onNewGameStarted(output: Parcelable) {
        val gameCard = output as GameCardItem
        val doesCardExist = gameAdapter.updateGameCard(gameCard.id, gameCard.gameStatus, gameCard.lastUpdated, gameCard.isPlayingWhite, gameCard.hasUpdate)
        if (!doesCardExist) {
            gameAdapter += gameCard
        }
    }

    private fun onGameDeleted(gameId: String) {
        sendToDataManager(DataManagerService.Request.REMOVE_GAME, Pair("game_id", gameId))
        sendToDataManager(DataManagerService.Request.REMOVE_INVITE, Pair("invite_id", gameId))

        sendNetworkMessage(NetworkMessage(Topic.DELETE, "$userId|$gameId"))
//        dataManager.removeGame(gameId)
//        dataManager.removeSavedInvite(gameId)
//        dataManager.saveData(applicationContext)
    }

    override fun onIncomingInvite(output: Parcelable) {
        val inviteData = output as InviteData

        Logger.debug(activityName, "OnIncoming invite")

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
        val messageData = output as ChatMessage
        val gameId = messageData.gameId
        gameAdapter.hasUpdate(gameId)
    }

    override fun onRestoreData(output: Parcelable) {
        super.onRestoreData(output)

        sendToDataManager<ArrayList<MultiPlayerGame>>(DataManagerService.Request.GET_SAVED_GAMES, ::restoreSavedGames)
        sendToDataManager<ArrayList<InviteData>>(DataManagerService.Request.GET_SAVED_INVITES, ::restoreSavedInvites)
//        restoreSavedGames(dataManager.getSavedGames())
//        restoreSavedInvites(dataManager.getSavedInvites())
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

        searchPlayersDialog.create(userId, this, ::sendNetworkMessage)

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
                sendToDataManager<ArrayList<OpponentData>>(DataManagerService.Request.GET_RECENT_OPPONENTS, {
                    searchPlayersDialog.setRecentOpponents(it)
                })

                searchPlayersDialog.show()
//                stayingInApp = true
//                startActivity(Intent(this, SinglePlayerGameActivity::class.java))
            }
    }

    private fun restoreSavedGames(games: ArrayList<MultiPlayerGame>?) {
        if (games == null) {
            Logger.debug(activityName, "Tried to restore games but was null")
            return
        }

        Logger.debug(activityName, "Number of saved games: ${games.size}")
        for (game in games) {
            if (!showFinishedGames && game.isFinished()) {
                continue
            }

            val doesCardExist = gameAdapter.updateGameCard(game.gameId, game.status, game.lastUpdated, game.isPlayingWhite, false)

            if (!doesCardExist) {
                gameAdapter += GameCardItem(game.gameId, game.lastUpdated, game.opponentName, game.status, game.isPlayingWhite, false)
            }
        }
    }

    private fun restoreSavedInvites(invites: ArrayList<InviteData>?) {
        if (invites == null) {
            Logger.debug(activityName, "Tried to restore invites but was null")
            return
        }

        Logger.debug(activityName, "Number of saved invites: ${invites.size}")

        for (inviteData in invites) {
            if (gameAdapter.containsCard(inviteData.inviteId)) {
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

            gameAdapter += GameCardItem(inviteData.inviteId, inviteData.timeStamp, inviteData.opponentName, status, hasUpdate = hasUpdate)
        }
    }

//    override fun updateRecentOpponents(opponents: Stack<Pair<String, String>>?) {
//        searchPlayersDialog.setRecentOpponents(opponents ?: return)
//    }

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