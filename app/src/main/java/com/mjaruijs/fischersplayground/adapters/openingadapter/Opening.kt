package com.mjaruijs.fischersplayground.adapters.openingadapter

import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.variationadapter.Variation
import com.mjaruijs.fischersplayground.chess.pieces.Team
import kotlin.random.Random

class Opening(val name: String, val team: Team, val variations: ArrayList<Variation> = arrayListOf()) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        Team.fromString(parcel.readString()!!)
    ) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            parcel.readList(variations, Variation::class.java.classLoader)
        } else {
            parcel.readList(variations, Variation::class.java.classLoader, Variation::class.java)
        }

    }

    override fun describeContents() = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(team.toString())
        parcel.writeList(variations)
    }

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
        var content = "$name|$team\n"
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

    companion object CREATOR : Parcelable.Creator<Opening> {

        fun fromString(content: String): Opening {
            val lines = content.split("\n")

//            val firstLineSeparatorIndex = content.indexOf('\n')
//            val openingInfo = content.substring(0, firstLineSeparatorIndex).split("|")
            val openingInfo = lines[0].split("|")
            val openingName = openingInfo[0]
            val team = Team.fromString(openingInfo[1])
//            val variationContent = content.substring(firstLineSeparatorIndex + 1)
//            val variationContent = content
            val variationContent = lines.subList(1, lines.size).joinToString("\n")
            val variationsStrings = variationContent.split("*")

            val variations = ArrayList<Variation>()
            for (variationString in variationsStrings) {
                variations += Variation.fromString(variationString)
            }

            return Opening(openingName, team, variations)
        }

        override fun createFromParcel(parcel: Parcel): Opening {
            return Opening(parcel)
        }

        override fun newArray(size: Int): Array<Opening?> {
            return arrayOfNulls(size)
        }
    }

}