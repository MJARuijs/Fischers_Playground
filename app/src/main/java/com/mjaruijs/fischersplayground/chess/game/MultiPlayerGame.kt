package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.chess.Action
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.util.FloatUtils

class MultiPlayerGame(val gameId: String, val opponentId: String, val opponentName: String, var status: GameStatus, var opponentStatus: String, lastUpdated: Long, isPlayingWhite: Boolean, moves: ArrayList<Move> = ArrayList(), val chatMessages: ArrayList<ChatMessage> = arrayListOf(), val newsUpdates: ArrayList<News> = arrayListOf()) : Game(isPlayingWhite, lastUpdated, moves) {

    var sendMoveData: (String) -> Unit = {
        println("Multiplayer move was made, but no data was actually sent. Did you forget to set the sendMoveData() function?")
    }

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

    fun addMessage(message: ChatMessage) {
        chatMessages += message
    }

    fun addNews(type: NewsType) {
        newsUpdates += News(type)
    }

    fun addNews(news: News) {
        newsUpdates += news
    }

    fun clearNews(newsType: NewsType) {
        newsUpdates.removeIf { news ->
            news.newsType == newsType
        }
    }

    fun clearAllNews() {
        println("Clearing news")
        newsUpdates.clear()

    }

    override fun getCurrentTeam() = team

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(team, piece, square, false, state, moves, lookingForCheck)

    fun undoMoves(numberOfMoves: Int) {
        for (i in 0 until numberOfMoves) {
            if (moves.isNotEmpty()) {
                undoMove(moves.removeLast())
            }
            status = if (status == GameStatus.OPPONENT_MOVE) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
        }
    }

    fun moveOpponent(move: Move, runInBackground: Boolean) {
        if (!runInBackground) {
            if (status != GameStatus.OPPONENT_MOVE) {
                println("RETURNING BECAUSE NOT OPPONENTS MOVE")
                return
            }
        }

//        move(move, runInBackground)
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

        status = GameStatus.OPPONENT_MOVE

        val move = move(team, fromPosition, toPosition, runInBackground)

        if (!runInBackground) {
            val timeStamp = lastUpdated
            val positionUpdateMessage = "${move.toChessNotation()}|$timeStamp"

            sendMoveData(positionUpdateMessage)
        }
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

//    override fun toString(): String {
//        return "$gameId|$lastUpdated|$opponentName|$status|$isPlayingWhite|true"
//    }

}