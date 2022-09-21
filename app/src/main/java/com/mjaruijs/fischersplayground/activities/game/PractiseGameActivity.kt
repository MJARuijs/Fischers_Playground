package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeActionButtonsFragment
import com.mjaruijs.fischersplayground.util.Time

class PractiseGameActivity : GameActivity() {

    override var activityName = "practice_activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        game = SinglePlayerGame(Time.getFullTimeStamp())

        val playerBundle = Bundle()
        playerBundle.putString("player_name", userName)
        playerBundle.putString("team", if (isPlayingWhite) "WHITE" else "BLACK")
        playerBundle.putBoolean("hide_status_icon", true)

        val opponentBundle = Bundle()
        opponentBundle.putString("player_name", "Opponent")
        opponentBundle.putString("team", if (isPlayingWhite) "BLACK" else "WHITE")
        opponentBundle.putBoolean("hide_status_icon", true)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, PracticeActionButtonsFragment(::requestRender, networkManager))
            replace(R.id.player_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
            replace(R.id.opponent_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
        }
    }

    override fun onContextCreated() {
        super.onContextCreated()
        setGameCallbacks()
    }

    override fun onResume() {
        getActionBarFragment()?.game = game
        super.onResume()
    }
}