package com.mjaruijs.fischersplayground.networking.client

import android.content.Context
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.util.Logger
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SecureClient(channel: SocketChannel, address: String, callback: (Message, Context) -> Unit) : EncodedClient(channel, address, callback) {

    private companion object {
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
            callback(Message.fromString(message), context)
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
            Logger.err(e.message)
            throw ClientException("")
        }
    }

    override fun write(message: String) {
        Logger.info("Writing to phone: $message")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey)
        val messageBytes = cipher.doFinal(message.toByteArray(UTF_8))

        val keyBytes = encryptor.doFinal(symmetricKey.encoded)

        write(messageBytes)
        write(keyBytes)
    }
}