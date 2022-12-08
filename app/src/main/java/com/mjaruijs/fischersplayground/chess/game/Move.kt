package com.mjaruijs.fischersplayground.chess.game

import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.util.Logger

class Move(val team: Team, private val fromPosition: Vector2, private val toPosition: Vector2, var movedPiece: PieceType, val isCheckMate: Boolean, val isCheck: Boolean, val pieceTaken: PieceType? = null, private val takenPiecePosition: Vector2?, val promotedPiece: PieceType?, val arrows: ArrayList<MoveArrow> = ArrayList()) : Parcelable {

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

        val fromPositionColSign = getColSign(fromPosition)
        val toPositionColSign = getColSign(toPosition)

        val movedPieceSign = if (team == Team.WHITE) movedPiece.sign.uppercase() else movedPiece.sign.lowercase()
        notation += movedPieceSign

        if (movedPiece == PieceType.KING && fromPositionColSign == "e" && toPositionColSign == "g") {
            notation += "0-0"
        } else if (movedPiece == PieceType.KING && fromPositionColSign == "e" && toPositionColSign == "c") {
            notation += "0-0-0"
        } else {
            if (pieceTaken != null) {
                notation += "x"
            }

            notation += toPositionColSign
            notation += getRowSign(toPosition)
        }

        if (isCheckMate) {
            notation += "#"
        } else if (isCheck) {
            notation += "+"
        }

        return notation
    }

    fun toChessNotation(): String {
        var notation = ""

        val fromPositionColSign = getColSign(fromPosition)
        val toPositionColSign = getColSign(toPosition)

        val movedPieceSign = if (team == Team.WHITE) movedPiece.sign.uppercase() else movedPiece.sign.lowercase()
        notation += movedPieceSign

        if (movedPiece == PieceType.KING && fromPositionColSign == "e" && toPositionColSign == "g") {
            notation += "0-0"
        } else if (movedPiece == PieceType.KING && fromPositionColSign == "e" && toPositionColSign == "c") {
            notation += "0-0-0"
        } else {
            notation += fromPositionColSign
            notation += getRowSign(fromPosition)
            notation += if (pieceTaken == null) "-" else "x"

            if (pieceTaken != null && takenPiecePosition != null) {
                val takenPieceSign = if (team == Team.BLACK) pieceTaken.sign.uppercase() else pieceTaken.sign.lowercase()
                notation += takenPieceSign
                notation += getColSign(takenPiecePosition)
                notation += getRowSign(takenPiecePosition)
            }

            notation += toPositionColSign
            notation += getRowSign(toPosition)

            if (promotedPiece != null) {
                notation += promotedPiece.sign
            }
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
            Logger.debug(TAG, "Comparison failed on null check")
            return false
        }

        if (this === other) {
            return true
        }

        if (other !is Move) {
            Logger.debug(TAG, "Comparison failed on type check")
            return false
        }

        if (team != other.team) {
            Logger.debug(TAG, "Comparison failed on team check")
            return false
        }

        if (fromPosition != other.fromPosition) {
            Logger.debug(TAG, "Comparison failed on fromPosition check")
            return false
        }

        if (toPosition != other.toPosition) {
            Logger.debug(TAG, "Comparison failed on toPosition check")
            return false
        }

        if (movedPiece != other.movedPiece) {
            Logger.debug(TAG, "Comparison failed on movedPiece check")
            return false
        }

        if (isCheck != other.isCheck) {
            Logger.debug(TAG, "Comparison failed on isCheck check")
            return false
        }

        if (isCheckMate != other.isCheckMate) {
            Logger.debug(TAG, "Comparison failed on isCheckMate check")
            return false
        }

        if (pieceTaken != other.pieceTaken) {
            Logger.debug(TAG, "Comparison failed on pieceTaken check")
            return false
        }

        if (takenPiecePosition != other.takenPiecePosition) {
            Logger.debug(TAG, "Comparison failed on takenPiecePosition check")
            return false
        }

        if (promotedPiece != other.promotedPiece) {
            Logger.debug(TAG, "Comparison failed on promotedPiece check")
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

        private const val TAG = "Move"

        fun fromChessNotation(moveContent: String): Move {
            return try {
                var i = 0
                val movedPieceSign = moveContent[i++]
                val movedPiece = PieceType.getBySign(movedPieceSign)
                val team = if (movedPieceSign.isUpperCase()) Team.WHITE else Team.BLACK

                val fromX: Int
                val fromY: Int
                val toX: Int
                val toY: Int

                var promotedPiece: PieceType? = null
                var takenPiece: PieceType? = null
                var takenPiecePosition: Vector2? = null

                if (movedPiece == PieceType.KING && moveContent[1] == '0') {
                    if (team == Team.WHITE) {
                        fromY = 0
                        toY = 0
                    } else {
                        fromY = 7
                        toY = 7
                    }

                    fromX = colToNumber('e')

                    val numberOfZeros = moveContent.count { c -> c == '0' }
                    toX = when (numberOfZeros) {
                        2 -> colToNumber('g')
                        3 -> colToNumber('c')
                        else -> throw IllegalArgumentException("Failed to parse into valid chess notation: $moveContent")
                    }
                } else {
                    val fromCol = moveContent[i++]
                    val fromRow = moveContent[i++]

                    fromX = colToNumber(fromCol)
                    fromY = fromRow.toString().toInt() - 1

                    val moveType = moveContent[i++]
                    val takenPieceSign: Char

                    if (moveType == 'x') {
                        takenPieceSign = moveContent[i++]
                        takenPiece = PieceType.getBySign(takenPieceSign)
                        val takenPieceCol = moveContent[i++]
                        val takenPieceRow = moveContent[i++]
                        takenPiecePosition = Vector2(colToNumber(takenPieceCol), takenPieceRow.toString().toInt() - 1)
                    }

                    val toCol = moveContent[i++]
                    val toRow = moveContent[i++]

                    toX = colToNumber(toCol)
                    toY = toRow.toString().toInt() - 1

                    if (movedPiece == PieceType.PAWN && ((team == Team.WHITE && toY == 7) || (team == Team.BLACK && toY == 0))) {
                        val promotedPieceSign = moveContent[i]
                        promotedPiece = PieceType.getBySign(promotedPieceSign)
                    }
                }

                var isCheckMate = false
                var isCheck = false

                val lastCharacter = moveContent.trim().last()
                if (lastCharacter == '+') {
                    isCheck = true
                } else if (lastCharacter == '#') {
                    isCheck = true
                    isCheckMate = true
                }

                val move = Move(team, Vector2(fromX, fromY), Vector2(toX, toY), movedPiece, isCheckMate, isCheck, takenPiece, takenPiecePosition, promotedPiece)

                Logger.debug(TAG, "Parsed ${move.toChessNotation()} from $moveContent")

                return move
            } catch (e: Exception) {
                NetworkManager.getInstance().sendCrashReport("crash_move_from_chess_notation.txt", e.stackTraceToString())
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