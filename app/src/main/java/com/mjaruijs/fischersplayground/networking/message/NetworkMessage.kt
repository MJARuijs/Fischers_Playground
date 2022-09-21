package com.mjaruijs.fischersplayground.networking.message

class NetworkMessage(val topic: Topic, val content: String, val id: Long = System.nanoTime()) {

    override fun toString() = "[$topic;$content;$id]"

    companion object {

        fun fromString(input: String): NetworkMessage {
            val data = input.removePrefix("[").removeSuffix("]").split(';')

            val topic = Topic.fromString(data[0])
            val content = data[1]
            val id = data[2].toLong()

            return NetworkMessage(topic, content, id)
        }
    }

}