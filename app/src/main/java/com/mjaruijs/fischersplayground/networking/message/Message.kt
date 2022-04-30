package com.mjaruijs.fischersplayground.networking.message

class Message(val topic: Topic, val content: String, var sender: String = "") {

    override fun toString() = "[$topic;$content;$sender]"

    companion object {
        fun fromString(input: String, sender: String): Message {
            val topicStartIndex = 1
            val topicEndIndex = input.indexOf(';')
            val topicString = input.substring(topicStartIndex, topicEndIndex)
            val topic = Topic.fromString(topicString)

            val messageEndIndex = input.lastIndexOf(';')
            val messageContent = input.substring(topicEndIndex + 1, messageEndIndex)
//
//            val senderEndIndex = input.lastIndexOf(']')
//            val senderString= input.substring()

            return Message(topic, messageContent, sender)
        }
    }

}