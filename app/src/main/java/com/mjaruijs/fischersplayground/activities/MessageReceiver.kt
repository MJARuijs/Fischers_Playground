package com.mjaruijs.fischersplayground.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.Logger

class MessageReceiver(private val onRead: (Topic, String, Long) -> Unit): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == "mjaruijs.fischers_playground") {
            val topic = intent.getStringExtra("topic") ?: return
            val message = intent.getStringExtra("content") ?: return
            val messageId = intent.getLongExtra("messageId", 0L)

            onRead(Topic.fromString(topic), message, messageId)
        }
    }
}