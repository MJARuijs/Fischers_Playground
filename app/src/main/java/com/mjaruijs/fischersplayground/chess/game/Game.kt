package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.AnimationData
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class Game(isPlayingWhite: Boolean, protected var moves: ArrayList<Move> = ArrayList()) {

    protected val state = GameState(isPlayingWhite)

    private val takenPieces = ArrayList<Piece>()
    private val piecesTakenByOpponent = ArrayList<Piece>()

    protected var possibleMoves = ArrayList<Vector2>()
    protected var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    private val animationData = ArrayList<AnimationData>()

    protected val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    abstract fun getCurrentTeam(): Team

    abstract fun getPieceMoves(piece: Piece, square: Vector2, state: GameState): ArrayList<Vector2>

    abstract fun processAction(action: Action): Action

    open fun showPreviousMove(): Pair<Boolean, Boolean> {
        if (currentMoveIndex == -1) {
            return Pair(first = true, second = false)
        }

        val currentMove = moves[currentMoveIndex]
        undoMove(currentMove)

        val shouldDisableBackButton = currentMoveIndex == -1
        val shouldEnableForwardButton = currentMoveIndex == moves.size - 2

        return Pair(shouldDisableBackButton, shouldEnableForwardButton)
    }

    open fun showNextMove(): Pair<Boolean, Boolean> {
        if (currentMoveIndex >= moves.size - 1) {
            return Pair(first = true, second = false)
        }

        val nextMove = moves[++currentMoveIndex]
        redoMove(nextMove)

        val shouldDisableForwardButton = currentMoveIndex == moves.size - 1
        val shouldEnableBackButton = currentMoveIndex == 0

        return Pair(shouldDisableForwardButton, shouldEnableBackButton)
    }

    protected fun isShowingCurrentMove(): Boolean {
        return if (moves.isEmpty()) {
            true
        } else {
            currentMoveIndex == moves.size - 1
        }
    }

    operator fun get(i: Int, j: Int): Piece? {
        return state[i, j]
    }

    private fun setAnimationData(fromPosition: Vector2, toPosition: Vector2, onAnimationFinished: () -> Unit = {}) {
        animationData += AnimationData(fromPosition, toPosition, onAnimationFinished)
    }

    fun getAnimationData() = animationData

    fun resetAnimationData() {
        animationData.clear()
    }

    private fun redoMove(move: Move) {
        val fromPosition = move.fromPosition
        val toPosition = move.toPosition

        val piece = Piece(move.movedPiece, move.team)

        if (piece.type == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            performCastle(move.team, fromPosition, toPosition, true)
        } else {
            setAnimationData(fromPosition, toPosition) {
                state[toPosition] = piece

                if (move.pieceTaken == null) {
                    state[fromPosition] = null
                } else {
                    state[fromPosition] = Piece(move.pieceTaken, !move.team)
                }
            }
        }
    }

    protected fun undoMove(move: Move) {
        val fromPosition = move.toPosition
        val toPosition = move.fromPosition

        val piece = Piece(move.movedPiece, move.team)

        if (piece.type == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            val direction = if (toPosition.x < fromPosition.x) -1 else 1
            val newX = if (toPosition.x < fromPosition.x) 7 else 0

            val y = fromPosition.y.roundToInt()
            val oldRookPosition = Vector2(fromPosition.x.roundToInt() + direction, y)
            val newRookPosition = Vector2(newX, y)

            setAnimationData(fromPosition, toPosition) {
                state[toPosition] = state[fromPosition]
                state[fromPosition] = null
                setAnimationData(oldRookPosition, newRookPosition) {
                    state[newRookPosition] = state[oldRookPosition]
                    state[oldRookPosition] = null
                }
            }
        } else {
            setAnimationData(fromPosition, toPosition) {
                state[toPosition] = piece

                if (move.pieceTaken == null) {
                    state[fromPosition] = null
                } else {
                    state[fromPosition] = Piece(move.pieceTaken, !move.team)
                }
            }
        }

        currentMoveIndex--
    }

    protected fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, shouldAnimate: Boolean): Move {
        possibleMoves.clear()

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val pieceAtNewPosition = state[toPosition]

        val takenPiece = take(currentPositionPiece, fromPosition, toPosition)

        if (takenPiece != null) {
            if (this.team == team) {
                takenPieces += takenPiece
            } else {
                piecesTakenByOpponent += takenPiece
            }
        }

        if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, shouldAnimate)
        } else {
            if (shouldAnimate) {
                setAnimationData(fromPosition, toPosition) {
                    state[fromPosition] = null
                    state[toPosition] = currentPositionPiece
                }
            } else {
                state[fromPosition] = null
                state[toPosition] = currentPositionPiece
            }
        }

        val isCheck = isPlayerChecked(state, !team)
        println("IsCheck: $isCheck ${!team}")

        val isCheckMate = if (!isCheck) {
            false
        } else {
            isPlayerCheckMate(state, !team)
        }

        println("CHECKMATE: $isCheckMate")

        val move = Move(team, fromPosition, toPosition, currentPositionPiece.type, isCheckMate, isCheck, pieceAtNewPosition?.type)
        if (shouldAnimate) {
            if (isShowingCurrentMove()) {
                currentMoveIndex++
            }
            moves += move
        }

        return move
    }

    private fun take(currentPiece: Piece, fromPosition: Vector2, toPosition: Vector2): Piece? {
        val pieceAtNewPosition = state[toPosition]

        if (pieceAtNewPosition == null) {
            if (currentPiece.type != PieceType.PAWN) {
                return null
            }

            val direction = toPosition - fromPosition

            if (direction.x == 0.0f) {
                return null
            }

            return takeEnPassant(fromPosition, direction)
        }

        return takeRegularly(toPosition)
    }

    private fun takeRegularly(toPosition: Vector2): Piece? {
        val takenPiece = state[toPosition]
        state[toPosition] = null
        return takenPiece
    }

    private fun takeEnPassant(fromPosition: Vector2, direction: Vector2): Piece? {
        val squareOfTakenPiece = fromPosition + Vector2(direction.x, 0.0f)
        val takenPiece = state[squareOfTakenPiece]
        state[squareOfTakenPiece] = null
        return takenPiece
    }

    fun determinePossibleMoves(square: Vector2, team: Team): ArrayList<Vector2> {
        val piece = state[square] ?: return arrayListOf()

        possibleMoves = getPieceMoves(piece, square, state)
        possibleMoves.removeIf { move -> !isMoveValid(square, move, piece, team) }
        return possibleMoves
    }

    private fun isMoveValid(fromPosition: Vector2, toPosition: Vector2, piece: Piece, team: Team): Boolean {
        val copiedState = state.copy()
        copiedState[fromPosition] = null
        copiedState[toPosition] = piece

        return !isPlayerChecked(copiedState, team)
    }

    private fun isPlayerChecked(state: GameState, team: Team): Boolean {
        val kingsPosition = Vector2(-1f, -1f)

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue
                if (piece.type == PieceType.KING && piece.team == team) {
                    kingsPosition.x = x.toFloat()
                    kingsPosition.y = y.toFloat()
                    break
                }
            }
        }

        val possibleMovesForOpponent = ArrayList<Vector2>()

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue

                if (piece.team != team && piece.type != PieceType.KING) {
                    possibleMovesForOpponent += getPieceMoves(piece, Vector2(x, y), state)
                }
            }
        }

//        for (m in possibleMovesForOpponent) {
//            println(m)
//        }

        return possibleMovesForOpponent.contains(kingsPosition)
    }

    private fun isPlayerCheckMate(state: GameState, team: Team): Boolean {
        val possibleMoves = ArrayList<Vector2>()

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue

                if (piece.team == team) {
                    possibleMoves += determinePossibleMoves(Vector2(x, y), team)
                }
            }
        }

        return possibleMoves.isEmpty()
    }

    private fun isCastling(piece: Piece, fromPosition: Vector2, toPosition: Vector2): Boolean {
        if (piece.type != PieceType.KING) {
            return false
        }

        if (abs(fromPosition.x.roundToInt() - toPosition.x.roundToInt()) == 2) {
            return true
        }

        return false
    }

    private fun performCastle(team: Team, fromPosition: Vector2, toPosition: Vector2, shouldAnimate: Boolean) {
        val rookDirection = if (toPosition.x < fromPosition.x) 1 else -1

        val oldRookPosition = if (toPosition.x > fromPosition.x) {
            if (this.team == team) Vector2(7, 0) else Vector2(7, 7)
        } else {
            if (this.team == team) Vector2(0, 0) else Vector2(0, 7)
        }

        val newRookPosition = toPosition + Vector2(rookDirection, 0)

        if (shouldAnimate) {
            setAnimationData(fromPosition, toPosition) {
                state[toPosition] = state[fromPosition]
                state[fromPosition] = null
                setAnimationData(oldRookPosition, newRookPosition) {
                    state[newRookPosition] = state[oldRookPosition]
                    state[oldRookPosition] = null
                }
            }
        }
    }

}