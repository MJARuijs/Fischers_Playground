package com.mjaruijs.fischersplayground.adapters.openingadapter

import com.mjaruijs.fischersplayground.adapters.variationadapter.Variation
import com.mjaruijs.fischersplayground.chess.pieces.Team

class Opening(val name: String, val team: Team, val variations: ArrayList<Variation> = arrayListOf()) {

    fun addVariation(variation: Variation) {
        variations += variation
    }

    fun clear() {
        variations.clear()
    }

    operator fun plusAssign(variation: Variation) {
        variations += variation
    }

    fun getVariation(name: String): Variation? {
        return variations.find { variation -> variation.name == name }
    }

    fun addFromString(content: String) {
        val variationsStrings = content.split("*")
        for (variationString in variationsStrings) {
            variations += Variation.fromString(variationString)
        }
//        val lineData = content.split("\n")

//        for (line in lineData) {
//            if (line.isNotBlank()) {
//                variations += OpeningLine.fromString(line)
//            }
//        }
    }

    override fun toString(): String {
        var content = ""
        for ((i, variation) in variations.withIndex()) {
            content += "$variation"

            if (i != variations.size - 1) {
                content += "*"
            }
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
        result = 31 * result + variations.hashCode()
        return result
    }

}