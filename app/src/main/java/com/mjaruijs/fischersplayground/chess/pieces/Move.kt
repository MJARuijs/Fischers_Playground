package com.mjaruijs.fischersplayground.chess.pieces

import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager

class Move(val team: Team, private val fromPosition: Vector2, private val toPosition: Vector2, var movedPiece: PieceType, val isCheckMate: Boolean, val isCheck: Boolean, val pieceTaken: PieceType? = null, private val takenPiecePosition: Vector2?, val promotedPiece: PieceType?) : Parcelable {

    constructor(parcel: Parcel) : this(
        Team.fromString(parcel.readString()!!),
        Vector2.fromString(parcel.readString()!!),
        Vector2.fromString(parcel.readString()!!),
        PieceType.getBySign(parcel.readString()!!),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        PieceType.getBySign(parcel.readByte().toInt().toChar()),
        Vector2.fromString(parcel.readString()!!),
        PieceType.getBySign(parcel.readByte().toInt().toChar())
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(team.toString())
        parcel.writeString(fromPosition.toString())
        parcel.writeString(toPosition.toString())
        parcel.writeString(movedPiece.sign.toString())
        parcel.writeByte(if (isCheckMate) 1 else 0)
        parcel.writeByte(if (isCheck) 1 else 0)
        parcel.writeString(pieceTaken?.sign.toString())
        parcel.writeString(takenPiecePosition.toString())
        parcel.writeString(promotedPiece?.sign.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getFromPosition(perspectiveOf: Team): Vector2 {
        return if (perspectiveOf == Team.WHITE) fromPosition else Vector2(7, 7) - fromPosition
    }

    fun getToPosition(perspectiveOf: Team): Vector2 {
        return if (perspectiveOf == Team.WHITE) toPosition else Vector2(7, 7) - toPosition
    }

    fun getTakenPosition(perspectiveOf: Team): Vector2? {
        if (takenPiecePosition == null) return null
        return if (perspectiveOf == Team.WHITE) takenPiecePosition else Vector2(7, 7) - takenPiecePosition
    }

    fun getSimpleChessNotation(): String {
        var notation = ""
        val movedPieceSign = if (team == Team.WHITE) movedPiece.sign.uppercase() else movedPiece.sign.lowercase()
        notation += movedPieceSign

        if (pieceTaken != null) {
            notation += "x"
        }

        notation += getColSign(toPosition)
        notation += getRowSign(toPosition)

        if (isCheckMate) {
            notation += "#"
        } else if (isCheck) {
            notation += "+"
        }

        return notation
    }

    fun toChessNotation(): String {
        var notation = ""
        val movedPieceSign = if (team == Team.WHITE) movedPiece.sign.uppercase() else movedPiece.sign.lowercase()

        notation += movedPieceSign
        notation += getColSign(fromPosition)
        notation += getRowSign(fromPosition)
        notation += if (pieceTaken == null) "-" else "x"

        if (pieceTaken != null && takenPiecePosition != null) {
            val takenPieceSign = if (team == Team.BLACK) pieceTaken.sign.uppercase() else pieceTaken.sign.lowercase()
            notation += takenPieceSign
            notation += getColSign(takenPiecePosition)
            notation += getRowSign(takenPiecePosition)
        }

        notation += getColSign(toPosition)
        notation += getRowSign(toPosition)

        if (promotedPiece != null) {
            notation += promotedPiece.sign
        }


        if (isCheckMate) {
            notation += "#"
        } else if (isCheck) {
            notation += "+"
        }

        return notation
    }

    private fun getRowSign(square: Vector2): String {
        return (square.y.toInt() + 1).toString()
    }

    private fun getColSign(square: Vector2): String {
        return when(square.x.toInt()) {
            0 -> "a"
            1 -> "b"
            2 -> "c"
            3 -> "d"
            4 -> "e"
            5 -> "f"
            6 -> "g"
            7 -> "h"
            else -> throw IllegalArgumentException("Couldn't parse square to chess notation: $square")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        if (this === other) {
            return true
        }

        if (other !is Move) {
            return false
        }

        if (team != other.team) {
            return false
        }

        if (fromPosition != other.fromPosition) {
            return false
        }

        if (toPosition != other.toPosition) {
            return false
        }

        if (movedPiece != other.movedPiece) {
            return false
        }

        if (isCheck != other.isCheck) {
            return false
        }

        if (isCheckMate != other.isCheckMate) {
            return false
        }

        if (pieceTaken != other.pieceTaken) {
            return false
        }

        if (takenPiecePosition != other.takenPiecePosition) {
            return false
        }

        if (promotedPiece != other.promotedPiece) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = team.hashCode()
        result = 31 * result + fromPosition.hashCode()
        result = 31 * result + toPosition.hashCode()
        result = 31 * result + movedPiece.hashCode()
        result = 31 * result + isCheckMate.hashCode()
        result = 31 * result + isCheck.hashCode()
        result = 31 * result + (pieceTaken?.hashCode() ?: 0)
        result = 31 * result + (takenPiecePosition?.hashCode() ?: 0)
        result = 31 * result + (promotedPiece?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<Move> {

        fun fromChessNotation(moveContent: String): Move {
            if (moveContent.length < 6) {
                throw IllegalArgumentException("Received notation is too short to be a proper Chess notation: $moveContent")
            }

            return try {
                var i = 0
                val movedPieceSign = moveContent[i++]
                val movedPiece = PieceType.getBySign(movedPieceSign)
                val team = if (movedPieceSign.isUpperCase()) Team.WHITE else Team.BLACK

                val fromCol = moveContent[i++]
                val fromRow = moveContent[i++]

                val fromX = colToNumber(fromCol)
                val fromY = fromRow.toString().toInt() - 1

                val moveType = moveContent[i++]
                val takenPieceSign: Char
                var takenPiece: PieceType? = null

                var takenPiecePosition: Vector2? = null

                if (moveType == 'x') {
                    takenPieceSign = moveContent[i++]
                    takenPiece = PieceType.getBySign(takenPieceSign)
                    val takenPieceCol = moveContent[i++]
                    val takenPieceRow = moveContent[i++]
                    takenPiecePosition = Vector2(colToNumber(takenPieceCol), takenPieceRow.toString().toInt() - 1)
                }

                val toCol = moveContent[i++]
                val toRow = moveContent[i++]

                val toX = colToNumber(toCol)
                val toY = toRow.toString().toInt() - 1

                var promotedPiece: PieceType? = null

                if (movedPiece == PieceType.PAWN && ((team == Team.WHITE && toY == 7) || (team == Team.BLACK && toY == 0))) {
                    val promotedPieceSign = moveContent[i]
                    promotedPiece = PieceType.getBySign(promotedPieceSign)
                }

                var isCheckMate = false
                var isCheck = false

                val lastCharacter = moveContent.last()
                if (lastCharacter == '+') {
                    isCheck = true
                } else if (lastCharacter == '#') {
                    isCheckMate = true
                }

                Move(team, Vector2(fromX, fromY), Vector2(toX, toY), movedPiece, isCheckMate, isCheck, takenPiece, takenPiecePosition, promotedPiece)
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("move_from_chess_notation_crash.txt", e.stackTraceToString())
                throw IllegalArgumentException("Failed to parse move from: $moveContent")
            }
        }

        private fun colToNumber(col: Char): Int {
            return when (col) {
                'a' -> 0
                'b' -> 1
                'c' -> 2
                'd' -> 3
                'e' -> 4
                'f' -> 5
                'g' -> 6
                'h' -> 7
                else -> throw IllegalArgumentException("Couldn't make a square from chess notation: $col")
            }
        }

        override fun createFromParcel(parcel: Parcel): Move {
            return Move(parcel)
        }

        override fun newArray(size: Int): Array<Move?> {
            return arrayOfNulls(size)
        }
    }
}