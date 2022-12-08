package com.mjaruijs.fischersplayground.activities.opening

import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.pieces.Team
import java.util.LinkedList

class PracticeSession(val openingName: String, val team: Team, val currentLineIndex: Int, val totalLineCount: Int, val currentLine: OpeningLine?, val nextLine: OpeningLine?, val lines: LinkedList<OpeningLine>) {

    override fun toString(): String {
        var content = "$openingName^$team^$currentLineIndex^$totalLineCount^$currentLine^$nextLine\n"

        for (line in lines) {
            content += "$line\n"
        }

        return content
    }

    companion object {

        fun fromString(content: String): PracticeSession {
            val fileLines = content.split("\n")

            val data = fileLines[0].split("^")
            val openingName = data[0]
            val team = Team.fromString(data[1])
            val currentLineIndex = data[2].toInt()
            val maxLineIndex = data[3].toInt()
            val currentLine = OpeningLine.fromString(data[4])
            val nextLineString = data[5]

            if (nextLineString == "null") {
                return PracticeSession(openingName, team, currentLineIndex, maxLineIndex, currentLine, null, LinkedList())
            }

            val nextLine = OpeningLine.fromString(nextLineString)

            val lines = LinkedList<OpeningLine>()
            for (i in 1 until fileLines.size) {
                val fileLine = fileLines[i]
                if (fileLine.isNotBlank()) {
                    lines += OpeningLine.fromString(fileLine)
                }
            }

            return PracticeSession(openingName, team, currentLineIndex, maxLineIndex, currentLine, nextLine, lines)
        }

    }

}