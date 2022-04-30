package com.mjaruijs.fischersplayground.networking.nio

import android.content.Context
import com.mjaruijs.fischersplayground.networking.client.Client
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

abstract class NonBlockingClient(internal val channel: SocketChannel) : Client, Registrable {

    private lateinit var key: SelectionKey

    init {
        channel.configureBlocking(false)
    }

    override fun register(selector: Selector) {
        key = channel.register(selector, SelectionKey.OP_READ, this)
    }

    abstract fun onRead(context: Context)

    override fun close() {
        key.cancel()
    }

}