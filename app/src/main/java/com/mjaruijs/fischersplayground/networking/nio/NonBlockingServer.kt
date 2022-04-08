package com.mjaruijs.fischersplayground.networking.nio

import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

abstract class NonBlockingServer(address: String, port: Int) : Registrable {

    private val channel = ServerSocketChannel.open()!!

    init {
        channel.bind(InetSocketAddress(address, port))
        channel.configureBlocking(false)
    }

    final override fun register(selector: Selector) {
        channel.register(selector, SelectionKey.OP_ACCEPT, this)
    }

    fun accept(): SocketChannel {
        return channel.accept()
    }

    abstract fun onAccept(channel: SocketChannel)

    open fun close() {
        channel.close()
    }
}