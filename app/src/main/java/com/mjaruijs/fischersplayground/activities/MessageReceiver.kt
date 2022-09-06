package com.mjaruijs.fischersplayground.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.message.Topic

class MessageReceiver(private val onRead: (Topic, Array<String>) -> Unit): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == "mjaruijs.fischers_playground") {
            val topic = intent.getStringExtra("topic") ?: return
            val message = intent.getStringArrayExtra("content") ?: return

            onRead(Topic.fromString(topic), message)
        }
    }
}