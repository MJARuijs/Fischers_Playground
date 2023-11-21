package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.renderer.animation.AnimationData
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.Time
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class Game(val isPlayingWhite: Boolean, var lastUpdated: Long, var moves: ArrayList<Move> = ArrayList()) {

    var state = GameState(isPlayingWhite)

    val board: Board = Board()
    var pieceMoving = false

    val takenPieces = ArrayList<Piece>()

    protected var possibleMoves = ArrayList<Vector2>()
    var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    private var isChecked = false

    private val promotionLock = AtomicBoolean(false)

    var onPawnPromoted: (Vector2, Team) -> PieceType = { _, _ -> PieceType.QUEEN }
    var enableBackButton: () -> Unit = {}
    var enableForwardButton: () -> Unit = {}
    var disableBackButton: () -> Unit = {}
    var onPieceTaken: (PieceType, Team) -> Unit = { _, _ ->
        Logger.warn(TAG, "Tried to call onPieceTaken(), but function is not set yet..")
    }
    var onPieceRegained: (PieceType, Team) -> Unit = { _, _ ->
        Logger.warn(TAG, "Tried to call onPieceRegained(), but function is not set yet..")
    }
    var onCheckMate: (Team) -> Unit = {
        Logger.warn(TAG, "Tried to call onCheckMate(), but function is not set yet..")
    }
    var onAnimationStarted: () -> Unit = {}
    var onAnimationFinished: (Int) -> Unit = {}

    var queueAnimation: (AnimationData) -> Unit = {
        Logger.warn(TAG,"Tried to animate move, but function is not set yet..")
    }

    var onMoveMade: (Move) -> Unit = {
        Logger.warn(TAG, "Tried to make a move, but function is not set yet..")
    }

    init {
        board.requestPossibleMoves = { square ->
            val possibleMoves = determinePossibleMoves(square, getCurrentTeam())
            board.updatePossibleMoves(possibleMoves)
        }
    }

    abstract fun getCurrentTeam(): Team

    abstract fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean): ArrayList<Vector2>

    abstract fun processOnClick(clickedSquare: Vector2)

    abstract fun processOnLongClick(clickedSquare: Vector2)

    fun onClick(x: Float, y: Float, displayWidth: Int, displayHeight: Int) {
        val selectedSquare = board.determineSelectedSquare(x, y, displayWidth, displayHeight)
        processOnClick(selectedSquare)
    }

    fun onLongClick(x: Float, y: Float, displayWidth: Int, displayHeight: Int) {
        val selectedSquare = board.determineSelectedSquare(x, y, displayWidth, displayHeight)
        processOnLongClick(selectedSquare)
    }

    fun determineSelectedSquare(x: Float, y: Float, displayWidth: Int, displayHeight: Int) = board.determineSelectedSquare(x, y, displayWidth, displayHeight)

    fun getMoveIndex() = currentMoveIndex

    fun getCurrentMove(): Move? {
        if (moves.isEmpty()) {
            return null
        }

        if (currentMoveIndex == -1) {
            return null
        }

        if (currentMoveIndex >= moves.size) {
            return null
        }

        return moves[currentMoveIndex]
    }

    protected fun incrementMoveCounter(): Int {
        currentMoveIndex++
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

    open fun resetMoves() {
        state.reset()

        currentMoveIndex = -1

        moves.clear()
        clearBoardData()
    }

    open fun swapMoves(newMoves: ArrayList<Move>, selectedMoveIndex: Int) {
        resetMoves()

        for (move in newMoves) {
            moves.add(move)
        }

        while (currentMoveIndex != selectedMoveIndex) {
            showNextMove(true, 0L)
        }
    }

    fun goToMove(move: Move) {
        if (!moves.contains(move)) {
            return
        }

        val moveIndex = moves.indexOf(move)
        val animationSpeed = 0L

        if (moveIndex < currentMoveIndex) {
            while (currentMoveIndex != moveIndex) {
                showPreviousMove(false, animationSpeed)
            }
        } else if (moveIndex > currentMoveIndex) {
            while (currentMoveIndex != moveIndex) {
                showNextMove(false, animationSpeed)
            }
        }

        clearBoardData()
    }

    open fun showPreviousMove(runInBackground: Boolean, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        if (currentMoveIndex == -1) {
            return
        }

        val currentMove = moves[currentMoveIndex]
        undoMove(currentMove, runInBackground, animationSpeed)
    }

    open fun showNextMove(runInBackground: Boolean, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        if (currentMoveIndex >= moves.size - 1) {
            return
        }

        val nextMove = moves[incrementMoveCounter()]
        redoMove(nextMove, runInBackground, animationSpeed)
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

    protected fun createAnimation(animationSpeed: Long, fromPosition: Vector2, toPosition: Vector2, isReversed: Boolean, takenPiece: Piece? = null, takenPiecePosition: Vector2? = null, onStart: () -> Unit = {}, onFinish: () -> Unit = {}): AnimationData {
        val translation = toPosition - fromPosition
        return AnimationData(animationSpeed, System.nanoTime(), fromPosition, translation, takenPiece, takenPiecePosition, isReversed, {
            state[toPosition] = state[fromPosition]
            state[fromPosition] = null
            onStart()
        }, {
            onFinish()
        }, null)
    }

    private fun onFinishRedoMove(move: Move, toPosition: Vector2, takenPiecePosition: Vector2?) {
        if (move.promotedPiece != null) {
            state[toPosition] = Piece(move.promotedPiece, move.team)
        }

        if (takenPiecePosition != null) {
            onPieceTaken(move.pieceTaken!!, !move.team)

            if (takenPiecePosition != toPosition) {
                state[takenPiecePosition] = null
            }
        }
    }

    private fun redoMove(move: Move, runInBackground: Boolean, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
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

            createAnimation(animationSpeed, fromPosition, toPosition, false, takenPiece, takenPiecePosition, {}, {
                onFinishRedoMove(move, toPosition, takenPiecePosition)
            })
        }

        val moveIndex = currentMoveIndex

        if (animation.nextAnimation == null) {
            animation.onStartCalls += {
                onAnimationStarted()
            }
            animation.onFinishCalls += {
                val isCheck = isPlayerChecked(state, !move.team)
                val isCheckMate = if (isCheck) isPlayerCheckMate(state, !move.team) else false

                updateCheckData(move.team, isCheck, isCheckMate)
                onAnimationFinished(moveIndex)
            }
        } else {
            animation.nextAnimation!!.onStartCalls += {
                onAnimationStarted()
            }
            animation.nextAnimation!!.onFinishCalls += {
                val isCheck = isPlayerChecked(state, !move.team)
                val isCheckMate = if (isCheck) isPlayerCheckMate(state, !move.team) else false

                updateCheckData(move.team, isCheck, isCheckMate)
                onAnimationFinished(moveIndex)
            }
        }

        if (runInBackground) {
            animation.invokeOnStartCalls()
            animation.invokeOnFinishCalls()

            animation.nextAnimation?.invokeOnStartCalls()
            animation.nextAnimation?.invokeOnFinishCalls()
        } else {
            Logger.debug(TAG, "Queuing animation of ${move.movedPiece} from $fromPosition to $toPosition")
            queueAnimation(animation)
        }
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

            createAnimation(animationSpeed, toPosition, fromPosition, true, takenPiece, takenPiecePosition, {
                onStartUndoMove(move, toPosition, takenPiecePosition)
            }, {
                onFinishUndoMove(move, fromPosition)
            })
        }

        decrementMoveCounter()

        val moveIndex = currentMoveIndex

        if (animation.nextAnimation == null) {
            animation.onStartCalls += {
                onAnimationStarted()
            }
            animation.onFinishCalls += {
                finishMove(move)
                onAnimationFinished(moveIndex)
            }
        } else {
            animation.nextAnimation!!.onStartCalls += {
                onAnimationStarted()
                finishMove(move)
            }
            animation.nextAnimation!!.onFinishCalls += {
                onAnimationFinished(moveIndex)
            }
        }

        if (runInBackground) {
            animation.invokeOnStartCalls()
            animation.invokeOnFinishCalls()

            animation.nextAnimation?.invokeOnStartCalls()
            animation.nextAnimation?.invokeOnFinishCalls()
        } else {
            queueAnimation(animation)
        }
    }

    open fun setMove(move: Move) {
        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

        if (move.movedPiece == PieceType.KING && abs(fromPosition.x - toPosition.x) == 2.0f) {
            val animation = performCastle(move.team, fromPosition, toPosition, 0L)
            animation.invokeOnStartCalls()
            animation.invokeOnFinishCalls()

            animation.nextAnimation?.invokeOnStartCalls()
            animation.nextAnimation?.invokeOnFinishCalls()
        } else {
            state[toPosition] = state[fromPosition]
            state[fromPosition] = null
        }

        if (move.promotedPiece != null) {
            state[toPosition] = Piece(move.promotedPiece, move.team)
        }

        if (move.pieceTaken != null) {
            val takenPosition = move.getTakenPosition(team)!!
            if (takenPosition != toPosition) {
                state[takenPosition] = null
            }
        }

        incrementMoveCounter()
        moves += move

    }

    open fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        possibleMoves.clear()

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")

        val takenPieceData = take(currentPositionPiece, fromPosition, toPosition)

        val takenPiece = takenPieceData?.first
        val takenPiecePosition = takenPieceData?.second

        val animation = if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, animationSpeed)
        } else {
            createAnimation(animationSpeed, fromPosition, toPosition, false, takenPiece, takenPiecePosition)
        }

        if (animation.nextAnimation == null) {
            animation.onStartCalls += {
                onAnimationStarted()
            }
            animation.onFinishCalls += {
                onAnimationFinished(team, currentPositionPiece, fromPosition, toPosition, takenPiecePosition, takenPiece)
                onAnimationFinished(currentMoveIndex)
            }
        } else {
            animation.nextAnimation!!.onStartCalls += {
                onAnimationStarted()
            }
            animation.nextAnimation!!.onFinishCalls += {
                onAnimationFinished(team, currentPositionPiece, fromPosition, toPosition, takenPiecePosition, takenPiece)
                onAnimationFinished(currentMoveIndex)
            }
        }

        Logger.debug(TAG, "Queuing animation of ${currentPositionPiece.type} from $fromPosition to $toPosition")
        queueAnimation(animation)
    }

    open fun onAnimationFinished(team: Team, currentPositionPiece: Piece, fromPosition: Vector2, toPosition: Vector2, takenPiecePosition: Vector2?, takenPiece: Piece?) {
        var promotedPiece: PieceType? = null

        if (takenPiece != null) {
            onPieceTaken(takenPiece.type, takenPiece.team)
        }

        if (currentPositionPiece.type == PieceType.PAWN && (toPosition.y == 0f || toPosition.y == 7f)) {
            promotedPiece = promotePawn(team, toPosition)
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

        lastUpdated = Time.getFullTimeStamp()
        pieceMoving = false

        if (isShowingCurrentMove()) {
            incrementMoveCounter()
        }

        moves += move
        onMoveMade(move)
    }

    private fun finishMove(move: Move) {
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

    private fun promotePawn(team: Team, toPosition: Vector2): PieceType? {
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

    protected fun isPlayerChecked(state: GameState, team: Team): Boolean {
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

    protected fun isPlayerCheckMate(state: GameState, team: Team): Boolean {
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

        val kingAnimation = createAnimation(animationSpeed, toPosition, fromPosition, true)
        val rookAnimation = createAnimation(animationSpeed, oldRookPosition, newRookPosition, true)
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

        val kingAnimation = createAnimation(animationSpeed, fromPosition, toPosition, false)
        val rookAnimation = createAnimation(animationSpeed, oldRookPosition, newRookPosition, false)
        kingAnimation.nextAnimation = rookAnimation

        return kingAnimation
    }

    protected fun updateCheckData(team: Team, isCheck: Boolean, isCheckMate: Boolean) {
        when {
            isCheckMate -> {
//                onCheckMate(team)
            }
            isCheck -> {
                isChecked = true
                board.checkedKingSquare = findKingPosition(state, !team)
            }
            isChecked -> {
                isChecked = false
                board.checkedKingSquare = Vector2(-1f, -1f)
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

    companion object {
        const val TAG = "GameObject"
        const val DEFAULT_ANIMATION_SPEED = 200L
        const val FAST_ANIMATION_SPEED = 50L
    }

}