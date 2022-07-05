package com.mjaruijs.fischersplayground.chess.news

class News(val newsType: NewsType, val data: Int = -1) {

    override fun toString(): String {
        var content = "$newsType"

        if (data != -1) {
            content += "$SEPARATOR$data"
        }
        return content
    }

    companion object {

        private const val SEPARATOR = ','

        fun fromString(content: String): News {
            val separatorIndex = content.indexOf(SEPARATOR)

            if (separatorIndex == -1) {
                return News(NewsType.fromString(content))
            }

            val typeData = content.substring(0, separatorIndex)
            val extraData = content.substring(separatorIndex + 1)

            return News(NewsType.fromString(typeData), extraData.toInt())
        }

    }

}