package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.util.FloatUtils
import com.mjaruijs.fischersplayground.util.Time

class MultiPlayerGame(private val gameId: String, private val playerId: String, val opponentName: String, isPlayingWhite: Boolean, moves: ArrayList<Move> = ArrayList(), val chatMessages: ArrayList<ChatMessage> = arrayListOf()) : Game(isPlayingWhite, moves) {

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

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(team, piece, square, false, state, moves, lookingForCheck)

    fun undoMoves(numberOfMoves: Int) {
        for (i in 0 until numberOfMoves) {
            undoMove(moves.removeLast())
            status = if (status == GameStatus.OPPONENT_MOVE) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
        }
    }

    fun moveOpponent(move: Move, runInBackground: Boolean) {
        if (!runInBackground) {
            if (status != GameStatus.OPPONENT_MOVE) {
                return
            }
        }

        move(move, runInBackground)

        status = GameStatus.PLAYER_MOVE
    }

    private fun movePlayer(move: Move) {
        movePlayer(move.getFromPosition(team), move.getToPosition(team), true)
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean) {
        if (!runInBackground) {
            if (status != GameStatus.PLAYER_MOVE) {
                return
            }
        }

        val move = move(team, fromPosition, toPosition, runInBackground)

        if (!runInBackground) {
            val timeStamp = Time.getFullTimeStamp()
            val positionUpdateMessage = "$gameId|$playerId|${move.toChessNotation()}|$timeStamp"
            val message = Message(Topic.GAME_UPDATE, "move", positionUpdateMessage)

            NetworkManager.sendMessage(message)
        }

        status = GameStatus.OPPONENT_MOVE
    }

    override fun processOnClick(clickedSquare: Vector2): Action {
        if (!isShowingCurrentMove()) {
            return Action.NO_OP
        }

        if (status != GameStatus.PLAYER_MOVE) {
            return Action.NO_OP
        }

        if (board.isASquareSelected()) {
            val previouslySelectedSquare = board.selectedSquare

            if (possibleMoves.contains(clickedSquare)) {
                Thread {
                    movePlayer(previouslySelectedSquare, clickedSquare, false)
                }.start()
                return Action.PIECE_MOVED
            }

            val pieceAtSquare = state[clickedSquare] ?: return Action.NO_OP

            if (pieceAtSquare.team == team) {
                return if (FloatUtils.compare(previouslySelectedSquare, clickedSquare)) Action.SQUARE_DESELECTED else Action.SQUARE_SELECTED
            }
        } else {
            val pieceAtSquare = state[clickedSquare] ?: return Action.NO_OP

            if (pieceAtSquare.team == team) {
                return Action.SQUARE_SELECTED
            }
        }

        return Action.NO_OP
    }

    override fun toString(): String {
        return "$gameId|$lastUpdated|$opponentName|$status|$isPlayingWhite|true"
    }

}