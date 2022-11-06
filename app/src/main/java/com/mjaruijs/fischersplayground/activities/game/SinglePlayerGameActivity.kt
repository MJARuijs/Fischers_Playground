package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Constraints
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeActionButtonsFragment
import com.mjaruijs.fischersplayground.util.Time

class SinglePlayerGameActivity : GameActivity() {

    override var activityName = "single_player_activity"

    override var isSinglePlayer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        game = SinglePlayerGame(true, Time.getFullTimeStamp())

        val playerBundle = Bundle()
        playerBundle.putString("player_name", userName)
        playerBundle.putString("team", if (isPlayingWhite) "WHITE" else "BLACK")
        playerBundle.putBoolean("hide_status_icon", true)

        val opponentBundle = Bundle()
        opponentBundle.putString("player_name", "Opponent")
        opponentBundle.putString("team", if (isPlayingWhite) "BLACK" else "WHITE")
        opponentBundle.putBoolean("hide_status_icon", isSinglePlayer)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.lower_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
            replace(R.id.upper_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
//            replace(R.id.action_buttons_fragment, PracticeActionButtonsFragment(::requestRender, networkManager))
        }

        val lowerFragment = findViewById<FragmentContainerView>(R.id.lower_fragment_container)

        Log.d("MyTag", "${lowerFragment.layoutParams.width}, ${lowerFragment.layoutParams.height}")

        val layoutParams = Constraints.LayoutParams(Constraints.LayoutParams.MATCH_CONSTRAINT, Constraints.LayoutParams.WRAP_CONTENT)
        lowerFragment.layoutParams = layoutParams

        val margin = dpToPx(resources, 8)

        val constraints = ConstraintSet()
        constraints.clone(gameLayout)
        constraints.connect(R.id.lower_fragment_container, ConstraintSet.TOP, R.id.opengl_view, ConstraintSet.BOTTOM, margin)
        constraints.connect(R.id.lower_fragment_container, ConstraintSet.BOTTOM, R.id.action_buttons_fragment, ConstraintSet.TOP, margin)
        constraints.connect(R.id.lower_fragment_container, ConstraintSet.LEFT, gameLayout.id, ConstraintSet.LEFT, margin)
        constraints.connect(R.id.lower_fragment_container, ConstraintSet.RIGHT, gameLayout.id, ConstraintSet.RIGHT, margin)

        constraints.applyTo(gameLayout)
        Log.d("MyTag", "${lowerFragment.layoutParams.width}, ${lowerFragment.layoutParams.height}")

        gameLayout.invalidate()
    }

    override fun onResume() {
//        getActionBarFragment()?.game = game
        super.onResume()
    }

    override fun onContextCreated() {
        super.onContextCreated()

        setGameCallbacks()
        setGameForRenderer()
    }
}