package com.mjaruijs.fischersplayground.chess.news

import com.mjaruijs.fischersplayground.chess.game.MoveData

class MoveNews(newsType: NewsType, val moveData: MoveData) : News(newsType) {

    override fun toString(): String {
        return "$newsType,$moveData"
    }

    companion object {

        fun fromString(content: String): MoveNews {
            val separatorIndex = content.indexOf(',')

            val newsType = NewsType.fromString(content.substring(0, separatorIndex))
            val moveData = MoveData.fromString(content.substring(separatorIndex + 1))

            return MoveNews(newsType, moveData)
        }

    }

}