package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.util.Time

class SinglePlayerGameActivity : GameActivity() {

    override var activityName = "single_player_activity"

    override var isSinglePlayer = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
    }

    override fun onContextCreated() {
        game = SinglePlayerGame(true, Time.getFullTimeStamp())
        super.onContextCreated()
    }

}