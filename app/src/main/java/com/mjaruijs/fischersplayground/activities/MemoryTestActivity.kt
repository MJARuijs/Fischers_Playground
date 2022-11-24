package com.mjaruijs.fischersplayground.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.GameActivity

class MemoryTestActivity : GameActivity() {
    override var isSinglePlayer = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
    }
}