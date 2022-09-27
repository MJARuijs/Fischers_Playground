package com.mjaruijs.fischersplayground.chess.news

class IntNews(newsType: NewsType, val data: Int) : News(newsType) {

    override fun toString(): String {
        var content = "$newsType"

        if (data != -1) {
            content += "$SEPARATOR$data"
        }
        return content
    }

    companion object {

        private const val SEPARATOR = ','

        fun fromString(content: String): IntNews {
            val separatorIndex = content.indexOf(SEPARATOR)

            if (separatorIndex == -1) {
                return IntNews(NewsType.fromString(content), -1)
            }

            val typeData = content.substring(0, separatorIndex)
            val extraData = content.substring(separatorIndex + 1)

            return IntNews(NewsType.fromString(typeData), extraData.toInt())
        }

    }

}