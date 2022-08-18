package com.mjaruijs.fischersplayground.activities.game

import android.app.Activity
import android.os.Handler
import android.os.Message
import com.mjaruijs.fischersplayground.activities.game.TestActivity

object TestActivity : Activity() {


    private var test = 0

    internal class Hand : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            test = 1
        }
    }
}