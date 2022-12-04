package com.mjaruijs.fischersplayground.activities.opening

import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.pieces.Team
import java.util.LinkedList

class PracticeSession(val openingName: String, val team: Team, val currentLine: OpeningLine?, val nextLine: OpeningLine?, val lines: LinkedList<OpeningLine>) {

    override fun toString(): String {
        var content = "$openingName\n"
        content += "$team\n"
        content += "$currentLine\n"
        content += "$nextLine\n"

        for (line in lines) {
            content += "$line\n"
        }

        return content
    }

    companion object {

        fun fromString(content: String): PracticeSession {
            val fileLines = content.split("\n")

            var lineIndex = 0

            val openingName = fileLines[lineIndex++]
            val team = Team.fromString(fileLines[lineIndex++])
            val currentLine = OpeningLine.fromString(fileLines[lineIndex++])

            val nextFileLine = fileLines[lineIndex++]
            if (nextFileLine == "null") {
                return PracticeSession(openingName, team, currentLine, null, LinkedList())
            }

            val nextLine = OpeningLine.fromString(nextFileLine)

            val lines = LinkedList<OpeningLine>()
            for (i in lineIndex until fileLines.size) {
                lines += OpeningLine.fromString(fileLines[i])
            }

            return PracticeSession(openingName, team, currentLine, nextLine, lines)
        }

    }

}