package com.mjaruijs.fischersplayground.activities.opening

import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.pieces.Team
import java.util.LinkedList

class PracticeSession(val openingName: String, val team: Team, val practiceArrows: Boolean, val currentLineIndex: Int, val totalLineCount: Int, val currentLine: OpeningLine?, val nextLine: OpeningLine?, val lines: LinkedList<OpeningLine> = LinkedList()) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        Team.fromString(parcel.readString()!!),
        parcel.readBoolean(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readParcelable(OpeningLine::class.java.classLoader),
        parcel.readParcelable(OpeningLine::class.java.classLoader)
    ) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            parcel.readList(lines, OpeningLine::class.java.classLoader)
        } else {
            parcel.readList(lines, OpeningLine::class.java.classLoader, OpeningLine::class.java)
        }
    }

    override fun toString(): String {
        var content = "$openingName^$team^$practiceArrows^$currentLineIndex^$totalLineCount^$currentLine^$nextLine\n"

        for (line in lines) {
            content += "$line\n"
        }

        return content
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(openingName)
        parcel.writeString(team.toString())
        parcel.writeBoolean(practiceArrows)
        parcel.writeInt(currentLineIndex)
        parcel.writeInt(totalLineCount)
        parcel.writeParcelable(currentLine, flags)
        parcel.writeParcelable(nextLine, flags)
        parcel.writeList(lines)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PracticeSession> {
        override fun createFromParcel(parcel: Parcel): PracticeSession {
            return PracticeSession(parcel)
        }

        override fun newArray(size: Int): Array<PracticeSession?> {
            return arrayOfNulls(size)
        }

        fun fromString(content: String): PracticeSession {
            val fileLines = content.split("\n")

            var dataIndex = 0

            val data = fileLines[0].split("^")
            val openingName = data[dataIndex++]
            val team = Team.fromString(data[dataIndex++])
            val practiceArrows = data[dataIndex++].toBoolean()
            val currentLineIndex = data[dataIndex++].toInt()
            val maxLineIndex = data[dataIndex++].toInt()
            val currentLine = OpeningLine.fromString(data[dataIndex++])
            val nextLineString = data[dataIndex]

            if (nextLineString == "null") {
                return PracticeSession(openingName, team, practiceArrows, currentLineIndex, maxLineIndex, currentLine, null, LinkedList())
            }

            val nextLine = OpeningLine.fromString(nextLineString)

            val lines = LinkedList<OpeningLine>()
            for (i in 1 until fileLines.size) {
                val fileLine = fileLines[i]
                if (fileLine.isNotBlank()) {
                    lines += OpeningLine.fromString(fileLine)
                }
            }

            return PracticeSession(openingName, team, practiceArrows, currentLineIndex, maxLineIndex, currentLine, nextLine, lines)
        }
    }

}