package com.mjaruijs.fischersplayground.networking.message

class NetworkMessage(val topic: Topic, val content: String) {

    override fun toString() = "[$topic;$content]"

    companion object {

        fun fromString(input: String): NetworkMessage {
            val data = input.removePrefix("[").removeSuffix("]").split(';')

            val topic = Topic.fromString(data[0])
            val content = data[1]

            return NetworkMessage(topic, content)
        }
    }

}