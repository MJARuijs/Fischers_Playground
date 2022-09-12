package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.pieces.*
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.animation.AnimationData
import com.mjaruijs.fischersplayground.util.FloatUtils
import com.mjaruijs.fischersplayground.util.Time
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class Game(val isPlayingWhite: Boolean, var lastUpdated: Long, var moves: ArrayList<Move> = ArrayList()) {

    protected val state = ArrayBasedGameState(isPlayingWhite)

    val board: Board = Board()

    private val takenPieces = ArrayList<Piece>()
    private val piecesTakenByOpponent = ArrayList<Piece>()

    protected var possibleMoves = ArrayList<Vector2>()
    protected var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    protected val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    private var isChecked = false

    private val promotionLock = AtomicBoolean(false)
    private val dataLock = AtomicBoolean(false)

    var onPawnPromoted: (Vector2, Team) -> PieceType = { _, _ -> PieceType.QUEEN }
    var enableBackButton: () -> Unit = {}
    var enableForwardButton: () -> Unit = {}
    var disableBackButton: () -> Unit = {}
    var disableForwardButton: () -> Unit = {}
    var onPieceTaken: (PieceType, Team) -> Unit = { _, _ -> }
    var onPieceRegained: (PieceType, Team) -> Unit = { _, _ -> }
//    var onCheck: (Vector2) -> Unit = {}
//    var onCheckCleared: () -> Unit = {}
    var onCheckMate: (Team) -> Unit = {}

    var queueAnimation: (AnimationData) -> Unit = {}

    var onMoveMade: (Move) -> Unit = {}

    init {
        board.requestPossibleMoves = { square ->
            val possibleMoves = determinePossibleMoves(square, getCurrentTeam())

//            println("Requesting possible moves for $square")
//            for (move in possibleMoves) {
//                println(move)
//            }
            board.updatePossibleMoves(possibleMoves)
        }
    }

    private fun lockData() {
        dataLock.set(true)
    }

    fun unlockData() {
        dataLock.set(false)
    }

    private fun isDataLocked() = dataLock.get()

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

    fun addMove(move: Move) {
        moves += move
    }

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

    fun goToLastMove() {
        while (currentMoveIndex < moves.size - 1) {
            showNextMove()
        }
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
//
//    fun getPieces(): ArrayList<Piece> {
//        while (isDataLocked()) {
//            Thread.sleep(1)
//        }
//
//        lockData()
//
//        return state.getPieces()
//    }

    operator fun get(i: Int, j: Int): Piece? {
        return state[i, j]
    }

    fun clearBoardData() {
        board.deselectSquare()
    }

//    protected fun setAnimationData(fromPosition: Vector2, toPosition: Vector2, onAnimationFinished: () -> Unit = {}) {
//        animationData += AnimationData(convertPosition(fromPosition), convertPosition(toPosition), onAnimationFinished)
//    }

    private fun convertPosition(position: Vector2): Vector2 {
        return if (team == Team.WHITE) position else position
    }

//    fun getAnimationData() = animationData

//    fun resetAnimationData() {
//        animationData.clear()
//    }

    fun upgradePawn(square: Vector2, pieceType: PieceType, team: Team) {
        state[square] = Piece(pieceType, team, square)
//        state.remove(square)
//        state.replace(square, pieceType, team)
        promotionLock.set(false)
    }

    private fun redoMove(move: Move) {
        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

//        val piece = Piece(move.movedPiece, move.team)

        if (move.movedPiece == PieceType.KING && abs(toPosition.x - fromPosition.x) == 2.0f) {
            performCastle(move.team, fromPosition, toPosition, true)
        } else {
            val movingPiece = state[fromPosition]!!

            queueAnimation(AnimationData(System.nanoTime(), movingPiece, fromPosition, toPosition) {
                state[toPosition] = movingPiece

                if (move.takenPiecePosition != null) {
                    if (move.takenPiecePosition != toPosition) {
                        state[move.takenPiecePosition] = null
                    }
                }
                state[fromPosition] = null
            })
        }

        val isCheck = isPlayerChecked(state, !move.team)
        val isCheckMate = if (isCheck) isPlayerCheckMate(state, !move.team) else false

        if (move.pieceTaken != null) {
            val takenPiecePosition = move.getTakenPosition(team)
            if (takenPiecePosition != null && !FloatUtils.compare(takenPiecePosition, toPosition)) {
//                state[takenPiecePosition] = null
//                state.remove(takenPiecePosition)
            }
            onPieceTaken(move.pieceTaken, !move.team)
        }

        updateCheckData(move.team, isCheck, isCheckMate)
    }

    protected fun undoMove(move: Move) {
        val toPosition = move.getToPosition(team)
        val fromPosition = move.getFromPosition(team)

//        val piece = Piece(move.movedPiece, move.team)

        if (move.movedPiece == PieceType.KING && abs(fromPosition.x - toPosition.x) == 2.0f) {
            val direction = if (fromPosition.x < toPosition.x) -1 else 1
            val newX = if (fromPosition.x < toPosition.x) 7 else 0

            val y = toPosition.y.roundToInt()
            val oldRookPosition = Vector2(toPosition.x.roundToInt() + direction, y)
            val newRookPosition = Vector2(newX, y)

//            state.move(fromPosition, toPosition) {
//                state.move(oldRookPosition, newRookPosition)
//            }

            state[fromPosition] = state[toPosition]
            state[toPosition] = null
//            setAnimationData(fromPosition, toPosition) {
                state[newRookPosition] = state[oldRookPosition]
                state[oldRookPosition] = null
//                setAnimationData(oldRookPosition, newRookPosition)
//            }
        } else {
//            val movingPiece = state[fromPosition, move.team]!!
//            state.move(fromPosition, toPosition)
//
//            queueAnimation(AnimationData(System.currentTimeMillis(), movingPiece, toPosition) {
//                state.remove(toPosition, !move.team)
//            })

//            state[fromPosition]!!.setPosition(toPosition)
//            val p = state[fromPosition]!!
//            println("$fromPosition $toPosition ::: ${p.type} ${p.team} ${p.boardPosition} ${p.animatedPosition}")

//            if (move.pieceTaken == null) {
//                state[toPosition] = null
//            } else {
//                state[toPosition] = Piece(move.pieceTaken, !move.team, fromPosition)
//            }



            queueAnimation(AnimationData(System.nanoTime(), state[toPosition]!!, toPosition, fromPosition) {
                state[fromPosition] = state[toPosition]

                if (move.pieceTaken == null) {
                    state[toPosition] = null
                } else {
                    if (move.takenPiecePosition!! == toPosition) {
                        state[toPosition] = Piece(move.pieceTaken, !move.team, toPosition)
                    } else {
                        state[move.takenPiecePosition] = Piece(move.pieceTaken, !move.team, toPosition)
                        state[toPosition] = null
                    }
                }

//                state[toPosition] = state[fromPosition]
//                state[fromPosition] = null
//                println("On Finish Undo: ${state[toPosition]!!.boardPosition} ${state[toPosition]!!.animatedPosition}")
            })


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

        val takenPieceData = take(currentPositionPiece, fromPosition, toPosition)

        val takenPiece = takenPieceData?.first
        val takenPiecePosition = takenPieceData?.second

        var promotedPiece: PieceType? = null

        if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, runInBackground)
        } else {

//            val movingPiece = state[fromPosition, team]!!
//            state.move(fromPosition, toPosition) {


//                println("ON FINISH ${movingPiece.boardPosition} ${movingPiece.animatedPosition}")
//                state.remove(toPosition, !team)
//            }

//            queueAnimation(AnimationData(System.currentTimeMillis(), movingPiece, toPosition) {
//                println("ON FINISH ${movingPiece.boardPosition} ${movingPiece.animatedPosition}")
//                state.remove(toPosition, !team)
//                for (piece in getPieces()) {
//                    println("${piece.type} ${piece.team} ${piece.boardPosition} ${piece.animatedPosition}")
//                }
//            })

//            state[toPosition] = currentPositionPiece
//            state[fromPosition] = null

            if (!runInBackground) {
//                state[fromPosition]!!.setPosition(toPosition)

                // TODO: Should this be before or after the animation?


//                println("Move: $fromPosition $toPosition ${state[fromPosition]!!.boardPosition} ${state[fromPosition]!!.animatedPosition}")

                queueAnimation(AnimationData(System.nanoTime(), currentPositionPiece, fromPosition, toPosition) {
                    state[toPosition] = currentPositionPiece
                    state[fromPosition] = null
                })
//                setAnimationData(fromPosition, toPosition)

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

//        println("Move made from: $actualFromPosition, to: $actualToPosition")

        val move = Move(team, actualFromPosition, actualToPosition, currentPositionPiece.type, isCheckMate, isCheck, takenPiece?.type, actualTakenPosition, promotedPiece)
        if (!runInBackground) {
            lastUpdated = Time.getFullTimeStamp()

            if (isShowingCurrentMove()) {
                incrementMoveCounter()
            }
            moves += move
        }

        onMoveMade(move)
        return move
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
            takeEnPassant(fromPosition, direction, !currentPiece.team)
        } else {
            takenPiecePosition = toPosition
            takeRegularly(toPosition, !currentPiece.team)
        }

        if (takenPiece != null) {
            onPieceTaken(takenPiece.type, takenPiece.team)
            if (this.team == team) {
                takenPieces += takenPiece
            } else {
                piecesTakenByOpponent += takenPiece
            }

            return Pair(takenPiece, takenPiecePosition)
        }

        return null
    }

    private fun takeRegularly(toPosition: Vector2, team: Team): Piece? {
        val takenPiece = state[toPosition]
        // TODO: should this be get() or remove() ?

//        state[toPosition] = null
//        state.remove(toPosition)
        return takenPiece

//        return state.get(toPosition, team)
    }

    private fun takeEnPassant(fromPosition: Vector2, direction: Vector2, team: Team): Piece? {
        val squareOfTakenPiece = fromPosition + Vector2(direction.x, 0.0f)
        val takenPiece = state[squareOfTakenPiece]
        state[squareOfTakenPiece] = null
//        state.remove(squareOfTakenPiece)
//        return state.remove(squareOfTakenPiece)
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
//        copiedState.setPosition(fromPosition, toPosition)
        copiedState[fromPosition] = null
        copiedState[toPosition] = piece

        return !isPlayerChecked(copiedState, team)
    }

    protected fun isPlayerChecked(state: ArrayBasedGameState, team: Team): Boolean {
        val kingsPosition = findKingPosition(state, team)
        val possibleMovesForOpponent = ArrayList<Vector2>()

//        val pieces = state.getPieces()

//        for (piece in state.getPieces()) {
//            if (piece.team != team) {
//                possibleMovesForOpponent += getPieceMoves(piece, piece.boardPosition, state, true)
//            }
//        }

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue
//                        && piece.type != PieceType.KING
                if (piece.team != team) {
                    possibleMovesForOpponent += getPieceMoves(piece, Vector2(x, y), state, true)
                }
            }
        }

        unlockData()

        return possibleMovesForOpponent.contains(kingsPosition)
    }

    protected fun isPlayerCheckMate(state: ArrayBasedGameState, team: Team): Boolean {
        val possibleMoves = ArrayList<Vector2>()

//        for (piece in state.getPieces()) {
//            if (piece.team == team) {
//                possibleMoves += determinePossibleMoves(piece, team)
//            }
//        }
//
        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue

                if (piece.team == team) {
                    possibleMoves += determinePossibleMoves(Vector2(x, y), team)
                }
            }
        }

//        unlockData()

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

    protected fun performCastle(team: Team, fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean) {
        val rookDirection = if (toPosition.x < fromPosition.x) 1 else -1

        val oldRookPosition = if (toPosition.x > fromPosition.x) {
            if (this.team == team) Vector2(7, 0) else Vector2(7, 7)
        } else {
            if (this.team == team) Vector2(0, 0) else Vector2(0, 7)
        }

        val newRookPosition = toPosition + Vector2(rookDirection, 0)

        if (!runInBackground) {
//            state.move(fromPosition, toPosition) {
//                state.move(oldRookPosition, newRookPosition)
//            }
//            state.add(state[fromPosition]!!)
//            state.remove(fromPosition)
//            state[toPosition] = state[fromPosition]
//            state[fromPosition] = null

//            setAnimationData(fromPosition, toPosition) {
//                state[newRookPosition] = state[oldRookPosition]
//                state[oldRookPosition] = null

//                setAnimationData(oldRookPosition, newRookPosition)
//            }
        }
    }

    protected fun updateCheckData(team: Team, isCheck: Boolean, isCheckMate: Boolean) {
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

    private fun findKingPosition(state: ArrayBasedGameState, team: Team): Vector2 {
        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x, y] ?: continue
                if (piece.type == PieceType.KING && piece.team == team) {
                    return Vector2(x, y)
                }
            }
        }

//        for (piece in state.getPieces()) {
//            if (piece.type == PieceType.KING && piece.team == team) {
//                return piece.boardPosition
//            }
//        }

//        unlockData()

        throw IllegalArgumentException("No king was found for team: $team")
    }

}