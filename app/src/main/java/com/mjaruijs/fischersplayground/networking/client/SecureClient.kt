package com.mjaruijs.fischersplayground.networking.client

import android.content.Context
import android.util.Log
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.util.Logger
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureClient(channel: SocketChannel, callback: (NetworkMessage, Context) -> Unit) : EncodedClient(channel, callback) {

    constructor(address: String, port: Int, callback: (NetworkMessage, Context) -> Unit) : this(SocketChannel.open(InetSocketAddress(address, port)), callback)

    private companion object {

        private const val TAG = "SecureClient"

        val symmetricGenerator: KeyGenerator = KeyGenerator.getInstance("AES")
        val asymmetricGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")

        init {
            asymmetricGenerator.initialize(2048)
            symmetricGenerator.init(128)
        }
    }

    private val encryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val decryptor = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    private val symmetricKey: SecretKey

    init {
        symmetricKey = symmetricGenerator.generateKey()
        val keyPair = asymmetricGenerator.generateKeyPair()
        val clientKey = keyPair.private

        write(keyPair.public.encoded)

        val keyFactory = KeyFactory.getInstance("RSA")
        val serverKey = keyFactory.generatePublic(X509EncodedKeySpec(read()))

        encryptor.init(Cipher.PUBLIC_KEY, serverKey)
        decryptor.init(Cipher.PRIVATE_KEY, clientKey)
    }

    override fun onRead(context: Context) {
        val message = decodeMessage()

        Thread {
            callback(NetworkMessage.fromString(message), context)
        }.start()
    }

    fun decodeMessage(): String {
        return try {
            val message = read()
            val key = read()

            val decryptedKey = decryptor.doFinal(key)

            val secretKey = SecretKeySpec(decryptedKey, 0, decryptedKey.size, "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decryptedMessage = cipher.doFinal(message)

            String(decryptedMessage, UTF_8)
        } catch (e: Exception) {
            throw e
        }
    }

    fun write(message: NetworkMessage) {
//        val cipher = Cipher.getInstance("AES")
//        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
//
//        val messageIdString = "${message.id};".toByteArray()
////        val messageString = "${message.topic};${message.content}"
//
//        val msg = "${message.id};${message.topic};${message.content}"
//
//        val encryptedMessage = cipher.doFinal(msg.toByteArray())
//
////        val buffer = ByteBuffer.allocate(messageIdString.size + encryptedMessage.size)
////        buffer.put(messageIdString)
////        buffer.put(encryptedMessage)
//
//        Logger.debug(TAG, "Message: ${message.id} ${message.topic} ${message.content}")
////        Logger.debug(TAG, "Writing: ${String(buffer.array())}")
//        Logger.debug(TAG, "EncryptedMessage: ${String(encryptedMessage)}")
//        write(encryptedMessage)
//
//        val keyBytes = encryptor.doFinal(symmetricKey.encoded)
//        write(keyBytes)

        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)

        val messageIdString = "${message.id};".toByteArray()
        val messageString = "${message.topic};${message.content}"
        val encryptedMessage = cipher.doFinal(messageString.toByteArray(UTF_8))
//        messageIdString.size +
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + encryptedMessage.size)
//        buffer.put(messageIdString)
//        buffer.putLong(message.id)
        buffer.putLong(message.id)
        buffer.put(encryptedMessage)

//        Logger.debug(TAG, "Message: ${message.id} ${message.topic} ${message.content}")
//        Logger.debug(TAG, "Writing: ${String(buffer.array())}")
//        Logger.debug(TAG, "EncryptedMessage: ${String(encryptedMessage)}")
//        Logger.debug(TAG, "Buffer size: ${buffer.array().size} ${encryptedMessage.size}")
        write(buffer.array())

        val keyBytes = encryptor.doFinal(symmetricKey.encoded)
//        Logger.debug(TAG, "Key: ${String(keyBytes)}")

        write(keyBytes)
    }

    override fun write(message: String) {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)

        val messageBytes = cipher.doFinal(message.toByteArray())
        val keyBytes = encryptor.doFinal(symmetricKey.encoded)

//        Logger.debug("SecureClient", "Writing message: ${String(messageBytes)}")
//        Logger.debug("SecureClient", "Writing key: ${String(keyBytes)}")

//        Logger.debug("SecureClient", "Writing message bytes")
        write(messageBytes)

//        Logger.debug("SecureClient", "Writing key bytes")
        write(keyBytes)
    }

    private fun createInitializationVector(): ByteArray {

//        val vector = ByteArray(16)
//        val secureRandom = SecureRandom()
//        secureRandom.nextBytes(vector)
//        return vector
        return byteArrayOf(
            'A'.code.toByte(),
            'B'.code.toByte(),
            'C'.code.toByte(),
            'D'.code.toByte(),
            'E'.code.toByte(),
            'F'.code.toByte(),
            'G'.code.toByte(),
            'H'.code.toByte(),
            'I'.code.toByte(),
            'J'.code.toByte(),
            'K'.code.toByte(),
            'L'.code.toByte(),
            'M'.code.toByte(),
            'N'.code.toByte(),
            'O'.code.toByte(),
            'P'.code.toByte()
        )
    }
}