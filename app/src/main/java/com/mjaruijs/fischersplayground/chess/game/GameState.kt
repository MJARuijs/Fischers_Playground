package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import kotlin.math.roundToInt

class GameState(private val isPlayingWhite: Boolean, private val state: ArrayList<ArrayList<Piece?>> = ArrayList()) {

    init {
        if (state.isEmpty()) {
            loadStartingState()
        }
    }

    private fun loadStartingState() {
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
        state[1][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
        state[2][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
        state[3 + queenOffset][whiteIndex] = Piece(PieceType.QUEEN, Team.WHITE)
        state[4 - queenOffset][whiteIndex] = Piece(PieceType.KING, Team.WHITE)
        state[5][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
        state[6][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
        state[7][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)

        state[0][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)
        state[1][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
        state[2][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
        state[3 + queenOffset][blackIndex] = Piece(PieceType.QUEEN, Team.BLACK)
        state[4 - queenOffset][blackIndex] = Piece(PieceType.KING, Team.BLACK)
        state[5][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
        state[6][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
        state[7][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)

        for (i in 0 until 8) {
            state[i][whitePawnIndex] = Piece(PieceType.PAWN, Team.WHITE)
            state[i][blackPawnIndex] = Piece(PieceType.PAWN, Team.BLACK)
        }
    }

    fun reset() {
        state.clear()
        loadStartingState()
    }

    operator fun get(i: Int, j: Int): Piece? {
        return try {
            state[i][j]
        } catch (e: IndexOutOfBoundsException) {
//            ErrorHandler.reportCrash(e)
            NetworkManager.getInstance().sendCrashReport("crash_game_state_int_get_oob.txt", e.stackTraceToString())
            return null
        }
    }

    operator fun get(i: Float, j: Float) = state[i.roundToInt()][j.roundToInt()]

    operator fun get(vector2: Vector2): Piece? {
        return try {
            state[vector2.x.roundToInt()][vector2.y.roundToInt()]
        } catch (e: ArrayIndexOutOfBoundsException) {
            NetworkManager.getInstance().sendCrashReport("crash_game_state_vec_get_oob.txt", e.stackTraceToString())

            null
        }
    }

    operator fun set(vector2: Vector2, piece: Piece?) {
       try {
           state[vector2.x.roundToInt()][vector2.y.roundToInt()] = piece
       } catch (e: IndexOutOfBoundsException) {
           NetworkManager.getInstance().sendCrashReport("crash_game_state_set_oob.txt", e.stackTraceToString())

//           ErrorHandler.reportCrash(e)
       }
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

    override fun toString(): String {
        var string = "$isPlayingWhite|"

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                string += if (state[x][y] == null) {
                    "null"
                } else {
                    val type = state[x][y]!!.type
                    val team = state[x][y]!!.team
                    "$type;$team"
                }

                if (x != 7 || y != 7) {
                    string += ","
                }
            }
        }

        return string
    }

    companion object {

        fun fromString(content: String): GameState {
            val data = content.split("|")
            val isPlayingWhite = data[0].toBoolean()

            val movesData = data[1].split(",")

            val moves = ArrayList<ArrayList<Piece?>>()

            for (row in 0 until 8) {
                moves += ArrayList<Piece?>()

                for (col in 0 until 8) {
                    moves[row] += null
                }
            }

            var moveIndex = 0
            movesData.forEach {
                val rowIndex = moveIndex % 8
                val colIndex = moveIndex / 8

                moves[colIndex][rowIndex] = if (it == "null") {
                    null
                } else {
                    val moveData = it.split(";")
                    val pieceType = PieceType.fromString(moveData[0])
                    val team = Team.fromString(moveData[1])
                    Piece(pieceType, team)
                }

                moveIndex++
            }

            return GameState(isPlayingWhite, moves)
        }
    }
}