package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.pieces.*
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.animation.AnimationData
import com.mjaruijs.fischersplayground.util.Time
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class Game(val isPlayingWhite: Boolean, var lastUpdated: Long, var moves: ArrayList<Move> = ArrayList()) {

    val state = ArrayBasedGameState(isPlayingWhite)

    val board: Board = Board()

    val takenPieces = ArrayList<Piece>()

    protected var possibleMoves = ArrayList<Vector2>()
    var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    protected val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    private var isChecked = false

    private val promotionLock = AtomicBoolean(false)

    var onPawnPromoted: (Vector2, Team) -> PieceType = { _, _ -> PieceType.QUEEN }
    var enableBackButton: () -> Unit = {}
    var enableForwardButton: () -> Unit = {}
    var disableBackButton: () -> Unit = {}
    var disableForwardButton: () -> Unit = {}
    var onPieceTaken: (PieceType, Team) -> Unit = { _, _ -> }
    var onPieceRegained: (PieceType, Team) -> Unit = { _, _ -> }
    var onCheckMate: (Team) -> Unit = {}

    var queueAnimation: (AnimationData) -> Unit = {}

    var onMoveMade: (Move) -> Unit = {
        println("onMoveMade() called, but not yet set")
    }

    init {
        board.requestPossibleMoves = { square ->
            val possibleMoves = determinePossibleMoves(square, getCurrentTeam())
            board.updatePossibleMoves(possibleMoves)
        }
    }

    abstract fun getCurrentTeam(): Team

    abstract fun getPieceMoves(piece: Piece, square: Vector2, state: ArrayBasedGameState, lookingForCheck: Boolean): ArrayList<Vector2>

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

    protected fun incrementMoveCounter(): Int {
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

    open fun showPreviousMove(runInBackground: Boolean, animationSpeed: Long = DEFAULT_ANIMATION_SPEED): Pair<Boolean, Boolean> {
        if (currentMoveIndex == -1) {
            return Pair(first = true, second = false)
        }

        val currentMove = moves[currentMoveIndex]
        undoMove(currentMove, runInBackground, animationSpeed)

        val shouldDisableBackButton = currentMoveIndex == -1
        val shouldEnableForwardButton = currentMoveIndex == moves.size - 2

        return Pair(shouldDisableBackButton, shouldEnableForwardButton)
    }

    open fun showNextMove(animationSpeed: Long = DEFAULT_ANIMATION_SPEED): Pair<Boolean, Boolean> {
        if (currentMoveIndex >= moves.size - 1) {
            return Pair(first = true, second = false)
        }

        val nextMove = moves[incrementMoveCounter()]
        redoMove(nextMove, animationSpeed)

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

    fun upgradePawn(square: Vector2, pieceType: PieceType, team: Team) {
        state[square] = Piece(pieceType, team)
        promotionLock.set(false)
    }

    protected fun createAnimation(animationSpeed: Long, fromPosition: Vector2, toPosition: Vector2, takenPiece: Piece? = null, takenPiecePosition: Vector2? = null, onStart: () -> Unit = {}, onFinish: () -> Unit = {}): AnimationData {
        val translation = toPosition - fromPosition
        return AnimationData(animationSpeed, System.nanoTime(), fromPosition, translation, takenPiece, takenPiecePosition, {
            state[toPosition] = state[fromPosition]
            state[fromPosition] = null
            onStart()
        }, {
            onFinish()
        }, null)
    }

    private fun onStartRedoMove(move: Move, toPosition: Vector2, takenPiecePosition: Vector2?) {
        if (takenPiecePosition != null) {
            onPieceTaken(move.pieceTaken!!, !move.team)

            if (takenPiecePosition != toPosition) {
                state[takenPiecePosition] = null
            }
        }
    }

    private fun onFinishRedoMove(move: Move, toPosition: Vector2) {
        if (move.promotedPiece != null) {
            state[toPosition] = Piece(move.promotedPiece, move.team)
        }
    }

    private fun redoMove(move: Move, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

        val animation = if (move.movedPiece == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            performCastle(move.team, fromPosition, toPosition, animationSpeed)
        } else {
            val takenPiece = if (move.pieceTaken == null) {
                null
            } else {
                Piece(move.pieceTaken, !move.team)
            }

            val takenPiecePosition = move.getTakenPosition(team)

            createAnimation(animationSpeed, fromPosition, toPosition, takenPiece, takenPiecePosition, {
                onStartRedoMove(move, toPosition, takenPiecePosition)
            }, {
                onFinishRedoMove(move, toPosition)
            })
        }

        animation.onFinishCalls += {
            val isCheck = isPlayerChecked(state, !move.team)
            val isCheckMate = if (isCheck) isPlayerCheckMate(state, !move.team) else false

            updateCheckData(move.team, isCheck, isCheckMate)
        }

        queueAnimation(animation)
    }

    private fun onStartUndoMove(move: Move, toPosition: Vector2, takenPiecePosition: Vector2?) {
        if (takenPiecePosition != null) {
            onPieceRegained(move.pieceTaken!!, move.team)

            if (takenPiecePosition == toPosition) {
                state[toPosition] = Piece(move.pieceTaken, !move.team)
            } else {
                state[takenPiecePosition] = Piece(move.pieceTaken, !move.team)
            }
        }
    }

    private fun onFinishUndoMove(move: Move, fromPosition: Vector2) {
        if (move.promotedPiece != null) {
            state[fromPosition] = Piece(PieceType.PAWN, move.team)
        }
    }

    fun undoMove(move: Move, runInBackground: Boolean, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        val toPosition = move.getToPosition(team)
        val fromPosition = move.getFromPosition(team)

        val animation = if (move.movedPiece == PieceType.KING && abs(fromPosition.x - toPosition.x) == 2.0f) {
            undoCastle(fromPosition, toPosition, animationSpeed)
        } else {
            val takenPiece = if (move.pieceTaken == null) {
                null
            } else {
                Piece(move.pieceTaken, !move.team)
            }

            val takenPiecePosition = move.getTakenPosition(team)

            createAnimation(animationSpeed, toPosition, fromPosition, takenPiece, takenPiecePosition, {
                onStartUndoMove(move, toPosition, takenPiecePosition)
            }, {
                onFinishUndoMove(move, fromPosition)
            })
        }

        decrementMoveCounter()

        animation.onFinishCalls += {
            finishMove(move)
        }

        if (runInBackground) {
            animation.invokeOnStartCalls()
            animation.invokeOnFinishCalls()
        } else {
            queueAnimation(animation)
        }
    }

    open fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        possibleMoves.clear()

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")

        val takenPieceData = take(currentPositionPiece, fromPosition, toPosition)

        val takenPiece = takenPieceData?.first
        val takenPiecePosition = takenPieceData?.second

        var promotedPiece: PieceType? = null

        val animation = if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, animationSpeed)
        } else {
            createAnimation(animationSpeed, fromPosition, toPosition, takenPiece, takenPiecePosition)
        }

        val animationStarted = AtomicBoolean(false)
        animation.onStartCalls += {
            animationStarted.set(true)
        }
        animation.onFinishCalls += {
            if (currentPositionPiece.type == PieceType.PAWN && (toPosition.y == 0f || toPosition.y == 7f)) {
                promotedPiece = promotePawn(toPosition)
            }

            val isCheck = isPlayerChecked(state, !team)
            val isCheckMate = if (isCheck) isPlayerCheckMate(state, !team) else false

            updateCheckData(team, isCheck, isCheckMate)

            val actualFromPosition: Vector2
            val actualToPosition: Vector2
            val actualTakenPosition: Vector2?

            if (this.team == Team.WHITE) {
                actualFromPosition = fromPosition
                actualToPosition = toPosition
                actualTakenPosition = takenPiecePosition
            } else {
                actualFromPosition = Vector2(7, 7) - fromPosition
                actualToPosition = Vector2(7, 7) - toPosition
                actualTakenPosition = if (takenPiecePosition != null) {
                    Vector2(7, 7) - takenPiecePosition
                } else {
                    null
                }
            }

            val move = Move(team, actualFromPosition, actualToPosition, currentPositionPiece.type, isCheckMate, isCheck, takenPiece?.type, actualTakenPosition, promotedPiece)
            if (!runInBackground) {
                lastUpdated = Time.getFullTimeStamp()

                if (isShowingCurrentMove()) {
                    incrementMoveCounter()
                }
                moves += move
            }

            onMoveMade(move)
        }

        if (runInBackground) {
            animation.invokeOnStartCalls()
            animation.invokeOnFinishCalls()
        } else {
            queueAnimation(animation)
        }
    }

    protected fun finishMove(move: Move) {
        val isCheck = isPlayerChecked(state, move.team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, move.team) else false

        updateCheckData(!move.team, isCheck, isCheckMate)
    }

    protected fun take(currentPiece: Piece, fromPosition: Vector2, toPosition: Vector2): Pair<Piece, Vector2>? {
        val pieceAtNewPosition = state[toPosition]

        val takenPiecePosition: Vector2?

        val takenPiece = if (pieceAtNewPosition == null) {
            if (currentPiece.type != PieceType.PAWN) {
                return null
            }

            val direction = toPosition - fromPosition

            if (direction.x == 0.0f) {
                return null
            }

            takenPiecePosition = fromPosition + Vector2(direction.x, 0.0f)
            takeEnPassant(fromPosition, direction)
        } else {
            takenPiecePosition = toPosition
            takeRegularly(toPosition)
        }

        if (takenPiece != null) {
            onPieceTaken(takenPiece.type, takenPiece.team)
            return Pair(takenPiece, takenPiecePosition)
        }

        return null
    }

    private fun takeRegularly(toPosition: Vector2): Piece? {
        return state[toPosition]
    }

    private fun takeEnPassant(fromPosition: Vector2, direction: Vector2): Piece? {
        val squareOfTakenPiece = fromPosition + Vector2(direction.x, 0.0f)
        val takenPiece = state[squareOfTakenPiece]
        state[squareOfTakenPiece] = null
        return takenPiece
    }

    private fun promotePawn(toPosition: Vector2): PieceType? {
        promotionLock.set(true)

        Thread {
            onPawnPromoted(toPosition, team)
        }.start()

        while (promotionLock.get()) {
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

    protected fun isPlayerChecked(state: ArrayBasedGameState, team: Team): Boolean {
        val kingsPosition = findKingPosition(state, team)
        val possibleMovesForOpponent = ArrayList<Vector2>()

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue
                if (piece.team != team) {
                    possibleMovesForOpponent += getPieceMoves(piece, Vector2(x, y), state, true)
                }
            }
        }

        return possibleMovesForOpponent.contains(kingsPosition)
    }

    protected fun isPlayerCheckMate(state: ArrayBasedGameState, team: Team): Boolean {
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

    protected fun isCastling(piece: Piece, fromPosition: Vector2, toPosition: Vector2): Boolean {
        if (piece.type != PieceType.KING) {
            return false
        }

        if (abs(fromPosition.x.roundToInt() - toPosition.x.roundToInt()) == 2) {
            return true
        }

        return false
    }

    private fun undoCastle(fromPosition: Vector2, toPosition: Vector2, animationSpeed: Long): AnimationData {
        val direction = if (fromPosition.x < toPosition.x) -1 else 1
        val newX = if (fromPosition.x < toPosition.x) 7 else 0

        val y = toPosition.y.roundToInt()
        val oldRookPosition = Vector2(toPosition.x.roundToInt() + direction, y)
        val newRookPosition = Vector2(newX, y)

        val kingAnimation = createAnimation(animationSpeed, toPosition, fromPosition)
        val rookAnimation = createAnimation(animationSpeed, oldRookPosition, newRookPosition)
        kingAnimation.nextAnimation = rookAnimation
        return kingAnimation
    }

    protected fun performCastle(team: Team, fromPosition: Vector2, toPosition: Vector2, animationSpeed: Long): AnimationData {
        val rookDirection = if (toPosition.x < fromPosition.x) 1 else -1

        val oldRookPosition = if (toPosition.x > fromPosition.x) {
            if (this.team == team) Vector2(7, 0) else Vector2(7, 7)
        } else {
            if (this.team == team) Vector2(0, 0) else Vector2(0, 7)
        }

        val newRookPosition = toPosition + Vector2(rookDirection, 0)

        val kingAnimation = createAnimation(animationSpeed, fromPosition, toPosition)
        val rookAnimation = createAnimation(animationSpeed, oldRookPosition, newRookPosition)
        kingAnimation.nextAnimation = rookAnimation
        return kingAnimation
    }

    protected fun updateCheckData(team: Team, isCheck: Boolean, isCheckMate: Boolean) {
        when {
            isCheckMate -> {
                onCheckMate(team)
            }
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

    private fun findKingPosition(state: ArrayBasedGameState, team: Team): Vector2 {
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

    companion object {
        const val DEFAULT_ANIMATION_SPEED = 500L
    }

}