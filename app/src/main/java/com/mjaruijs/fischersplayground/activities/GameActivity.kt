package com.mjaruijs.fischersplayground.activities

import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.dialogs.IncomingInviteDialog
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.SurfaceView

class GameActivity : AppCompatActivity() {

    private val inviteReceiver = MessageReceiver("INFO", "invite", ::onIncomingInvite)
    private val gameUpdateReceiver = MessageReceiver("GAME_UPDATE", "game_update", ::onGameUpdate)
    private val incomingInviteDialog = IncomingInviteDialog()

    private val infoFilter = IntentFilter("mjaruijs.fischers_playground.INFO")
    private val gameUpdateFilter = IntentFilter("mjaruijs.fischers_playground.GAME_UPDATE")

    private lateinit var glView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActivityDecorations()

        incomingInviteDialog.create(this)

        if (!intent.hasExtra("is_playing_white")) {
            throw IllegalArgumentException("Missing essential information: is_player_white")
        }

        val isPlayingWhite = intent.getBooleanExtra("is_playing_white", false)
        val opponentName = intent.getStringExtra("opponent_name") ?: throw IllegalArgumentException("Missing essential information: opponent_name")
        val gameId = intent.getStringExtra("game_id") ?: throw IllegalArgumentException("Missing essential information: game_id")

        glView = SurfaceView(this, gameId, isPlayingWhite)
        setContentView(glView)
    }

    private fun onGameUpdate(content: String) {
        val firstSeparatorIndex = content.indexOf('|')
        val secondSeparatorIndex = content.indexOf('|', firstSeparatorIndex + 1)

        val gameId = content.substring(0, firstSeparatorIndex)
        val fromPosition = content.substring(firstSeparatorIndex + 1, secondSeparatorIndex)
        val toPosition = content.substring(secondSeparatorIndex + 1)

        glView.move(Vector2.fromString(fromPosition), Vector2.fromString(toPosition))
    }

    private fun onIncomingInvite(content: String) {
        val firstSeparatorIndex = content.indexOf(';')
        val secondSeparatorIndex = content.indexOf(';', firstSeparatorIndex + 1)

        val invitingUsername = content.substring(0, firstSeparatorIndex)
        val invitingUserId = content.substring(firstSeparatorIndex + 1, secondSeparatorIndex)
        val inviteId = content.substring(secondSeparatorIndex + 1)

        incomingInviteDialog.showInvite(invitingUsername, inviteId)
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

    private fun saveGame() {

    }

    override fun onRestart() {
        super.onRestart()
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(inviteReceiver, infoFilter)
        registerReceiver(gameUpdateReceiver, gameUpdateFilter)
    }

    override fun onStop() {
        unregisterReceiver(inviteReceiver)
        unregisterReceiver(gameUpdateReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        glView.destroy()
        super.onDestroy()
    }
}