package com.mjaruijs.fischersplayground.networking.client

interface Client {

    fun write(message: String) = write(message.toByteArray())

    fun write(bytes: ByteArray)

    fun readMessage() = String(read())

    fun read(): ByteArray

    fun close()

}

