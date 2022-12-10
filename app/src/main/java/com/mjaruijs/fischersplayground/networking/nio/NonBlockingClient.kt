package com.mjaruijs.fischersplayground.networking.nio

import android.content.Context
import com.mjaruijs.fischersplayground.networking.client.Client
import com.mjaruijs.fischersplayground.util.Logger
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

abstract class NonBlockingClient(internal val channel: SocketChannel) : Client, Registrable {

    private lateinit var key: SelectionKey

    init {
        channel.socket().keepAlive = true
        channel.configureBlocking(false)
    }

    override fun register(selector: Selector) {
        try {
            key = channel.register(selector, SelectionKey.OP_READ, this)
        } catch (e: Exception) {
            Logger.error(TAG, e.stackTraceToString())
        }
    }

    abstract fun onRead(context: Context)

    override fun close() {
        if (this::key.isInitialized) {
            key.cancel()
        }
    }

    companion object {

        private const val TAG = "NonBlockingClient"

    }

}