package com.mjaruijs.fischersplayground.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mjaruijs.fischersplayground.networking.message.Topic

class MessageReceiver(private val type: String, private val topic: String, private val onRead: (String) -> Unit): BroadcastReceiver() {

    constructor(type: Topic, topic: String, onRead: (String) -> Unit) : this(type.toString(), topic, onRead)

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return

        if (action == "mjaruijs.fischers_playground.$type") {
            val message = intent.getStringExtra(topic) ?: return
            onRead(message)
        }
    }
}