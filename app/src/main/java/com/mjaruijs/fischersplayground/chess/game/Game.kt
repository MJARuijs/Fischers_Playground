package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.Action2
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.AnimationData
import com.mjaruijs.fischersplayground.util.Time
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class Game(isPlayingWhite: Boolean, protected var moves: ArrayList<Move> = ArrayList()) {

    protected val state = GameState(isPlayingWhite)

    val board: Board = Board()

    private val takenPieces = ArrayList<Piece>()
    private val piecesTakenByOpponent = ArrayList<Piece>()

    protected var possibleMoves = ArrayList<Vector2>()
    protected var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    private val animationData = ArrayList<AnimationData>()

    protected val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    private var isChecked = false

    var lastUpdated = 0L

    var onPawnPromoted: (Vector2, Team) -> PieceType = { _, _ -> PieceType.QUEEN }
    var enableBackButton: () -> Unit = {}
    var enableForwardButton: () -> Unit = {}
    var onPieceTaken: (PieceType, Team) -> Unit = { _, _ -> }
//    var onCheck: (Vector2) -> Unit = {}
//    var onCheckCleared: () -> Unit = {}
    var onCheckMate: (Team) -> Unit = {}

    init {
        board.requestPossibleMoves = { square ->
            val possibleMoves = determinePossibleMoves(square, getCurrentTeam())
            board.updatePossibleMoves(possibleMoves)
        }
    }

    abstract fun getCurrentTeam(): Team

    abstract fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean): ArrayList<Vector2>

    abstract fun processAction(action: Action): Action

    abstract fun processOnClick(square: Vector2): Action2

    fun onClick(x: Float, y: Float, displayWidth: Int, displayHeight: Int) {
//        val clickAction = board.onClick(x, y, displayWidth, displayHeight)
//        val boardAction = processAction(clickAction)
//        board.processAction(boardAction)

        val selectedSquare = board.determineSelectedSquare(x, y, displayWidth, displayHeight)

        val action = processOnClick(selectedSquare)

        if (action == Action2.SQUARE_SELECTED) {
            board.updateSelectedSquare(selectedSquare)
        } else if (action == Action2.SQUARE_DESELECTED || action == Action2.PIECE_MOVED) {
            board.deselectSquare()
        }
    }

    private fun incrementMoveCounter(): Int {
        currentMoveIndex++
        println("CURRENT MOVE INDEX: $currentMoveIndex")
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

    fun clearBoardData() {
        board.deselectSquare()
    }

    private fun setAnimationData(pieceType: PieceType, fromPosition: Vector2, toPosition: Vector2, onAnimationFinished: () -> Unit = {}) {
        animationData += AnimationData(pieceType, fromPosition, toPosition, onAnimationFinished)
    }

    fun getAnimationData() = animationData

    fun resetAnimationData() {
        animationData.clear()
    }

    fun upgradePawn(square: Vector2, pieceType: PieceType, team: Team) {
        state[square] = Piece(pieceType, team)
    }

    private fun redoMove(move: Move) {
        val fromPosition = move.fromPosition
        val toPosition = move.toPosition

        val piece = Piece(move.movedPiece, move.team)

        if (piece.type == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            performCastle(move.team, fromPosition, toPosition, true)
        } else {
            state[toPosition] = piece
            state[fromPosition] = null

            setAnimationData(piece.type, fromPosition, toPosition)
        }

        val isCheck = isPlayerChecked(state, !move.team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, !move.team) else false

        updateCheckData(!move.team, isCheck, isCheckMate)
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
            state[toPosition] = state[fromPosition]
            state[fromPosition] = null
            setAnimationData(piece.type, fromPosition, toPosition) {
                state[newRookPosition] = state[oldRookPosition]
                state[oldRookPosition] = null
                setAnimationData(piece.type, oldRookPosition, newRookPosition)
            }
        } else {
            state[toPosition] = piece

            if (move.pieceTaken == null) {
                state[fromPosition] = null
            } else {
                state[fromPosition] = Piece(move.pieceTaken, !move.team)
            }

            setAnimationData(piece.type, fromPosition, toPosition)
        }

        val isCheck = isPlayerChecked(state, move.team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, move.team) else false

        updateCheckData(move.team, isCheck, isCheckMate)
        decrementMoveCounter()
    }

    open fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean): Move {
        possibleMoves.clear()

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val pieceAtNewPosition = state[toPosition]
        println("MOVING PIECE FROM $team: ${currentPositionPiece.type} from: $fromPosition to: $toPosition")

        val takenPiece = take(currentPositionPiece, fromPosition, toPosition)

        if (takenPiece != null) {
            onPieceTaken(takenPiece.type, takenPiece.team)
            if (this.team == team) {
                takenPieces += takenPiece
            } else {
                piecesTakenByOpponent += takenPiece
            }
        }

        if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, runInBackground)
//            return finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)
        } else {
            state[toPosition] = currentPositionPiece
            state[fromPosition] = null

            if (!runInBackground) {
                setAnimationData(currentPositionPiece.type, fromPosition, toPosition)
            }

//            if (currentPositionPiece.type == PieceType.PAWN && (toPosition.y == 0f || toPosition.y == 7f)) {
//                val promotedPieceType = onPawnPromoted(toPosition, team)
//                state[toPosition] = Piece(promotedPieceType, team)
//                return finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)

//            } else {
//                return finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)
//            }

        }

        val isCheck = isPlayerChecked(state, !team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, !team) else false

        updateCheckData(!team, isCheck, isCheckMate)

        val move = Move(Time.getFullTimeStamp(), team, fromPosition, toPosition, currentPositionPiece.type, isCheckMate, isCheck, pieceAtNewPosition?.type)
        if (!runInBackground) {
            if (isShowingCurrentMove()) {
                incrementMoveCounter()
            }
            moves += move
        }

        return move
//        return finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)
    }

//    private fun finishMove(fromPosition: Vector2, toPosition: Vector2, currentPositionPiece: Piece, pieceAtNewPosition: Piece?, runInBackground: Boolean): Move {
//        val isCheck = isPlayerChecked(state, !team)
//        val isCheckMate = if (isCheck) isPlayerCheckMate(state, !team) else false
//
//        updateCheckData(!team, isCheck, isCheckMate)
//
//        val move = Move(Time.getFullTimeStamp(), team, fromPosition, toPosition, currentPositionPiece.type, isCheckMate, isCheck, pieceAtNewPosition?.type)
//        if (!runInBackground) {
//            if (isShowingCurrentMove()) {
//                incrementMoveCounter()
//            }
//            moves += move
//        }
//
//        return move
//    }

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

            setAnimationData(PieceType.KING, fromPosition, toPosition) {
                state[newRookPosition] = state[oldRookPosition]
                state[oldRookPosition] = null

                setAnimationData(PieceType.ROOK, oldRookPosition, newRookPosition)
            }
        }
    }

    private fun updateCheckData(team: Team, isCheck: Boolean, isCheckMate: Boolean) {
        when {
            isCheckMate -> onCheckMate(team)
            isCheck -> {
                isChecked = true
                board.checkedKingSquare = findKingPosition(state, team)
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