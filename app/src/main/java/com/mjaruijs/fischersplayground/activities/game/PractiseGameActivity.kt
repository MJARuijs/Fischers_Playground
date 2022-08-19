package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.os.Messenger
import android.view.View
import android.widget.ImageView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame

class PractiseGameActivity : GameActivity() {

    override var activityName = "practice_activity"

    override var clientMessenger = Messenger(IncomingHandler(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
    }

    override fun onContextCreated() {
        super.onContextCreated()
        game = SinglePlayerGame()
        setGameCallbacks()
    }

}