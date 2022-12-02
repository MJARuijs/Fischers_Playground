package com.mjaruijs.fischersplayground.activities.opening

import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import java.util.LinkedList

class PracticeSession(val openingName: String, val currentLine: OpeningLine, val lines: LinkedList<OpeningLine>) {

    override fun toString(): String {
        var content = "$openingName\n"
        content += "$currentLine\n"

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
            val currentLine = OpeningLine.fromString(fileLines[lineIndex++])
            val lines = LinkedList<OpeningLine>()
            for (i in lineIndex until fileLines.size) {
                lines += OpeningLine.fromString(fileLines[i])
            }

            return PracticeSession(openingName, currentLine, lines)
        }

    }

}