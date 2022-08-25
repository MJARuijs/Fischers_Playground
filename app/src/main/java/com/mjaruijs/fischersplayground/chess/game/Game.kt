package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.AnimationData
import com.mjaruijs.fischersplayground.util.Time
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class Game(val isPlayingWhite: Boolean, var moves: ArrayList<Move> = ArrayList()) {

    protected val state = GameState(isPlayingWhite)

    val board: Board = Board()

    private val takenPieces = ArrayList<Piece>()
    private val piecesTakenByOpponent = ArrayList<Piece>()

    protected var possibleMoves = ArrayList<Vector2>()
    protected var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    private val animationData = ArrayList<AnimationData>()

    protected val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    private var isChecked = false

    private var locked = AtomicBoolean(false)

    var lastUpdated = 0L

    var onPawnPromoted: (Vector2, Team) -> PieceType = { _, _ -> PieceType.QUEEN }
    var enableBackButton: () -> Unit = {}
    var enableForwardButton: () -> Unit = {}
    var onPieceTaken: (PieceType, Team) -> Unit = { _, _ -> }
    var onPieceRegained: (PieceType, Team) -> Unit = { _, _ -> }
//    var onCheck: (Vector2) -> Unit = {}
//    var onCheckCleared: () -> Unit = {}
    var onCheckMate: (Team) -> Unit = {}

//    var onMoveMade: (Move) -> Unit = {}

    init {
        board.requestPossibleMoves = { square ->
            val possibleMoves = determinePossibleMoves(square, getCurrentTeam())
            board.updatePossibleMoves(possibleMoves)
        }
    }

    abstract fun getCurrentTeam(): Team

    abstract fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean): ArrayList<Vector2>

    abstract fun processOnClick(clickedSquare: Vector2): Action

    fun onClick(x: Float, y: Float, displayWidth: Int, displayHeight: Int) {
        val selectedSquare = board.determineSelectedSquare(x, y, displayWidth, displayHeight)
        val action = processOnClick(selectedSquare)

        if (action == Action.SQUARE_SELECTED) {
            board.updateSelectedSquare(selectedSquare)
        } else if (action == Action.SQUARE_DESELECTED || action == Action.PIECE_MOVED) {
            board.deselectSquare()
        }
    }

    fun getMoveIndex() = currentMoveIndex

    private fun incrementMoveCounter(): Int {
        currentMoveIndex++
//        println("CURRENT MOVE INDEX: $currentMoveIndex")
        if (currentMoveIndex == 0) {
            enableBackButton()
        }
        return currentMoveIndex
    }

    private fun decrementMoveCounter(): Int {
        currentMoveIndex--
        if (currentMoveIndex == moves.size - 2) {
            enableForwardButton()
        }
        return currentMoveIndex
    }

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

        val nextMove = moves[incrementMoveCounter()]
        redoMove(nextMove)

        val shouldDisableForwardButton = currentMoveIndex == moves.size - 1
        val shouldEnableBackButton = currentMoveIndex == 0

        return Pair(shouldDisableForwardButton, shouldEnableBackButton)
    }

    fun isShowingCurrentMove(): Boolean {
        return if (moves.isEmpty()) {
            true
        } else {
            currentMoveIndex == moves.size - 1
        }
    }

    operator fun get(i: Int, j: Int): Piece? {
        return state[i, j]
    }

    fun clearBoardData() {
        board.deselectSquare()
    }

    private fun setAnimationData(fromPosition: Vector2, toPosition: Vector2, onAnimationFinished: () -> Unit = {}) {
        animationData += AnimationData(fromPosition, toPosition, onAnimationFinished)
    }

    fun getAnimationData() = animationData

    fun resetAnimationData() {
        animationData.clear()
    }

    fun upgradePawn(square: Vector2, pieceType: PieceType, team: Team) {
        state[square] = Piece(pieceType, team)
        locked.set(false)
    }

    private fun redoMove(move: Move) {
        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

        val piece = Piece(move.movedPiece, move.team)

        if (piece.type == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            performCastle(move.team, fromPosition, toPosition, true)
        } else {
            state[toPosition] = piece
            state[fromPosition] = null

            setAnimationData(fromPosition, toPosition)
        }

        val isCheck = isPlayerChecked(state, !move.team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, !move.team) else false

        if (move.pieceTaken != null) {
            onPieceTaken(move.pieceTaken, !move.team)
        }

        updateCheckData(move.team, isCheck, isCheckMate)
    }

    protected fun undoMove(move: Move) {
        val fromPosition = move.getToPosition(team)
        val toPosition = move.getFromPosition(team)

//        println("Undoing move from ${move.team}: from $fromPosition to $toPosition")

        val piece = Piece(move.movedPiece, move.team)

        if (piece.type == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            val direction = if (toPosition.x < fromPosition.x) -1 else 1
            val newX = if (toPosition.x < fromPosition.x) 7 else 0

            val y = fromPosition.y.roundToInt()
            val oldRookPosition = Vector2(fromPosition.x.roundToInt() + direction, y)
            val newRookPosition = Vector2(newX, y)
            state[toPosition] = state[fromPosition]
            state[fromPosition] = null
            setAnimationData(fromPosition, toPosition) {
                state[newRookPosition] = state[oldRookPosition]
                state[oldRookPosition] = null
                setAnimationData(oldRookPosition, newRookPosition)
            }
        } else {
            state[toPosition] = piece

            if (move.pieceTaken == null) {
                state[fromPosition] = null
            } else {
                state[fromPosition] = Piece(move.pieceTaken, !move.team)
            }

            setAnimationData(fromPosition, toPosition)
        }

        val isCheck = isPlayerChecked(state, move.team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, move.team) else false

        if (move.pieceTaken != null) {
            onPieceRegained(move.pieceTaken, move.team)
        }

        updateCheckData(!move.team, isCheck, isCheckMate)
        decrementMoveCounter()
    }

    open fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean): Move {
        possibleMoves.clear()

//        println("Moving $team from $fromPosition to $toPosition")

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val pieceAtNewPosition = state[toPosition]

        take(currentPositionPiece, fromPosition, toPosition)

        var promotedPiece: PieceType? = null

        if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, runInBackground)
        } else {
            state[toPosition] = currentPositionPiece
            state[fromPosition] = null

            if (!runInBackground) {
                setAnimationData(fromPosition, toPosition)

                if (currentPositionPiece.type == PieceType.PAWN && (toPosition.y == 0f || toPosition.y == 7f)) {
                    promotedPiece = promotePawn(toPosition)
                }
            }
        }

        val isCheck = isPlayerChecked(state, !team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, !team) else false

        updateCheckData(team, isCheck, isCheckMate)

        val actualFromPosition: Vector2
        val actualToPosition: Vector2

        if (this.team == Team.WHITE) {
            actualFromPosition = fromPosition
            actualToPosition = toPosition
        } else {
            actualFromPosition = Vector2(7, 7) - fromPosition
            actualToPosition = Vector2(7, 7) - toPosition
        }

//        println("Move made from: $actualFromPosition, to: $actualToPosition")

        val move = Move(Time.getFullTimeStamp(), team, actualFromPosition, actualToPosition, currentPositionPiece.type, isCheckMate, isCheck, pieceAtNewPosition?.type, promotedPiece)
        if (!runInBackground) {
            if (isShowingCurrentMove()) {
                incrementMoveCounter()
            }
            moves += move
        }

//        onMoveMade(move)

        return move
    }

    fun move(move: Move, runInBackground: Boolean) {
        possibleMoves.clear()

        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

        val currentPositionPiece = Piece(move.movedPiece, move.team)

        take(currentPositionPiece, fromPosition, toPosition)

        if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(move.team, fromPosition, toPosition, runInBackground)
        } else {
            state[toPosition] = currentPositionPiece
            state[fromPosition] = null

            if (!runInBackground) {
                setAnimationData(fromPosition, toPosition)
            }

            if (move.promotedPiece != null) {
                state[toPosition] = Piece(move.promotedPiece, move.team)
            }
        }

        val isCheck = isPlayerChecked(state, team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, team) else false

        updateCheckData(move.team, isCheck, isCheckMate)

        if (!runInBackground) {
            if (isShowingCurrentMove()) {
                incrementMoveCounter()
            }
            moves += move
        }
    }

    private fun take(currentPiece: Piece, fromPosition: Vector2, toPosition: Vector2) {
        val pieceAtNewPosition = state[toPosition]

        val takenPiece = if (pieceAtNewPosition == null) {
            if (currentPiece.type != PieceType.PAWN) {
                return
            }

            val direction = toPosition - fromPosition

            if (direction.x == 0.0f) {
                return
            }

            takeEnPassant(fromPosition, direction)
        } else {
            takeRegularly(toPosition)
        }

        if (takenPiece != null) {
            onPieceTaken(takenPiece.type, takenPiece.team)
            if (this.team == team) {
                takenPieces += takenPiece
            } else {
                piecesTakenByOpponent += takenPiece
            }
        }
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

    private fun promotePawn(toPosition: Vector2): PieceType? {
        locked.set(true)

        Thread {
            onPawnPromoted(toPosition, team)
        }.start()

        while (locked.get()) {
            Thread.sleep(1)
        }

        return state[toPosition]?.type
    }

    private fun determinePossibleMoves(square: Vector2, team: Team): ArrayList<Vector2> {
        val piece = state[square] ?: return arrayListOf()

        possibleMoves = getPieceMoves(piece, square, state, false)
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
        val kingsPosition = findKingPosition(state, team)
        val possibleMovesForOpponent = ArrayList<Vector2>()

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue
//                        && piece.type != PieceType.KING
                if (piece.team != team) {
                    possibleMovesForOpponent += getPieceMoves(piece, Vector2(x, y), state, true)
                }
            }
        }

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

    private fun performCastle(team: Team, fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean) {
        val rookDirection = if (toPosition.x < fromPosition.x) 1 else -1

        val oldRookPosition = if (toPosition.x > fromPosition.x) {
            if (this.team == team) Vector2(7, 0) else Vector2(7, 7)
        } else {
            if (this.team == team) Vector2(0, 0) else Vector2(0, 7)
        }

        val newRookPosition = toPosition + Vector2(rookDirection, 0)

        if (!runInBackground) {
            state[toPosition] = state[fromPosition]
            state[fromPosition] = null

            setAnimationData(fromPosition, toPosition) {
                state[newRookPosition] = state[oldRookPosition]
                state[oldRookPosition] = null

                setAnimationData(oldRookPosition, newRookPosition)
            }
        }
    }

    private fun updateCheckData(team: Team, isCheck: Boolean, isCheckMate: Boolean) {
        when {
            isCheckMate -> onCheckMate(team)
            isCheck -> {
                isChecked = true
                board.checkedKingSquare = findKingPosition(state, !team)
//                onCheck(findKingPosition(state, team))
            }
            isChecked -> {
                isChecked = false
                board.checkedKingSquare = Vector2(-1f, -1f)
//                onCheckCleared()
            }
        }
    }

    private fun findKingPosition(state: GameState, team: Team): Vector2 {
        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue
                if (piece.type == PieceType.KING && piece.team == team) {
                    return Vector2(x, y)
                }
            }
        }

        throw IllegalArgumentException("No king was found for team: $team")
    }

}