package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.util.Logger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class GameState(private val isPlayingWhite: Boolean, private val pieces: ArrayList<Piece> = ArrayList()) {

    private val pieceLock = AtomicBoolean(false)

    init {
        if (pieces.isEmpty()) {
            loadStartingState()
        }
    }

    private fun loadStartingState() {
        val queenOffset = if (isPlayingWhite) 0 else 1
        val whiteIndex = if (isPlayingWhite) 0 else 7
        val blackIndex = if (isPlayingWhite) 7 else 0
        val whitePawnIndex = if (isPlayingWhite) 1 else 6
        val blackPawnIndex = if (isPlayingWhite) 6 else 1

        acquirePieceLock()
        add(Piece(PieceType.ROOK, Team.WHITE, Vector2(0, whiteIndex)))
        add(Piece(PieceType.KNIGHT, Team.WHITE, Vector2(1, whiteIndex)))
        add(Piece(PieceType.BISHOP, Team.WHITE, Vector2(2, whiteIndex)))
        add(Piece(PieceType.QUEEN, Team.WHITE, Vector2(3 + queenOffset, whiteIndex)))
        add(Piece(PieceType.KING, Team.WHITE, Vector2(4 - queenOffset, whiteIndex)))
        add(Piece(PieceType.BISHOP, Team.WHITE, Vector2(5, whiteIndex)))
        add(Piece(PieceType.KNIGHT, Team.WHITE, Vector2(6, whiteIndex)))
        add(Piece(PieceType.ROOK, Team.WHITE, Vector2(7, whiteIndex)))

        for (i in 0 until 8) {
            add(Piece(PieceType.PAWN, Team.WHITE, Vector2(i, whitePawnIndex)))
        }

        add(Piece(PieceType.ROOK, Team.BLACK, Vector2(0, blackIndex)))
        add(Piece(PieceType.KNIGHT, Team.BLACK, Vector2(1, blackIndex)))
        add(Piece(PieceType.BISHOP, Team.BLACK, Vector2(2, blackIndex)))
        add(Piece(PieceType.QUEEN, Team.BLACK, Vector2(3 + queenOffset, blackIndex)))
        add(Piece(PieceType.KING, Team.BLACK, Vector2(4 - queenOffset, blackIndex)))
        add(Piece(PieceType.BISHOP, Team.BLACK, Vector2(5, blackIndex)))
        add(Piece(PieceType.KNIGHT, Team.BLACK, Vector2(6, blackIndex)))
        add(Piece(PieceType.ROOK, Team.BLACK, Vector2(7, blackIndex)))

        for (i in 0 until 8) {
            add(Piece(PieceType.PAWN, Team.BLACK, Vector2(i, blackPawnIndex)))
        }
        releasePieceLock()
//        for (row in 0 until 8) {
//            state += ArrayList<Piece?>()
//
//            for (col in 0 until 8) {
//                state[row] += null
//            }
//        }




//        state[0][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)
//        state[1][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
//        state[2][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
//        state[3 + queenOffset][whiteIndex] = Piece(PieceType.QUEEN, Team.WHITE)
//        state[4 - queenOffset][whiteIndex] = Piece(PieceType.KING, Team.WHITE)
//        state[5][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
//        state[6][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
//        state[7][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)
//
//        state[0][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)
//        state[1][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
//        state[2][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
//        state[3 + queenOffset][blackIndex] = Piece(PieceType.QUEEN, Team.BLACK)
//        state[4 - queenOffset][blackIndex] = Piece(PieceType.KING, Team.BLACK)
//        state[5][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
//        state[6][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
//        state[7][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)
//
//        for (i in 0 until 8) {
//            state[i][whitePawnIndex] = Piece(PieceType.PAWN, Team.WHITE)
//            state[i][blackPawnIndex] = Piece(PieceType.PAWN, Team.BLACK)
//        }
    }

    fun getPieces(): ArrayList<Piece> {
//        val piecesCopy = ArrayList<Piece>()
//        for (piece in pieces) {
//            piecesCopy += Piece(piece.type, piece.team, piece.square, piece.translation)
//        }
        waitForPieceLock()
        return pieces
    }

    private fun clearPieces() {
        acquirePieceLock()
        pieces.clear()
        releasePieceLock()
    }

    fun reset() {
//        state.clear()
        clearPieces()
        loadStartingState()
    }

    operator fun get(i: Int, j: Int): Piece? {
        return try {
            acquirePieceLock()
            for (piece in pieces) {
                if (piece.square.x.roundToInt() == i && piece.square.y.roundToInt() == j) {
                    releasePieceLock()
                    return piece
                }
            }
            releasePieceLock()
            return null
//            state[i][j]
        } catch (e: IndexOutOfBoundsException) {
            releasePieceLock()
//            ErrorHandler.reportCrash(e)
            return null
        }
    }

    private fun acquirePieceLock() {
        waitForPieceLock()
        pieceLock.set(true)
    }

    private fun waitForPieceLock() {
//        while (pieceLock.get()) {
//            Thread.sleep(10)
//        }
    }

    private fun releasePieceLock() {
        pieceLock.set(false)
    }

//    operator fun get(i: Float, j: Float) = state[i.roundToInt()][j.roundToInt()]

    operator fun get(vector2: Vector2): Piece? {
        return get(vector2.x.roundToInt(), vector2.y.roundToInt())
    }

    operator fun plusAssign(piece: Piece) {
        add(piece)
    }

    fun add(piece: Piece) {
        acquirePieceLock()
        pieces += piece
        releasePieceLock()
    }

    fun remove(piece: Piece) {
        acquirePieceLock()
        pieces.remove(piece)
        releasePieceLock()
    }

    fun replaceAt(square: Vector2, piece: Piece) {
        removeAt(square)
        add(piece)
    }

    fun movePieceTo(fromPosition: Vector2, toPosition: Vector2) {
        val piece = get(fromPosition) ?: return
        Logger.debug(TAG, "Moving piece from $fromPosition to $toPosition")
        piece.moveTo(toPosition)
    }

    fun removeAt(file: Int, rank: Int) {
        acquirePieceLock()
        pieces.removeIf { piece ->
            piece.square.x.roundToInt() == file && piece.square.y.roundToInt() == rank
        }
        releasePieceLock()
    }

    fun removeAt(square: Vector2, team: Team) {
        acquirePieceLock()
        pieces.removeIf { piece ->
            piece.getFile() == square.x.roundToInt() && piece.getRank() == square.y.roundToInt() && piece.team == team
        }
        releasePieceLock()
    }

    fun removeAt(square: Vector2) {
        removeAt(square.x.roundToInt(), square.y.roundToInt())
    }

//    operator fun set(vector2: Vector2, piece: Piece?) {
//       try {
//
////           state[vector2.x.roundToInt()][vector2.y.roundToInt()] = piece
//       } catch (e: IndexOutOfBoundsException) {
////           ErrorHandler.reportCrash(e)
//       }
//    }

    fun copy(): GameState {
        val copiedState = ArrayList<Piece>()

//        for (row in 0 until 8) {
//            copiedState += ArrayList<Piece?>()
//
//            for (col in 0 until 8) {
//                copiedState[row] += null
//            }
//        }

//        for (x in 0 until 8) {
//            for (y in 0 until 8) {
//                copiedState[x][y] = state[x][y]
//            }
//        }
        acquirePieceLock()
        for (piece in pieces) {
            copiedState += piece.copy()
        }
        releasePieceLock()

        return GameState(isPlayingWhite, copiedState)
    }

    override fun toString(): String {
        var string = "\n"

        for (rank in 0 until 8) {
            for (file in 0 until 8) {
                val piece = get(file, 7 - rank)

                string += if (piece == null) {
                    "_;_"
                } else {
                    val type = piece.type
                    val team = piece.team
                    "${type.sign};${team.toString()[0]}"
                }

                string += " "
            }
            string += "\n"
        }

        return string
    }

    companion object {

        private const val TAG = "GameState"
//        fun fromString(content: String): GameState {
//            val data = content.split("|")
//            val isPlayingWhite = data[0].toBoolean()
//
//            val movesData = data[1].split(",")
//
//            val moves = ArrayList<ArrayList<Piece?>>()
//
//            for (row in 0 until 8) {
//                moves += ArrayList<Piece?>()
//
//                for (col in 0 until 8) {
//                    moves[row] += null
//                }
//            }
//
//            var moveIndex = 0
//            movesData.forEach {
//                val rowIndex = moveIndex % 8
//                val colIndex = moveIndex / 8
//
//                moves[colIndex][rowIndex] = if (it == "null") {
//                    null
//                } else {
//                    val moveData = it.split(";")
//                    val pieceType = PieceType.fromString(moveData[0])
//                    val team = Team.fromString(moveData[1])
//                    Piece(pieceType, team)
//                }
//
//                moveIndex++
//            }
//
//            return GameState(isPlayingWhite, moves)
//        }
    }
}