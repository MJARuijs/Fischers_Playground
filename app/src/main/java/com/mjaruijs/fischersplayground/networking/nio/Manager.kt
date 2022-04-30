package com.mjaruijs.fischersplayground.networking.nio

import android.content.Context
import com.mjaruijs.fischersplayground.networking.client.ClientException
import com.mjaruijs.fischersplayground.util.Logger
import java.nio.channels.Selector
import java.util.concurrent.atomic.AtomicBoolean

class Manager(private val name: String) : Runnable {

    private val selector = Selector.open()
    private val running = AtomicBoolean(true)
    private val registering = AtomicBoolean(false)

    lateinit var context: Context

    private lateinit var onClientDisconnect: (String) -> Unit

    fun setOnClientDisconnect(callback: (String) -> Unit) {
        onClientDisconnect = callback
    }

    fun register(obj: Registrable) {
        registering.set(true)
        selector.wakeup()
        obj.register(selector)
        registering.set(false)
    }

    override fun run() {
        while (running.get()) {

            try {
                while (registering.get()) {}

                selector.select()
                val keys = selector.selectedKeys()
                val iterator = keys.iterator()
                while (iterator.hasNext()) {

                    val key = iterator.next()
                    iterator.remove()
                    if (key.isValid) {

                        if (key.isAcceptable) {
                            val server = key.attachment() as NonBlockingServer
                            server.onAccept(server.accept())
                        }

                        if (key.isReadable) {
                            val client = key.attachment() as NonBlockingClient
                            try {
                                client.onRead(context)
                            } catch (exception: ClientException) {
                                if (::onClientDisconnect.isInitialized) {
                                    val clientInfo = client.channel.remoteAddress.toString()
                                    val startIndex = clientInfo.indexOf("/")
                                    val endIndex = clientInfo.indexOf(":")
                                    val address = clientInfo.substring(startIndex + 1, endIndex)
                                    onClientDisconnect(address)
                                }
                                Logger.warn("${name}_Manager: CLIENT DISCONNECTED! ${client.channel.remoteAddress}")
                                client.close()
                                key.cancel()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.warn("STOPPED: ${e.message}")
            }
        }
    }

    fun stop() {
        running.set(false)
        registering.set(false)
        selector.close()
    }

}