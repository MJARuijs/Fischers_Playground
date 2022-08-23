package com.mjaruijs.fischersplayground.services

import android.content.Context
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
//        super.onNewToken(p0)
//        println("New token was generated: $token")
//        Toast.makeText(this, "Got new token! $token", Toast.LENGTH_SHORT).show()
        getSharedPreferences("fcm_token", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    companion object {

        fun getToken(context: Context): String {
            return context.getSharedPreferences("fcm_token", MODE_PRIVATE).getString("token", "")!!
        }

    }

}