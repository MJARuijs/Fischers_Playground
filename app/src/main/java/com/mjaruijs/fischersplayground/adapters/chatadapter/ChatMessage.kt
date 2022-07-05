package com.mjaruijs.fischersplayground.adapters.chatadapter

class ChatMessage(val timeStamp: String, val message: String, val type: MessageType) {

//    fun convertTimeNotation(): String {
//        val stringNotation = timeStamp.toString()
//        println(timeStamp.toString())
//        var convertedTimeNotation = ""
//        convertedTimeNotation += stringNotation[0]
//        convertedTimeNotation += stringNotation[1]
//        convertedTimeNotation += ":"
//        convertedTimeNotation += stringNotation[2]
//        convertedTimeNotation += stringNotation[3]
//        return convertedTimeNotation
//    }

    override fun toString(): String {
        return "$timeStamp,$message,$type"
    }

}