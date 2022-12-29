package com.mjaruijs.fischersplayground.adapters.variationadapter

import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.util.Logger

class Variation(var name: String, val lines: ArrayList<OpeningLine> = arrayListOf()) {

    fun addLine(line: OpeningLine) {
        lines += line
    }

    fun clear() {
        lines.clear()
    }

    operator fun plusAssign(openingLine: OpeningLine) {
        lines += openingLine
    }

    override fun toString(): String {
        var content = "$name\n"
        for (line in lines) {
            content += "$line\n"
        }
        return content
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (this === other) {
            return true
        }

        if (other !is Variation) {
            return false
        }

        if (name != other.name) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + lines.hashCode()
        return result
    }

    companion object {

        private const val TAG = "Variation"

        fun fromString(content: String): Variation {
            val fileLines = content.split("\n")
            val name = fileLines[0].trim()
            val openingLines = ArrayList<OpeningLine>()

            for (i in 1 until fileLines.size) {
                val line = fileLines[i]
                if (line.isEmpty()) {
                    continue
                }

                val openingLine = OpeningLine.fromString(line)
                openingLines += openingLine
            }

            return Variation(name, openingLines)
        }

    }

}