package com.mjaruijs.fischersplayground.adapters.openingadapter

import com.mjaruijs.fischersplayground.chess.pieces.Team

class Opening(val name: String, val team: Team, val lines: ArrayList<OpeningLine> = arrayListOf()) {

    fun addLine(line: OpeningLine) {
        lines += line
    }

    operator fun plusAssign(openingLine: OpeningLine) {
        lines += openingLine
    }

    fun addFromString(content: String) {
        val lineData = content.split("\n")

        for (line in lineData) {
            if (line.isNotBlank()) {
                lines += OpeningLine.fromString(line)
            }
        }
    }

    override fun toString(): String {
        var content = ""
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

        if (other !is Opening) {
            return false
        }

        if (name != other.name) {
            return false
        }

        if (team != other.team) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + team.hashCode()
        result = 31 * result + lines.hashCode()
        return result
    }

}