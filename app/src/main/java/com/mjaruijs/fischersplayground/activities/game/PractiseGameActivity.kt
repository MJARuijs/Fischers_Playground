package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.os.Messenger
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.fragments.actionbars.PracticeActionButtonsFragment

class PractiseGameActivity : GameActivity() {

    override var activityName = "practice_activity"

    override var clientMessenger = Messenger(IncomingHandler(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        game = SinglePlayerGame()
        setGameCallbacks()

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, PracticeActionButtonsFragment {
                glView.requestRender()
            })
        }

    }

//    override fun onContextCreated() {
//        super.onContextCreated()
//        game = SinglePlayerGame()
//        getActionBarFragment().game = game
//        setGameCallbacks()
//    }

    override fun onResume() {
        getActionBarFragment().game = game
        super.onResume()
    }
}