package com.mjaruijs.fischersplayground.networking.message

class Message(val topic: Topic, val category: String, val content: String) {

    override fun toString() = "[$topic;$category;$content]"

    companion object {

        fun fromString(input: String): Message {
            val data = input.removePrefix("[").removeSuffix("]").split(';')

            val topic = Topic.fromString(data[0])
            val category = data[1]
            val content = data[2]

            return Message(topic, category, content)
        }
    }

}