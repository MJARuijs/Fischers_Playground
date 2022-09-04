package com.mjaruijs.fischersplayground.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.mjaruijs.fischersplayground.services.FirebaseService

class RestartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        Toast.makeText(context, "Restarting!", Toast.LENGTH_SHORT).show()

        context.startService(Intent(context, FirebaseService::class.java))
    }
}