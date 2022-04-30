package com.mjaruijs.fischersplayground.chess.pieces

import com.mjaruijs.fischersplayground.math.vectors.Vector2
import kotlin.math.roundToInt

enum class PieceType(val value: Int, val sign: Char) {

    PAWN(1, 'P'),
    KNIGHT(3, 'N'),
    BISHOP(3, 'B'),
    ROOK(5, 'R'),
    QUEEN(9, 'Q'),
    KING(900, 'K');

    companion object {

        fun getBySign(sign: Char): PieceType {
            for (piece in values()) {
                if (sign.uppercase() == piece.sign.uppercase()) {
                    return piece
                }
            }

            throw IllegalArgumentException("Could not find a PieceType with sign: $sign")
        }

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

            possibleMoves += getStraightMoves(piece, square, gameState)
            possibleMoves += getDiagonalMoves(piece, square, gameState)

            return possibleMoves
        }

        private fun getPossibleMovesForRook(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            return getStraightMoves(piece, square, gameState)
        }

        private fun getPossibleMovesForBishop(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            return getDiagonalMoves(piece, square, gameState)
        }

        private fun getPossibleMovesForKnight(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            val x = square.x.roundToInt()
            val y = square.y.roundToInt()

            val positions = ArrayList<Vector2>()
            positions += Vector2(x + 1, y + 2)
            positions += Vector2(x + 2, y + 1)
            positions += Vector2(x - 1, y + 2)
            positions += Vector2(x - 2, y + 1)
            positions += Vector2(x - 1, y - 2)
            positions += Vector2(x - 2, y - 1)
            positions += Vector2(x + 1, y - 2)
            positions += Vector2(x + 2, y - 1)

            for (position in positions) {
                if (position.x < 0 || position.x > 7 || position.y < 0 || position.y > 7) {
                    continue
                }

                val pieceAtPosition = gameState[position.x.roundToInt()][position.y.roundToInt()]

                if (pieceAtPosition == null || pieceAtPosition.team != piece.team) {
                    possibleMoves += position
                }
            }

            return possibleMoves
        }

        private fun getPossibleMovesForPawn(piece: Piece, square: Vector2, gameState: ArrayList<ArrayList<Piece?>>): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            val x = square.x.roundToInt()
            val y = square.y.roundToInt()

            val direction = if (piece.team == Team.WHITE) {
                1
            } else {
                1
            }

            var firstSquareEmpty = false
            if (gameState[x][y + direction] == null) {
                possibleMoves += Vector2(x, y + direction)
                firstSquareEmpty = true
            }

            val pawnAtStartingSquare = (y == 1 )
//            val pawnAtStartingSquare = (y == 1 && piece.team == Team.WHITE) || (y == 6 && piece.team == Team.BLACK)

            if (pawnAtStartingSquare && firstSquareEmpty) {
                if (gameState[x][y + direction * 2] == null) {
                    possibleMoves += Vector2(x, y + direction * 2)
                }
            }

            if (x != 0) {
                if (gameState[x - 1][y + direction] != null && gameState[x - 1][y + direction]?.team != piece.team) {
                    possibleMoves += Vector2(x - 1, y + direction)
                }
            }

            if (x != 7) {
                if (gameState[x + 1][y + direction] != null && gameState[x + 1][y + direction]?.team != piece.team) {
                    possibleMoves += Vector2(x + 1, y + direction)
                }
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

            for (i in 1 until 8) {
                val x = square.x.roundToInt() - i
                val y = square.y.roundToInt() + i

                if (x < 0 || x > 7 || y < 0 || y > 7) {
                    break
                }

                val pieceAtSquare = gameState[x][y]

                if (pieceAtSquare == null) {
                    possibleMoves += Vector2(x, y)
                } else if (pieceAtSquare.team != piece.team) {
                    possibleMoves += Vector2(x, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until 8) {
                val x = square.x.roundToInt() + i
                val y = square.y.roundToInt() + i

                if (x < 0 || x > 7 || y < 0 || y > 7) {
                    break
                }

                val pieceAtSquare = gameState[x][y]

                if (pieceAtSquare == null) {
                    possibleMoves += Vector2(x, y)
                } else if (pieceAtSquare.team != piece.team) {
                    possibleMoves += Vector2(x, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until 8) {
                val x = square.x.roundToInt() - i
                val y = square.y.roundToInt() - i

                if (x < 0 || x > 7 || y < 0 || y > 7) {
                    break
                }

                val pieceAtSquare = gameState[x][y]

                if (pieceAtSquare == null) {
                    possibleMoves += Vector2(x, y)
                } else if (pieceAtSquare.team != piece.team) {
                    possibleMoves += Vector2(x, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until 8) {
                val x = square.x.roundToInt() + i
                val y = square.y.roundToInt() - i

                if (x < 0 || x > 7 || y < 0 || y > 7) {
                    break
                }

                val pieceAtSquare = gameState[x][y]

                if (pieceAtSquare == null) {
                    possibleMoves += Vector2(x, y)
                } else if (pieceAtSquare.team != piece.team) {
                    possibleMoves += Vector2(x, y)
                    break
                } else {
                    break
                }
            }

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