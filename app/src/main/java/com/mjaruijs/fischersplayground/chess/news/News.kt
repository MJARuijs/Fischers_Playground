package com.mjaruijs.fischersplayground.chess.news

open class News(val newsType: NewsType) {

    override fun toString(): String {
        return "$newsType"
    }

    companion object {

        fun fromString(content: String): News {
            return News(NewsType.fromString(content))
        }

    }

}