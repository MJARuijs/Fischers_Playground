package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import kotlin.math.roundToInt

class ArrayBasedGameState(private val isPlayingWhite: Boolean, private val state: ArrayList<ArrayList<Piece?>> = ArrayList()) {

    init {
        if (state.isEmpty()) {
            for (row in 0 until 8) {
                state += ArrayList<Piece?>()

                for (col in 0 until 8) {
                    state[row] += null
                }
            }

            val whiteIndex = if (isPlayingWhite) 0 else 7
            val blackIndex = if (isPlayingWhite) 7 else 0
            val whitePawnIndex = if (isPlayingWhite) 1 else 6
            val blackPawnIndex = if (isPlayingWhite) 6 else 1

            val queenOffset = if (isPlayingWhite) 0 else 1

            state[0][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE, Vector2(0, whiteIndex))
            state[1][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE, Vector2(1, whiteIndex))
            state[2][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE, Vector2(2, whiteIndex))
            state[3 + queenOffset][whiteIndex] = Piece(PieceType.QUEEN, Team.WHITE, Vector2(3 + queenOffset, whiteIndex))
            state[4 - queenOffset][whiteIndex] = Piece(PieceType.KING, Team.WHITE, Vector2(4 - queenOffset, whiteIndex))
            state[5][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE, Vector2(5, whiteIndex))
            state[6][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE, Vector2(6, whiteIndex))
            state[7][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE, Vector2(7, whiteIndex))

            state[0][blackIndex] = Piece(PieceType.ROOK, Team.BLACK, Vector2(0, blackIndex))
            state[1][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK, Vector2(1, blackIndex))
            state[2][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK, Vector2(2, blackIndex))
            state[3 + queenOffset][blackIndex] = Piece(PieceType.QUEEN, Team.BLACK, Vector2(3 + queenOffset, blackIndex))
            state[4 - queenOffset][blackIndex] = Piece(PieceType.KING, Team.BLACK, Vector2(4 - queenOffset, blackIndex))
            state[5][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK, Vector2(5, blackIndex))
            state[6][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK, Vector2(6, blackIndex))
            state[7][blackIndex] = Piece(PieceType.ROOK, Team.BLACK, Vector2(7, blackIndex))

            for (i in 0 until 8) {
                state[i][whitePawnIndex] = Piece(PieceType.PAWN, Team.WHITE, Vector2(i, whitePawnIndex))
                state[i][blackPawnIndex] = Piece(PieceType.PAWN, Team.BLACK, Vector2(i, blackPawnIndex))
            }
        }
    }

    operator fun get(i: Int, j: Int): Piece? {
        return try {
            state[i][j]
        } catch (e: IndexOutOfBoundsException) {
//            ErrorHandler.reportCrash(e)
            return null
        }
    }

    operator fun get(i: Float, j: Float) = state[i.roundToInt()][j.roundToInt()]

    operator fun get(vector2: Vector2): Piece? {
        return try {
            state[vector2.x.roundToInt()][vector2.y.roundToInt()]
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }
    }

    operator fun set(vector2: Vector2, piece: Piece?) {
       try {
           state[vector2.x.roundToInt()][vector2.y.roundToInt()] = piece
       } catch (e: IndexOutOfBoundsException) {
//           ErrorHandler.reportCrash(e)
       }
    }

    fun copy(): ArrayBasedGameState {
        val copiedState = ArrayList<ArrayList<Piece?>>()

        for (row in 0 until 8) {
            copiedState += ArrayList<Piece?>()

            for (col in 0 until 8) {
                copiedState[row] += null
            }
        }

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                copiedState[x][y] = state[x][y]
            }
        }

        return ArrayBasedGameState(isPlayingWhite, copiedState)
    }

    override fun toString(): String {
        var string = ""

        for (y in 0 until 8) {
            string += "["
            for (x in 0 until 8) {

                string += state[x][y]?.type

                if (x != 7) {
                    string += "\t"
                }
            }

            string += "]\n"
        }

        return string
    }
}