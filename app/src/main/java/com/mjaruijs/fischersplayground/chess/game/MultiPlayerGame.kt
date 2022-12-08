package com.mjaruijs.fischersplayground.chess.game

import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.util.FloatUtils

class MultiPlayerGame(val gameId: String, val opponentId: String, val opponentName: String, var status: GameStatus, var opponentStatus: String, lastUpdated: Long, isPlayingWhite: Boolean, var moveToBeConfirmed: String = "", private val savedMoves: ArrayList<Move> = ArrayList(), val chatMessages: ArrayList<ChatMessage> = arrayListOf(), val newsUpdates: ArrayList<News> = arrayListOf()) : Game(isPlayingWhite, lastUpdated) {

    init {
        status = if (savedMoves.isEmpty()) {
            if (isPlayingWhite) {
                GameStatus.PLAYER_MOVE
            } else {
                GameStatus.OPPONENT_MOVE
            }
        } else {
            val lastMove = savedMoves.last()
            if (lastMove.team == team) {
                GameStatus.OPPONENT_MOVE
            } else {
                GameStatus.PLAYER_MOVE
            }
        }

        restoreMoves()
    }

    fun isFinished(): Boolean {
        return status == GameStatus.GAME_WON || status == GameStatus.GAME_DRAW || status == GameStatus.GAME_LOST
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

    fun hasNews(topic: NewsType): Boolean {
        return newsUpdates.any { news -> news.newsType == topic }
    }

    fun clearNews(newsType: NewsType) {
        newsUpdates.removeIf { news ->
            news.newsType == newsType
        }
    }

    fun clearAllNews() {
        newsUpdates.clear()
    }

    override fun getCurrentTeam() = team

    override fun getPieceMoves(piece: Piece, square: Vector2, state: GameState, lookingForCheck: Boolean) = PieceType.getPossibleMoves(team, piece, square, false, state, moves.subList(0, currentMoveIndex + 1), lookingForCheck)

    fun undoMoves(numberOfMoves: Int) {
        for (i in 0 until numberOfMoves) {
            if (moves.isNotEmpty()) {
                undoMove(moves.removeLast(), false)
            }
            status = if (status == GameStatus.OPPONENT_MOVE) GameStatus.PLAYER_MOVE else GameStatus.OPPONENT_MOVE
        }

        if (currentMoveIndex == -1) {
            disableBackButton()
        }
    }

    fun hasPendingMove() = moveToBeConfirmed.isNotEmpty()

    fun confirmMove() {
        status = GameStatus.OPPONENT_MOVE
        moveToBeConfirmed = ""
    }

    fun cancelMove() {
        undoMove(moves.removeLast(), false)
        status = GameStatus.PLAYER_MOVE

        if (moves.size == 0) {
            disableBackButton()
        }
        moveToBeConfirmed = ""
    }

    private fun restoreMoves() {
        for (move in savedMoves) {
            restoreMove(move)
        }
    }

    private fun restoreMove(move: Move) {
        moves += move
        status = if (move.team == team) GameStatus.OPPONENT_MOVE else GameStatus.PLAYER_MOVE

        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")

        val takenPieceData = take(currentPositionPiece, fromPosition, toPosition)

        val takenPiece = takenPieceData?.first
        val takenPiecePosition = takenPieceData?.second

        if (takenPiece != null && move != savedMoves.last()) {
            takenPieces += takenPiece
        }

        val animation = if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(move.team, fromPosition, toPosition, 0L)
        } else {
            createAnimation(0L, fromPosition, toPosition, false, takenPiece, takenPiecePosition, {}, {
                if (move.promotedPiece != null) {
                    state[toPosition] = Piece(move.promotedPiece, move.team)
                }
            })
        }

        if (animation.nextAnimation == null) {
            animation.onFinishCalls += {
                val isCheck = isPlayerChecked(state, team)
                val isCheckMate = if (isCheck) isPlayerCheckMate(state, team) else false

                updateCheckData(move.team, isCheck, isCheckMate)

                currentMoveIndex++
            }
        } else {
            animation.nextAnimation!!.onFinishCalls += {
                val isCheck = isPlayerChecked(state, team)
                val isCheckMate = if (isCheck) isPlayerCheckMate(state, team) else false

                updateCheckData(move.team, isCheck, isCheckMate)

                currentMoveIndex++
            }
        }

        animation.invokeOnStartCalls()
        animation.invokeOnFinishCalls()

        animation.nextAnimation?.invokeOnStartCalls()
        animation.nextAnimation?.invokeOnFinishCalls()
    }

    fun moveOpponent(move: Move, animationSpeed: Long = DEFAULT_ANIMATION_SPEED) {
        if (status != GameStatus.OPPONENT_MOVE) {
            return
        }

        if (isShowingCurrentMove()) {
            incrementMoveCounter()
        }

        moves += move
        status = GameStatus.PLAYER_MOVE
        onMoveMade(move)

        if (!isShowingCurrentMove()) {
            return
        }

        possibleMoves.clear()

        val fromPosition = move.getFromPosition(team)
        val toPosition = move.getToPosition(team)

        val currentPositionPiece = state[fromPosition] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")

        val takenPieceData = take(currentPositionPiece, fromPosition, toPosition)

        val takenPiece = takenPieceData?.first
        val takenPiecePosition = takenPieceData?.second

        val animation = if (isCastling(currentPositionPiece, fromPosition, toPosition)) {
            performCastle(move.team, fromPosition, toPosition, animationSpeed)
        } else {
            createAnimation(animationSpeed, fromPosition, toPosition, false, takenPiece, takenPiecePosition, {}, {
                if (move.promotedPiece != null) {
                    state[toPosition] = Piece(move.promotedPiece, move.team)
                }
                if (takenPiecePosition != null) {
                    onPieceTaken(move.pieceTaken!!, !move.team)

                    if (takenPiecePosition != toPosition) {
                        state[takenPiecePosition] = null
                    }
                }
            })
        }

        if (animation.nextAnimation == null) {
            animation.onFinishCalls += {
                val isCheck = isPlayerChecked(state, team)
                val isCheckMate = if (isCheck) isPlayerCheckMate(state, team) else false

                updateCheckData(move.team, isCheck, isCheckMate)
            }
        } else {
            animation.nextAnimation!!.onFinishCalls += {
                val isCheck = isPlayerChecked(state, team)
                val isCheckMate = if (isCheck) isPlayerCheckMate(state, team) else false

                updateCheckData(move.team, isCheck, isCheckMate)
            }
        }

        queueAnimation(animation)
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2) {
        if (status != GameStatus.PLAYER_MOVE) {
            return
        }

        move(team, fromPosition, toPosition)
        status = GameStatus.OPPONENT_MOVE
    }

    override fun processOnClick(clickedSquare: Vector2) {
        if (!isShowingCurrentMove()) {
            return
        }

        if (status != GameStatus.PLAYER_MOVE) {
            return
        }

        if (board.isASquareSelected()) {
            val previouslySelectedSquare = board.selectedSquare

            if (possibleMoves.contains(clickedSquare)) {
                Thread {
                    movePlayer(previouslySelectedSquare, clickedSquare)
                }.start()
                board.deselectSquare()
                return
            }

            val pieceAtSquare = state[clickedSquare] ?: return

            if (pieceAtSquare.team == team) {
                if (FloatUtils.compare(previouslySelectedSquare, clickedSquare)) {
                    board.deselectSquare()
                } else {
                    board.selectSquare(clickedSquare)
                }
            }
        } else {
            val pieceAtSquare = state[clickedSquare] ?: return

            if (pieceAtSquare.team == team) {
                board.selectSquare(clickedSquare)
            }
        }
    }

    override fun processOnLongClick(clickedSquare: Vector2) {

    }
}