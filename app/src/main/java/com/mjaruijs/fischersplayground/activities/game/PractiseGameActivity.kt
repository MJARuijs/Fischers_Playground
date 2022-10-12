package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.fragments.OpeningMovesFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeActionButtonsFragment
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

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, PracticeActionButtonsFragment(::requestRender, networkManager, ::onBackClicked, ::onForwardClicked))
            replace(R.id.lower_fragment_container, OpeningMovesFragment(), "opening_moves")
        }
    }

    override fun onContextCreated() {
        super.onContextCreated()
        runOnUiThread {
            val constraints = ConstraintSet()
            constraints.clone(gameLayout)
            constraints.clear(R.id.opengl_view, ConstraintSet.TOP)
            constraints.clear(R.id.opengl_view, ConstraintSet.BOTTOM)

            constraints.applyTo(gameLayout)
            gameLayout.invalidate()
        }

        setGameCallbacks()
        setGameForRenderer()
    }

    override fun onResume() {
        getActionBarFragment().game = game
        findFragment<OpeningMovesFragment>().setGame(game as SinglePlayerGame)

        super.onResume()
    }

    override fun onMoveMade(move: Move) {
        runOnUiThread {
            val fragment = findFragment<OpeningMovesFragment>()
            fragment.addMove(move)
            gameLayout.invalidate()
        }
    }

    private fun onBackClicked() {
        val fragment = findFragment<OpeningMovesFragment>()
        fragment.onBackClicked()
    }

    private fun onForwardClicked() {
        val fragment = findFragment<OpeningMovesFragment>()
        fragment.onForwardClicked()
    }

}