package com.mjaruijs.fischersplayground.networking

import com.mjaruijs.fischersplayground.networking.client.EncodedClient
import com.mjaruijs.fischersplayground.networking.nio.Manager
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean

object NetworkManager {

    private const val SERVER_IP = "192.168.178.71"
    private const val SERVER_PORT = 4500

    private val clientInitializing = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)

    private val manager = Manager("Client")

    private lateinit var client: EncodedClient

    fun run() {
        if (initialized.get()) {
            return
        }

        Thread {
            try {
                clientInitializing.set(true)
                client = EncodedClient(SERVER_IP, SERVER_PORT, NetworkManager::onRead)
                initialized.set(true)
            } catch (e: ConnectException) {
                println("Failed to connect to server..")
                initialized.set(false)
            } finally {
                clientInitializing.set(false)
            }

            if (initialized.get()) {
                Thread(manager).start()
                manager.register(client)
            }
        }.start()
    }

    fun sendMessage(message: String) {
        Thread {
            while (clientInitializing.get()) {}

            client.write(message)
        }.start()
    }

    private fun onRead(message: String, address: String) {
        println("Received message from: $address: $message")
    }

}