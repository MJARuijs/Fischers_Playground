package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.news.News
import com.mjaruijs.fischersplayground.news.NewsType
import com.mjaruijs.fischersplayground.opengl.renderer.AnimationData
import com.mjaruijs.fischersplayground.util.FloatUtils
import kotlin.math.abs
import kotlin.math.roundToInt

class Game(private val gameId: String, private val id: String, val opponentName: String, val isPlayingWhite: Boolean, private var moves: ArrayList<Move> = ArrayList()) {

    private val state = GameState(isPlayingWhite)

    private val takenPieces = ArrayList<Piece>()
    private val piecesTakenByOpponent = ArrayList<Piece>()

    private var possibleMoves = ArrayList<Vector2>()
    private var currentMoveIndex = if (moves.isEmpty()) -1 else moves.size - 1

    private val animationData = ArrayList<AnimationData>()
//    private var animationData: AnimationData? = null

    val team = if (isPlayingWhite) Team.WHITE else Team.BLACK

    var status: GameStatus
    var news = News(NewsType.NO_NEWS)

    private var initialized = false

    init {
        status = if (moves.isEmpty()) {
            if (isPlayingWhite) {
                GameStatus.PLAYER_MOVE
            } else {
                GameStatus.OPPONENT_MOVE
            }
        } else {
            val lastMove = moves.last()
            if (lastMove.team == team) {
                GameStatus.OPPONENT_MOVE
            } else {
                GameStatus.PLAYER_MOVE
            }
        }
    }

    fun init() {
        if (initialized) {
            return
        }
        println("INITTING")
        for (move in moves) {
            if (move.team == team) {
                movePlayer(move)
            } else {
                moveOpponent(move, false)
            }
        }

        initialized = true
    }

    operator fun get(i: Int, j: Int): Piece? {
        return state[i, j]
    }

    private fun isShowingCurrentMove(): Boolean {
        return if (moves.isEmpty()) {
            true
        } else {
            currentMoveIndex == moves.size - 1
        }
    }

    fun reverseMoves(numberOfMoves: Int) {
        for (i in 0 until numberOfMoves) {
            undoMove(moves.removeLast())
            status = if (status == GameStatus.OPPONENT_MOVE) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
        }
    }

    fun showPreviousMove(): Pair<Boolean, Boolean> {
        if (currentMoveIndex == -1) {
            return Pair(first = true, second = false)
        }

        val currentMove = moves[currentMoveIndex]
        undoMove(currentMove)

        val shouldDisableBackButton = currentMoveIndex == -1
        val shouldEnableForwardButton = currentMoveIndex == moves.size - 2

        return Pair(shouldDisableBackButton, shouldEnableForwardButton)
    }

    fun showNextMove(): Pair<Boolean, Boolean> {
        if (currentMoveIndex >= moves.size - 1) {
            return Pair(first = true, second = false)
        }

        val nextMove = moves[++currentMoveIndex]
        redoMove(nextMove)

        val shouldDisableForwardButton = currentMoveIndex == moves.size - 1
        val shouldEnableBackButton = currentMoveIndex == 0

        return Pair(shouldDisableForwardButton, shouldEnableBackButton)
    }

    fun getAnimationData() = animationData

    fun resetAnimationData() {
        animationData.clear()
    }

    fun moveOpponent(move: Move, shouldAnimate: Boolean): Move? {
        val fromPosition = if (team == Team.WHITE) move.fromPosition else Vector2(7, 7) - move.fromPosition
        val toPosition = if (team == Team.WHITE) move.toPosition else Vector2(7, 7) - move.toPosition

        return moveOpponent(fromPosition, toPosition, shouldAnimate)
    }

    private fun moveOpponent(fromPosition: Vector2, toPosition: Vector2, shouldAnimate: Boolean): Move? {
        if (shouldAnimate) {
            if (status != GameStatus.OPPONENT_MOVE) {
                return null
            }
        }

        val move = move(!team, fromPosition, toPosition, shouldAnimate)

        if (move.movedPiece == PieceType.PAWN) {
            if (toPosition.y == 0.0f) {
                state[toPosition] = Piece(PieceType.QUEEN, !team)
            }
        }

        status = GameStatus.PLAYER_MOVE
        return move
    }

    private fun movePlayer(move: Move): Move? {
        val fromPosition = if (team == Team.WHITE) move.fromPosition else Vector2(7, 7) - move.fromPosition
        val toPosition = if (team == Team.WHITE) move.toPosition else Vector2(7, 7) - move.toPosition

        return movePlayer(fromPosition, toPosition, false)
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2, shouldAnimate: Boolean): Move? {
        if (shouldAnimate) {
            if (status != GameStatus.PLAYER_MOVE) {
                return null
            }
        }

        val move = move(team, fromPosition, toPosition, shouldAnimate)

        if (move.movedPiece == PieceType.PAWN) {
            if (toPosition.y == 7.0f) {
                state[toPosition] = Piece(PieceType.QUEEN, team)
            }
        }

        if (shouldAnimate) {
            val positionUpdateMessage = "$gameId|$id|${move.toChessNotation()}"
            val message = Message(Topic.GAME_UPDATE, "move", positionUpdateMessage)

            NetworkManager.sendMessage(message)
        }

        status = GameStatus.OPPONENT_MOVE
        return move
    }

    private fun move(team: Team, fromPosition: Vector2, toPosition: Vector2, shouldAnimate: Boolean): Move {
        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val pieceAtNewPosition = state[toPosition]

        val takenPiece = take(currentPositionPiece, fromPosition, toPosition)

        if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(team, fromPosition, toPosition, shouldAnimate)
        } else {
            state[toPosition] = currentPositionPiece
            state[fromPosition] = null

            if (shouldAnimate) {
                setAnimationData(fromPosition, toPosition)
            }
        }

        possibleMoves.clear()

        val isCheck = isPlayerChecked(state, team)

        val isCheckMate = if (!isCheck) {
            false
        } else {
            isPlayerCheckMate(state, !team)
        }

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

    private fun redoMove(move: Move) {
        val fromPosition = if (team == Team.WHITE) move.fromPosition else Vector2(7, 7) - move.fromPosition
        val toPosition = if (team == Team.WHITE) move.toPosition else Vector2(7, 7) - move.toPosition

        val piece = Piece(move.movedPiece, move.team)

        state[toPosition] = piece
        state[fromPosition] = null

        setAnimationData(fromPosition, toPosition)
    }

    private fun undoMove(move: Move) {
        val fromPosition = move.toPosition
        val toPosition = move.fromPosition

        val piece = Piece(move.movedPiece, move.team)

        state[toPosition] = piece

        if (move.pieceTaken == null) {
            state[fromPosition] = null
        } else {
            state[fromPosition] = Piece(move.pieceTaken, !move.team)
        }

        setAnimationData(fromPosition, toPosition)
        
        currentMoveIndex--
    }

    private fun setAnimationData(fromPosition: Vector2, toPosition: Vector2, onAnimationFinished: () -> Unit = {}) {
        animationData += AnimationData(fromPosition, toPosition, onAnimationFinished)
    }

    fun processAction(action: Action): Action {
        if (!isShowingCurrentMove()) {
            println("NOT SHOWING CURRENT MOVE $currentMoveIndex ${moves.size}")
            return Action(action.clickedPosition, ActionType.NO_OP)
        }

        if (status != GameStatus.PLAYER_MOVE) {
            return Action(action.clickedPosition, ActionType.NO_OP)
        }

        if (action.type == ActionType.SQUARE_DESELECTED) {
            return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED) // Square deselected
        }

        if (action.type == ActionType.SQUARE_SELECTED) {
            // No piece has been selected yet
            if (action.previouslySelectedPosition == null || action.previouslySelectedPosition.x == -1.0f || action.previouslySelectedPosition.y == -1.0f) {
                val piece = state[action.clickedPosition] ?: return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)

                // Select a piece now, if the piece belongs to the team who's turn it is
                if (piece.team == team) {
                    return Action(action.clickedPosition, ActionType.SQUARE_SELECTED)
                }
            } else { // A piece is already selected

                // If the newly selected square belongs to the possible moves of the selected piece, we can move to that new square
                if (possibleMoves.contains(action.clickedPosition)) {
                    movePlayer(action.previouslySelectedPosition, action.clickedPosition, true)
                    return Action(action.clickedPosition, ActionType.PIECE_MOVED)
                }

                val currentlySelectedPiece = state[action.clickedPosition] ?: return Action(action.clickedPosition, ActionType.NO_OP)

                if (currentlySelectedPiece.team == team) {
                    if (FloatUtils.compare(action.clickedPosition, action.previouslySelectedPosition)) {
                        return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)
                    }
                    return Action(action.clickedPosition, ActionType.SQUARE_SELECTED)
                }

                return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)
            }
        }

        return Action(action.clickedPosition, ActionType.NO_OP)
    }

    fun determinePossibleMoves(square: Vector2, team: Team): ArrayList<Vector2> {
        val piece = state[square] ?: return arrayListOf()

        possibleMoves = PieceType.getPossibleMoves(team, piece, square, state, moves)
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
                    possibleMovesForOpponent += PieceType.getPossibleMoves(this.team, piece, Vector2(x, y), state, moves)
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

    private fun performCastle(team: Team, fromPosition: Vector2, toPosition: Vector2, shouldAnimate: Boolean) {
        state[toPosition] = state[fromPosition]

        val rookDirection = if (toPosition.x < fromPosition.x) 1 else -1

        val oldRookPosition = if (toPosition.x > fromPosition.x) {
            if (this.team == team) Vector2(7, 0) else Vector2(7, 7)
        } else {
            if (this.team == team) Vector2(0, 0) else Vector2(0, 7)
        }

        val newRookPosition = toPosition + Vector2(rookDirection, 0)

        println("${state[oldRookPosition]} $oldRookPosition")
//        println("${state[rookPosition]} $rookPosition")
//        state[toPosition + Vector2(1 * rookDirection, 0)] = state[rookPosition]
//        state[rookPosition] = null

        state[fromPosition] = null
        state[newRookPosition] = state[oldRookPosition]
        state[oldRookPosition] = null

        if (shouldAnimate) {
            setAnimationData(fromPosition, toPosition) {

            }
            setAnimationData(oldRookPosition, newRookPosition) {
                state[oldRookPosition] = null
            }
        }
    }
}