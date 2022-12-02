package com.mjaruijs.fischersplayground.adapters.variationadapter

import com.google.errorprone.annotations.Var
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine

class Variation(val name: String, val lines: ArrayList<OpeningLine> = arrayListOf()) {

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

        fun fromString(content: String): Variation {
            val fileLines = content.split("\n")
            val name = fileLines[0].trim()
            val openingLines = ArrayList<OpeningLine>()

            for (i in 1 until fileLines.size) {
                val line = fileLines[i]
                if (line.isEmpty()) {
                    continue
                }

                openingLines += OpeningLine.fromString(line)
            }

            return Variation(name, openingLines)
        }

    }

}