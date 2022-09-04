package com.mjaruijs.fischersplayground.chess.pieces

import com.mjaruijs.fischersplayground.math.vectors.Vector2

class Move(val team: Team, private val fromPosition: Vector2, private val toPosition: Vector2, var movedPiece: PieceType, private val isCheckMate: Boolean, private val isCheck: Boolean, val pieceTaken: PieceType? = null, val promotedPiece: PieceType?) {

    fun getFromPosition(perspectiveOf: Team): Vector2 {
        return if (perspectiveOf == Team.WHITE) fromPosition else Vector2(7, 7) - fromPosition
    }

    fun getToPosition(perspectiveOf: Team): Vector2 {
        return if (perspectiveOf == Team.WHITE) toPosition else Vector2(7, 7) - toPosition
    }

    fun toChessNotation(): String {
        var notation = ""
        val movedPieceSign = if (team == Team.WHITE) movedPiece.sign.uppercase() else movedPiece.sign.lowercase()

        notation += movedPieceSign
        notation += getColSign(fromPosition)
        notation += getRowSign(fromPosition)
        notation += if (pieceTaken == null) "-" else "x"

        if (pieceTaken != null) {
            val takenPieceSign = if (team == Team.BLACK) pieceTaken.sign.uppercase() else pieceTaken.sign.lowercase()
            notation += takenPieceSign
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

    companion object {

        fun fromChessNotation(moveContent: String): Move {
//            val separatorIndex = moveContent.indexOf(':')
//            val timeStamp = moveContent.substring(0, separatorIndex).toLong()
//            val notation = moveContent.substring(separatorIndex + 1)

            if (moveContent.length < 6) {
                throw IllegalArgumentException("Received notation is too short to be a proper Chess notation: $moveContent")
            }

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

            if (moveType == 'x') {
                takenPieceSign = moveContent[i++]
                takenPiece = PieceType.getBySign(takenPieceSign)
            }

            val toCol = moveContent[i++]
            val toRow = moveContent[i++]

            val toX = colToNumber(toCol)
            val toY = toRow.toString().toInt() - 1

            var promotedPiece: PieceType? = null

            if (movedPiece == PieceType.PAWN && ((team == Team.WHITE && toY == 7) || (team == Team.BLACK && toY == 0))) {
                val promotedPieceSign = moveContent[i++]
                promotedPiece = PieceType.getBySign(promotedPieceSign)
            }

            var isCheckMate = false
            var isCheck = false

            if (moveContent.length == i - 1) {
                val kingSituation = moveContent[i]
                if (kingSituation == '+') {
                    isCheck = true
                }
                if (kingSituation == '#') {
                    isCheckMate = true
                }
            }

            return Move(team, Vector2(fromX, fromY), Vector2(toX, toY), movedPiece, isCheckMate, isCheck, takenPiece, promotedPiece)
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
    }
}