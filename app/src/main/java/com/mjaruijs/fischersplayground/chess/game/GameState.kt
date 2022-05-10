package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import kotlin.math.roundToInt

class GameState(private val isPlayingWhite: Boolean, internal val state: ArrayList<ArrayList<Piece?>> = ArrayList()) {

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

            state[0][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)
//            state[1][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
//            state[2][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
//            state[3 + queenOffset][whiteIndex] = Piece(PieceType.QUEEN, Team.WHITE)
            state[4 - queenOffset][whiteIndex] = Piece(PieceType.KING, Team.WHITE)
//            state[5][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
//            state[6][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
            state[7][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)

            state[0][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)
//            state[1][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
//            state[2][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
//            state[3 + queenOffset][blackIndex] = Piece(PieceType.QUEEN, Team.BLACK)
            state[4 - queenOffset][blackIndex] = Piece(PieceType.KING, Team.BLACK)
//            state[5][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
//            state[6][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
            state[7][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)

//            state[4][6] = Piece(PieceType.PAWN, Team.BLACK)
//            state[5][6] = Piece(PieceType.PAWN, Team.BLACK)

            for (i in 0 until 8) {
                state[i][whitePawnIndex] = Piece(PieceType.PAWN, Team.WHITE)
                state[i][blackPawnIndex] = Piece(PieceType.PAWN, Team.BLACK)
            }
        }
    }

    operator fun get(i: Int, j: Int) = state[i][j]

    operator fun get(i: Float, j: Float) = state[i.roundToInt()][j.roundToInt()]

    operator fun get(vector2: Vector2) = state[vector2.x.roundToInt()][vector2.y.roundToInt()]

    operator fun set(vector2: Vector2, piece: Piece?) {
        state[vector2.x.roundToInt()][vector2.y.roundToInt()] = piece
    }

    fun copy(): GameState {
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

        return GameState(isPlayingWhite, copiedState)
    }
}