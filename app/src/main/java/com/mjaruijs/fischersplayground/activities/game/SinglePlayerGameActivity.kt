package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame

class SinglePlayerGameActivity : GameActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
    }

    override fun onContextCreated() {
        game = SinglePlayerGame()
        super.onContextCreated()
    }

}