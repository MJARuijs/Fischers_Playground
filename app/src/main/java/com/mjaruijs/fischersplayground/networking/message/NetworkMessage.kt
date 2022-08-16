package com.mjaruijs.fischersplayground.networking.message

class NetworkMessage(val topic: Topic, val category: String, val content: String) {

    override fun toString() = "[$topic;$category;$content]"

    companion object {

        fun fromString(input: String): NetworkMessage {
            val data = input.removePrefix("[").removeSuffix("]").split(';')

            val topic = Topic.fromString(data[0])
            val category = data[1]
            val content = data[2]

            return NetworkMessage(topic, category, content)
        }
    }

}