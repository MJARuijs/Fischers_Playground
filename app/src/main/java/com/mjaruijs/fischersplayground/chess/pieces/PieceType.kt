package com.mjaruijs.fischersplayground.chess.pieces

import com.mjaruijs.fischersplayground.chess.GameState
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import kotlin.math.abs
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

//        fun getPossibleMoves(team: Team, piece: Piece, square: Vector2, gameState: GameState, moves: ArrayList<Move>) = getPossibleMoves(team, piece, square, gameState, moves)

        fun getPossibleMoves(team: Team, piece: Piece, square: Vector2, gameState: GameState, moves: ArrayList<Move>): ArrayList<Vector2> {
            return when (piece.type) {
                KING -> getPossibleMovesForKing(piece, square, moves, gameState)
                QUEEN -> getPossibleMovesForQueen(piece, square, gameState)
                ROOK -> getPossibleMovesForRook(piece, square, gameState)
                BISHOP -> getPossibleMovesForBishop(piece, square, gameState)
                KNIGHT -> getPossibleMovesForKnight(piece, square, gameState)
                PAWN -> getPossibleMovesForPawn(team, piece, square, moves, gameState)
            }
        }

        private fun getPossibleMovesForQueen(piece: Piece, square: Vector2, gameState: GameState): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            possibleMoves += getStraightMoves(piece, square, gameState)
            possibleMoves += getDiagonalMoves(piece, square, gameState)

            return possibleMoves
        }

        private fun getPossibleMovesForRook(piece: Piece, square: Vector2, gameState: GameState): ArrayList<Vector2> {
            return getStraightMoves(piece, square, gameState)
        }

        private fun getPossibleMovesForBishop(piece: Piece, square: Vector2, gameState: GameState): ArrayList<Vector2> {
            return getDiagonalMoves(piece, square, gameState)
        }

        private fun getPossibleMovesForKnight(piece: Piece, square: Vector2,  gameState: GameState): ArrayList<Vector2> {
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

                val pieceAtPosition = gameState[position]

                if (pieceAtPosition == null || pieceAtPosition.team != piece.team) {
                    possibleMoves += position
                }
            }

            return possibleMoves
        }

        private fun getPossibleMovesForPawn(team: Team, piece: Piece, square: Vector2, moves: ArrayList<Move>, gameState: GameState): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            val x = square.x.roundToInt()
            val y = square.y.roundToInt()

            val direction = if (piece.team == team) 1 else -1

            var firstSquareEmpty = false
            if (gameState[x, y + direction] == null) {
                possibleMoves += Vector2(x, y + direction)
                firstSquareEmpty = true
            }

            val pawnAtStartingSquare = y == 1
//            val pawnAtStartingSquare = (y == 1 && piece.team == Team.WHITE) || (y == 6 && piece.team == Team.BLACK)

            if (pawnAtStartingSquare && firstSquareEmpty) {
                if (gameState[x, y + direction * 2] == null) {
                    possibleMoves += Vector2(x, y + direction * 2)
                }
            }

            if (x != 0) {
                if (gameState[x - 1, y + direction] != null && gameState[x - 1, y + direction]?.team != piece.team) {
                    possibleMoves += Vector2(x - 1, y + direction)
                }
            }

            if (x != 7) {
                if (gameState[x + 1, y + direction] != null && gameState[x + 1, y + direction]?.team != piece.team) {
                    possibleMoves += Vector2(x + 1, y + direction)
                }
            }

            val lastMove = try {
                moves.last()
            } catch (e: NoSuchElementException) {
                null
            }

            if (lastMove != null) {
                if (y == 4) {
                    if (x != 7) {
                        val pieceRightToPawn = gameState[x + 1, y]
                        if (pieceRightToPawn != null) {
                            if (pieceRightToPawn.team != piece.team && pieceRightToPawn.type == PAWN) {
                                if (lastMove.fromPosition.y.roundToInt() == 6) {
                                    possibleMoves += Vector2(x + 1, y + direction)
                                }
                            }
                        }
                    }

                    if (x != 0) {
                        val pieceLeftToPawn = gameState[x - 1, y]
                        if (pieceLeftToPawn != null) {
                            if (pieceLeftToPawn.team != piece.team && pieceLeftToPawn.type == PAWN) {
                                if (lastMove.fromPosition.y.roundToInt() == 6) {
                                    possibleMoves += Vector2(x - 1, y + direction)
                                }
                            }
                        }
                    }
                }
            }

            return possibleMoves
        }

        private fun getPossibleMovesForKing(piece: Piece, square: Vector2, moves: ArrayList<Move>, gameState: GameState): ArrayList<Vector2> {
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

                    if (gameState[x, y] == null || gameState[x, y]?.team != piece.team) {
                        possibleMoves += square + Vector2(i, j)
                    }
                }
            }

            val direction = if (piece.team == Team.WHITE) 1 else -1

            if (canShortCastle(piece.team, moves, gameState)) {
                possibleMoves += square + Vector2(2 * direction, 0)
            }

            if (canLongCastle(piece.team, moves, gameState)) {
                possibleMoves += square - Vector2(2 * direction, 0)
            }

            return possibleMoves
        }

        private fun getDiagonalMoves(piece: Piece, square: Vector2, gameState: GameState): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            for (i in 1 until 8) {
                val x = square.x.roundToInt() - i
                val y = square.y.roundToInt() + i

                if (x < 0 || x > 7 || y < 0 || y > 7) {
                    break
                }

                val pieceAtSquare = gameState[x, y]

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

                val pieceAtSquare = gameState[x, y]

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

                val pieceAtSquare = gameState[x, y]

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

                val pieceAtSquare = gameState[x, y]

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

        private fun getStraightMoves(piece: Piece, square: Vector2, gameState: GameState): ArrayList<Vector2> {
            val possibleMoves = ArrayList<Vector2>()

            val x = square.x.roundToInt()
            val y = square.y.roundToInt()

            val maxX = 8 - x
            val maxY = 8 - y

            for (i in 1 .. x) {
                if (gameState[x - i, y] == null) {
                    possibleMoves += Vector2(x - i, y)
                } else if (gameState[x - i, y]?.team != piece.team) {
                    possibleMoves += Vector2(x - i, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until maxX) {
                if (gameState[x + i, y] == null) {
                    possibleMoves += Vector2(x + i, y)
                } else if (gameState[x + i, y]?.team != piece.team) {
                    possibleMoves += Vector2(x + i, y)
                    break
                } else {
                    break
                }
            }

            for (i in 1 .. y) {
                if (gameState[x, y - i] == null) {
                    possibleMoves += Vector2(x, y - i)
                } else if (gameState[x, y - i]?.team != piece.team) {
                    possibleMoves += Vector2(x, y - i)
                    break
                } else {
                    break
                }
            }

            for (i in 1 until maxY) {
                if (gameState[x, y + i] == null) {
                    possibleMoves += Vector2(x, y + i)
                } else if (gameState[x, y + i]?.team != piece.team) {
                    possibleMoves += Vector2(x, y + i)
                    break
                } else {
                    break
                }
            }

            return possibleMoves
        }

        private fun canShortCastle(team: Team, moves: ArrayList<Move>, state: GameState): Boolean {
            val kingX = if (team == Team.WHITE) 4 else 3
            val direction = if (team == Team.WHITE) 1 else -1

            var kingMoved = false
            var rookMoved = false

            for (move in moves) {
                if (move.team == team) {
                    if (move.movedPiece == KING) {
                        kingMoved = true
                    }

                    if (move.movedPiece == ROOK) {
                        if (team == Team.WHITE && move.fromPosition == Vector2(7, 0)) {
                            rookMoved = true
                        }
                        if (team == Team.BLACK && move.fromPosition == Vector2(0, 0)) {
                            rookMoved = true
                        }
                    }
                }
            }

            if (kingMoved || rookMoved) {
                return false
            }

//            println(".")
//            println(".")

            for (i in 1 until 3) {
                val square = Vector2(kingX + i * direction, 0)

                if (state[square] != null) {
                    return false
                }
            }

            for (i in 0 until 3) {
                val square = Vector2(kingX + i * direction, 0)
                val attacked = isSquareAttacked(team, square, moves, state)

//                println("$team $square : $attacked")

                if (attacked) {
                    return false
                }
            }

            return true
        }

        private fun canLongCastle(team: Team, moves: ArrayList<Move>, state: GameState): Boolean {
            val kingX = if (team == Team.WHITE) 4 else 3
            val direction = if (team == Team.WHITE) -1 else 1

            var kingMoved = false
            var rookMoved = false

            for (move in moves) {
                if (move.team == team) {
                    if (move.movedPiece == KING) {
                        kingMoved = true
                    }

                    if (move.movedPiece == ROOK) {
                        if (team == Team.WHITE && move.fromPosition == Vector2(0, 0)) {
                            rookMoved = true
                        }
                        if (team == Team.BLACK && move.fromPosition == Vector2(7, 0)) {
                            rookMoved = true
                        }
                    }
                }
            }

            if (kingMoved || rookMoved) {
                return false
            }

            for (i in 0 until 4) {
                val square = Vector2(kingX + i * direction, 0)

                if (i != 0) {
                    if (state[square] != null) {
                        return false
                    }
                }

                if (isSquareAttacked(team, square, moves, state)) {
                    return false
                }
            }

            return true
        }

        private fun isSquareAttacked(team: Team, square: Vector2, moves: ArrayList<Move>, state: GameState): Boolean {
            for (x in 0 until 8) {
                for (y in 0 until 8) {
                    val piece = state[x, y] ?: continue

//                    println("PIECE: ${piece.type}")

                    if (piece.team == team) {
                        continue
                    }

                    if (piece.type == KING) {
                        if (abs(x - square.x) < 1 && abs(y - square.y) < 1) {
//                            println("$x $y KING IS TARGETING $square")
                            return true
                        }
                    } else {
                        val possibleMoves = getPossibleMoves(piece.team, piece, Vector2(x, y), state, moves)

//                        println("${piece.type} ")
//                        for (m in possibleMoves) {
//                            println(m)
//                        }
//                        println()
//                        println()
//                        println()

                        if (possibleMoves.contains(square)) {
//                            println("${piece.type} is targetting $square")
                            return true
                        }
                    }
                }
            }
            return false
        }
    }

}