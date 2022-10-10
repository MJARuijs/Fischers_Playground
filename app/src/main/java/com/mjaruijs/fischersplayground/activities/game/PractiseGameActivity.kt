package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.constraintlayout.widget.Constraints
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.fragments.OpeningMovesFragment
import com.mjaruijs.fischersplayground.fragments.PlayerCardFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeActionButtonsFragment
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.Time

class PractiseGameActivity : GameActivity() {

    override var activityName = "practice_activity"

    override var isSinglePlayer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        findViewById<FragmentContainerView>(R.id.upper_fragment_container).visibility = View.GONE

        val isPlayingWhite = intent.getBooleanExtra("is_playing_white", true)
        game = SinglePlayerGame(isPlayingWhite, Time.getFullTimeStamp())

//        val playerBundle = Bundle()
//        playerBundle.putString("player_name", userName)
//        playerBundle.putString("team", if (isPlayingWhite) "WHITE" else "BLACK")
//        playerBundle.putBoolean("hide_status_icon", true)
//
//        val opponentBundle = Bundle()
//        opponentBundle.putString("player_name", "Opponent")
//        opponentBundle.putString("team", if (isPlayingWhite) "BLACK" else "WHITE")
//        opponentBundle.putBoolean("hide_status_icon", true)



        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, PracticeActionButtonsFragment(::requestRender, networkManager))
            replace(R.id.lower_fragment_container, OpeningMovesFragment(), "opening_moves")
//            replace(R.id.upper_fragment_container, PlayerCardFragment::class.java, playerBundle, "player")
//            replace(R.id.lower_fragment_container, PlayerCardFragment::class.java, opponentBundle, "opponent")
        }





    }

    override fun onContextCreated() {
        super.onContextCreated()
        runOnUiThread {
            val constraints = ConstraintSet()
            constraints.clone(gameLayout)
            constraints.clear(R.id.opengl_view, ConstraintSet.TOP)
            constraints.clear(R.id.opengl_view, ConstraintSet.BOTTOM)

//            constraints.clear(R.id.lower_fragment_container, ConstraintSet.BOTTOM)
//            constraints.clear(R.id.lower_fragment_container, ConstraintSet.TOP)
//            constraints.connect(R.id.lower_fragment_container, ConstraintSet.TOP, R.id.opengl_view, ConstraintSet.BOTTOM, 0)
//            constraints.connect(R.id.lower_fragment_container, ConstraintSet.BOTTOM, R.id.action_buttons_fragment, ConstraintSet.TOP, 0)

            constraints.applyTo(gameLayout)
            gameLayout.invalidate()
        }

        setGameCallbacks()
        setGameForRenderer()
    }

    override fun onResume() {
        getActionBarFragment()?.game = game
        super.onResume()
    }

    override fun onMoveMade(move: Move) {
        runOnUiThread {
            val fragment = findFragment<OpeningMovesFragment>("opening_moves")
            fragment.addMove(move)
            gameLayout.invalidate()
        }
    }

}