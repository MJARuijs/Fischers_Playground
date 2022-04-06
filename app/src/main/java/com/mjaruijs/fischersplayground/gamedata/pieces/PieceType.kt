package com.mjaruijs.fischersplayground.gamedata.pieces

import com.mjaruijs.fischersplayground.math.vectors.Vector2
import kotlin.math.roundToInt

enum class PieceType(val value: Int) {

    PAWN(1),
    KNIGHT(3),
    BISHOP(3),
    ROOK(5),
    QUEEN(9),
    KING(900);

    companion object {

        fun getPossibleMoves(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            return when (piece.type) {
                KING -> getPossibleMovesForKing(piece, square, gameState)
                QUEEN -> getPossibleMovesForQueen(piece, square, gameState)
                ROOK -> getPossibleMovesForRook(piece, square, gameState)
                BISHOP -> getPossibleMovesForBishop(piece, square, gameState)
                KNIGHT -> getPossibleMovesForKnight(piece, square, gameState)
                PAWN -> getPossibleMovesForPawn(piece, square, gameState)
            }
        }

        private fun getPossibleMovesForQueen(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            return possibleMoves
        }

        private fun getPossibleMovesForRook(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            return getStraightMoves(piece, square, gameState)
        }

        private fun getPossibleMovesForBishop(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()



            return possibleMoves
        }

        private fun getPossibleMovesForKnight(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            return possibleMoves
        }

        private fun getPossibleMovesForPawn(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            val direction = if (piece.team == Team.WHITE) {
                1
            } else {
                -1
            }

            var firstSquareEmpty = false
            if (gameState[square.x.roundToInt()][square.y.roundToInt() + direction] == null) {
                possibleMoves += Vector2(square.x, square.y + direction)
                firstSquareEmpty = true
            }

            val pawnAtStartingSquare = (square.y == 1.0f && piece.team == Team.WHITE) || (square.y == 6.0f && piece.team == Team.BLACK)

            if (pawnAtStartingSquare && firstSquareEmpty) {
                if (gameState[square.x.roundToInt()][square.y.roundToInt() + direction * 2] == null) {
                    possibleMoves += Vector2(square.x, square.y + direction * 2)
                }
            }

            if (gameState[square.x.roundToInt() - 1][square.y.roundToInt() + direction] != null && gameState[square.x.roundToInt() - 1][square.y.roundToInt() + direction]?.team != piece.team) {
                possibleMoves += Vector2(square.x - 1, square.y + direction)
            }

            if (gameState[square.x.roundToInt() + 1][square.y.roundToInt() + direction] != null && gameState[square.x.roundToInt() + 1][square.y.roundToInt() + direction]?.team != piece.team) {
                possibleMoves += Vector2(square.x + 1, square.y + direction)
            }

            return possibleMoves
        }

        private fun getPossibleMovesForKing(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            for (i in -1 .. 1) {
                for (j in -1 .. 1) {
                    if (i == 0 && j == 0) {
                        continue
                    }

                    val x = square.x.roundToInt() + i
                    val y = square.y.roundToInt() + j

                    if (x < 0 || x > 7) {
                        continue
                    }

                    if (y < 0 || y > 7) {
                        continue
                    }

                    if (gameState[x][y] == null || gameState[x][y]?.team != piece.team) {
                        possibleMoves += square + Vector2(i, j)
                    }
                }
            }

            return possibleMoves
        }

        private fun getDiagonalMoves(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            return possibleMoves
        }

        private fun getStraightMoves(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            val x = square.x.roundToInt()
            val y = square.y.roundToInt()

            val maxX = 8 - x
            val maxY = 8 - y

            for (i in 1 .. x) {
                if (gameState[x - i][y] == null) {
                    possibleMoves += Vector2(x - i, y)
                } else if (gameState[x - i][y]?.team != piece.team) {
                    possibleMoves += Vector2(x - i, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until maxX) {
                if (gameState[x + i][y] == null) {
                    possibleMoves += Vector2(x + i, y)
                } else if (gameState[x + i][y]?.team != piece.team) {
                    possibleMoves += Vector2(x + i, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 .. y) {
                if (gameState[x][y - i] == null) {
                    possibleMoves += Vector2(x, y - i)
                } else if (gameState[x][y - i]?.team != piece.team) {
                    possibleMoves += Vector2(x, y - i)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until maxY) {
                if (gameState[x][y + i] == null) {
                    possibleMoves += Vector2(x, y + i)
                } else if (gameState[x][y + i]?.team != piece.team) {
                    possibleMoves += Vector2(x, y + i)
                    break
                } else {
                    break
                }
            }

            return possibleMoves
        }
    }

}