package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.ActionType
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
import com.mjaruijs.fischersplayground.util.FloatUtils
import com.mjaruijs.fischersplayground.util.Time

class MultiPlayerGame(private val gameId: String, private val id: String, val opponentName: String, val isPlayingWhite: Boolean, moves: ArrayList<Move> = ArrayList(), val chatMessages: ArrayList<ChatMessage> = arrayListOf()) : Game(isPlayingWhite, moves) {

    var status: GameStatus
    var news = News(NewsType.NO_NEWS)

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

        for (move in moves) {
            if (move.team == team) {
                movePlayer(move)
            } else {
                moveOpponent(move, true)
            }
        }
    }

    override fun getCurrentTeam() = team

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(this.team, piece, square, false, state, moves, lookingForCheck)

    fun reverseMoves(numberOfMoves: Int) {
        for (i in 0 until numberOfMoves) {
            undoMove(moves.removeLast())
            status = if (status == GameStatus.OPPONENT_MOVE) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
        }
    }

    fun moveOpponent(move: Move, runInBackground: Boolean): Move? {
        val fromPosition = if (team == Team.WHITE) move.fromPosition else Vector2(7, 7) - move.fromPosition
        val toPosition = if (team == Team.WHITE) move.toPosition else Vector2(7, 7) - move.toPosition

        return moveOpponent(fromPosition, toPosition, runInBackground)
    }

    private fun moveOpponent(fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean): Move? {
        if (!runInBackground) {
            if (status != GameStatus.OPPONENT_MOVE) {
                return null
            }
        }

        println("MOVING OPPONENT: $fromPosition $toPosition")

        val move = move(!team, fromPosition, toPosition, runInBackground)

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

        return movePlayer(fromPosition, toPosition, true)
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean): Move? {
        if (!runInBackground) {
            if (status != GameStatus.PLAYER_MOVE) {
                return null
            }
        }

        println("MOVING PLAYER $fromPosition $toPosition")

        val move = move(team, fromPosition, toPosition, runInBackground)

        if (move.movedPiece == PieceType.PAWN) {
            if (toPosition.y == 7.0f) {
                state[toPosition] = Piece(PieceType.QUEEN, team)
            }
        }

        if (!runInBackground) {
            val timeStamp = Time.getFullTimeStamp()
            val positionUpdateMessage = "$gameId|$id|${move.toChessNotation()}|$timeStamp"
            val message = Message(Topic.GAME_UPDATE, "move", positionUpdateMessage)

            NetworkManager.sendMessage(message)
        }

        status = GameStatus.OPPONENT_MOVE
        return move
    }

    override fun processAction(action: Action): Action {
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
                    movePlayer(action.previouslySelectedPosition, action.clickedPosition, false)
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

}