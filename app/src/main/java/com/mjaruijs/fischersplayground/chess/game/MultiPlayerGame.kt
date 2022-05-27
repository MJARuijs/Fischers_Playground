package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.chess.Action
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
            undoMove(moves.removeLast(), true)
            status = if (status == GameStatus.OPPONENT_MOVE) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
        }
    }

    fun moveOpponent(move: Move, runInBackground: Boolean) {
        val fromPosition = if (team == Team.WHITE) move.fromPosition else Vector2(7, 7) - move.fromPosition
        val toPosition = if (team == Team.WHITE) move.toPosition else Vector2(7, 7) - move.toPosition

        if (!runInBackground) {
            if (status != GameStatus.OPPONENT_MOVE) {
                return
            }
        }

        println("MOVING OPPONENT: $fromPosition $toPosition")

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val pieceAtNewPosition = state[toPosition]

        move(move, runInBackground)
//        move(!team, fromPosition, toPosition, runInBackground)

//        if (move.promotedPiece != null) {
//            state[move.toPosition] = Piece(move.promotedPiece, move.team)
//        }


//        finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)

        println("MULTIPLAYER MOVING OPPONENT")

        status = GameStatus.PLAYER_MOVE
    }

    private fun movePlayer(move: Move) {
        val fromPosition = if (team == Team.WHITE) move.fromPosition else Vector2(7, 7) - move.fromPosition
        val toPosition = if (team == Team.WHITE) move.toPosition else Vector2(7, 7) - move.toPosition

        movePlayer(fromPosition, toPosition, true)
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2, runInBackground: Boolean) {
        if (!runInBackground) {
            if (status != GameStatus.PLAYER_MOVE) {
                return
            }
        }

        println("MOVING PLAYER $fromPosition $toPosition")

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val pieceAtNewPosition = state[toPosition]

        val move = move(team, fromPosition, toPosition, runInBackground)

//        finishMove(fromPosition, toPosition, currentPositionPiece, pieceAtNewPosition, runInBackground)

//        move.movedPiece = state[toPosition]?.type ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")

        if (!runInBackground) {
            println("MULTIPLAYER FINALIZING MOVE ${pieceAtNewPosition?.type} ")

            val timeStamp = Time.getFullTimeStamp()
            val positionUpdateMessage = "$gameId|$id|${move.toChessNotation()}|$timeStamp"
            val message = Message(Topic.GAME_UPDATE, "move", positionUpdateMessage)

            NetworkManager.sendMessage(message)
        }

        status = GameStatus.OPPONENT_MOVE
    }

    override fun processOnClick(square: Vector2): Action {
        if (!isShowingCurrentMove()) {
            return Action.NO_OP
        }

        if (status != GameStatus.PLAYER_MOVE) {
            return Action.NO_OP
        }

        if (board.isASquareSelected()) {
            val selectedSquare = board.selectedSquare

            if (possibleMoves.contains(square)) {
                Thread {
                    movePlayer(selectedSquare, square, false)
                }.start()
                return Action.PIECE_MOVED
            }

            val pieceAtSquare = state[square] ?: return Action.NO_OP

            if (pieceAtSquare.team == team) {
                return if (FloatUtils.compare(selectedSquare, square)) Action.SQUARE_DESELECTED else Action.SQUARE_SELECTED
            }
        } else {
            val pieceAtSquare = state[square] ?: return Action.NO_OP

            if (pieceAtSquare.team == team) {
                return Action.SQUARE_SELECTED
            }
        }

        return Action.NO_OP
    }

}