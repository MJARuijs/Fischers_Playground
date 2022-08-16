package com.mjaruijs.fischersplayground.util

import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic

object ErrorHandler {

    fun reportCrash(e: Exception) {
        val serverConnected = reportToServer(e)
        if (!serverConnected) {
            saveLocally(e)
        }
    }

    private fun reportToServer(e: Exception): Boolean {
        if (NetworkManager.isRunning()) {
            NetworkManager.sendMessage(NetworkMessage(Topic.CRASH_REPORT, "", e.toString()))
            return true
        }
        return false
    }

    private fun saveLocally(e: Exception) {
        println(e)
    }

}