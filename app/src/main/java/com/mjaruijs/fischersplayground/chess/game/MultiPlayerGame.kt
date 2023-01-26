package com.mjaruijs.fischersplayground.chess.game

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.mjaruijs.fischersplayground.adapters.gameadapter.GameStatus
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.chess.news.News
import com.mjaruijs.fischersplayground.chess.news.NewsType
import com.mjaruijs.fischersplayground.util.FloatUtils
import com.mjaruijs.fischersplayground.util.Logger

class MultiPlayerGame(val gameId: String, val opponentId: String, val opponentName: String, var status: GameStatus, var opponentStatus: String, lastUpdated: Long, isPlayingWhite: Boolean, var moveToBeConfirmed: String = "", private val savedMoves: ArrayList<Move> = ArrayList(), val chatMessages: ArrayList<ChatMessage> = arrayListOf(), val newsUpdates: ArrayList<News> = arrayListOf()) : Game(isPlayingWhite, lastUpdated), Parcelable {

    @Suppress("DEPRECATION")
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        GameStatus.fromString(parcel.readString()!!),
        parcel.readString()!!,
        parcel.readLong(),
        parcel.readBoolean(),
        parcel.readString()!!
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            parcel.readList(savedMoves, Move::class.java.classLoader)
            parcel.readList(chatMessages, ChatMessage::class.java.classLoader)
            parcel.readList(newsUpdates, News::class.java.classLoader)
        } else {
            parcel.readList(savedMoves, Move::class.java.classLoader, Move::class.java)
            parcel.readList(chatMessages, ChatMessage::class.java.classLoader, ChatMessage::class.java)
            parcel.readList(newsUpdates, News::class.java.classLoader, News::class.java)
        }
    }

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

    fun getNews(topic: NewsType): News? {
        return newsUpdates.find { news -> news.newsType == topic }
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

    override fun setMove(move: Move) {
        super.setMove(move)
        status = if (move.team == team) GameStatus.OPPONENT_MOVE else GameStatus.PLAYER_MOVE
    }

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
        for ((i, move) in savedMoves.withIndex()) {
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

        if (takenPiece != null) {
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

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(gameId)
        parcel.writeString(opponentId)
        parcel.writeString(opponentName)
        parcel.writeString(status.toString())
        parcel.writeString(opponentStatus)
        parcel.writeLong(lastUpdated)
        parcel.writeBoolean(isPlayingWhite)
        parcel.writeString(moveToBeConfirmed)
        parcel.writeList(savedMoves)
        parcel.writeList(chatMessages)
        parcel.writeList(newsUpdates)
    }

    companion object CREATOR : Parcelable.Creator<MultiPlayerGame> {
        override fun createFromParcel(parcel: Parcel): MultiPlayerGame {
            return MultiPlayerGame(parcel)
        }

        override fun newArray(size: Int): Array<MultiPlayerGame?> {
            return arrayOfNulls(size)
        }

        fun parseFromServer(content: String, userId: String): MultiPlayerGame {
            try {
                val data = content.removePrefix("(").removeSuffix(")").split("@#!")
                val gameId = data[0]
                val opponentId = data[1]
                val opponentName = data[2]
                val opponentStatus = data[3]
                val gameStatus = GameStatus.parseFromServer(data[4], userId)
                val lastUpdated = data[5].toLong()
                val whitePlayerId = data[6]
                val moveToBeConfirmed = data[7]
                val moveList = data[8].removePrefix("[").removeSuffix("]").split('\\')
                val chatMessages = data[9].removePrefix("[").removeSuffix("]").split('\\')
                val newsData = data[10].removePrefix("[").removeSuffix("]").split("\\")
                val moves = ArrayList<Move>()

                for (move in moveList) {
                    if (move.isNotBlank()) {
                        moves += Move.fromChessNotation(move)
                    }
                }

                val messages = ArrayList<ChatMessage>()
                for (message in chatMessages) {
                    if (message.isNotBlank()) {
                        val messageData = message.split('~')
                        val senderId = messageData[0]
                        val timeStamp = messageData[1]
                        val messageContent = messageData[2]
                        val type = if (senderId == userId) MessageType.SENT else MessageType.RECEIVED

                        messages += ChatMessage(gameId, timeStamp, messageContent, type)
                    }
                }

                val newsUpdates = ArrayList<News>()
                for (news in newsData) {
                    if (news.isBlank()) {
                        continue
                    }
                    newsUpdates += News.fromString(news)
//                    when (news.count { char -> char == '~' }) {
//                        0 -> newsUpdates += News.fromString(news)
//                        1 -> newsUpdates += IntNews.fromString(news)
//                        else -> newsUpdates += MoveNews.fromString(news)
//                    }
                }

                val isPlayerWhite = whitePlayerId == userId
                val newGame = MultiPlayerGame(gameId, opponentId, opponentName, gameStatus, opponentStatus, lastUpdated, isPlayerWhite, moveToBeConfirmed, moves, messages, newsUpdates)
                newGame.status = gameStatus

                return newGame
            } catch (e: Exception) {
                Logger.error(TAG, e.stackTraceToString())
                throw e
            }

        }
    }

}