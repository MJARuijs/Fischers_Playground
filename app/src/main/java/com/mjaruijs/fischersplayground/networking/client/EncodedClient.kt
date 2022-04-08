package com.mjaruijs.fischersplayground.networking.client

import com.mjaruijs.fischersplayground.networking.nio.NonBlockingClient
import java.net.InetSocketAddress
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.*

open class EncodedClient(channel: SocketChannel, val address: String, val callback: (String, String) -> Unit) : NonBlockingClient(channel) {

    constructor(address: String, port: Int, callback: (String, String) -> Unit): this(SocketChannel.open(InetSocketAddress(address, port)), address, callback)

    final override fun write(bytes: ByteArray) {
        try {
            val encodedBytes = Base64.getEncoder().encode(bytes)
            val bufferSize = encodedBytes.size.toString().toByteArray()
            val encodedSize = Base64.getEncoder().encode(bufferSize)
            val buffer = ByteBuffer.allocate(encodedBytes.size + encodedSize.size)
            buffer.put(encodedSize)
            buffer.put(encodedBytes)
            (buffer as Buffer).rewind()
            channel.write(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw ClientException("Invalid write!")
        }
    }

    @Throws (ClientException::class)
    final override fun read(): ByteArray {

        // Read size
        val readSizeBuffer = ByteBuffer.allocate(Integer.BYTES)
        var sizeBytesRead = channel.read(readSizeBuffer)

        while (sizeBytesRead == 0) {
            sizeBytesRead = channel.read(readSizeBuffer)
        }

        if (sizeBytesRead == -1) {
            throw ClientException("Size was invalid")
        }

        (readSizeBuffer as Buffer).rewind()

        // Read data
        val sizeArray = ByteArray(4)
        var index = 0

        while (readSizeBuffer.hasRemaining()) {
            val b = readSizeBuffer.get()
            sizeArray[index] = b
            index++
        }

        val size = String(Base64.getDecoder().decode(sizeArray)).toInt()

        val data = ByteBuffer.allocate(size)
        var bytesRead = 0

        while (bytesRead < size) {
            bytesRead += channel.read(data)
        }

        (data as Buffer).rewind()
        return Base64.getDecoder().decode(data).array()
    }

    override fun onRead() {
        val message = readMessage()

        Thread {
            callback(message, address)
        }.start()
    }

    override fun close() {
        super.close()
        channel.close()
    }
}