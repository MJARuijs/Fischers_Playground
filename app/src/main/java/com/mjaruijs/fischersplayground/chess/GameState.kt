package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.Preferences
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.renderer.AnimationData
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.util.FloatUtils
import kotlin.math.abs
import kotlin.math.roundToInt

class GameState(private val gameId: String, private val isPlayingWhite: Boolean) {

//    private val quad = Quad()
//    private val sampler = Sampler(0)

    val state = ArrayList<ArrayList<Piece?>>()
    private var possibleMoves = ArrayList<Vector2>()

    private val team = if (isPlayingWhite) Team.WHITE else Team.BLACK
    private val id = Preferences.get("id")

    private var currentlyMyMove = isPlayingWhite

    private var animationData: AnimationData? = null

//    private var translationOffset = Vector2()
//    private var transitionDistance = Vector2()
//    private var animatingCol = -1
//    private var animatingRow = -1
//
//    private var stopAnimating = false

    init {
        initBoard()
    }

    operator fun get(i: Int, j: Int): Piece? {
        return state[i][j]
    }

    fun getAnimationData() = animationData

    fun resetAnimationData() {
        animationData = null
    }

    fun moveOpponent(fromPosition: Vector2, toPosition: Vector2) {
        if (currentlyMyMove) {
            return
        }

        val invertedFromPosition = Vector2(7, 7) - fromPosition
        val invertedToPosition = Vector2(7, 7) - toPosition

        val pieceType = move(invertedFromPosition, invertedToPosition)

        if (pieceType == PieceType.PAWN) {
            if (invertedToPosition.y == 0.0f) {
                state[invertedToPosition.x.roundToInt()][invertedToPosition.y.roundToInt()] = Piece(PieceType.QUEEN, !team)
            }
        }

        currentlyMyMove = !currentlyMyMove
    }

    private fun movePlayer(fromPosition: Vector2, toPosition: Vector2) {
        if (!currentlyMyMove) {
            return
        }

        val pieceType = move(fromPosition, toPosition)

        if (pieceType == PieceType.PAWN) {
            if (toPosition.y == 7.0f) {
                state[toPosition.x.roundToInt()][toPosition.y.roundToInt()] = Piece(PieceType.QUEEN, team)
            }
        }

        val positionUpdateMessage = "$gameId|$id|$fromPosition|$toPosition"
        val message = Message(Topic.GAME_UPDATE, positionUpdateMessage)

        println("SENDING MOVE: $message")

        NetworkManager.sendMessage(message)

        currentlyMyMove = !currentlyMyMove
    }

    private fun move(fromPosition: Vector2, toPosition: Vector2): PieceType {
        val currentPositionPiece = state[fromPosition.x.roundToInt()][fromPosition.y.roundToInt()] ?: throw IllegalArgumentException("Could not find a piece at square: $fromPosition")
        val newPositionPiece = state[toPosition.x.roundToInt()][toPosition.y.roundToInt()]

        state[toPosition.x.roundToInt()][toPosition.y.roundToInt()] = currentPositionPiece
        state[fromPosition.x.roundToInt()][fromPosition.y.roundToInt()] = null

        setAnimationData(fromPosition, toPosition)
//        animateMove(fromPosition, toPosition)

//        isPlayerChecked(teamToMove)
        possibleMoves.clear()

        return currentPositionPiece.type
    }

    private fun setAnimationData(fromPosition: Vector2, toPosition: Vector2) {
        animationData = AnimationData(fromPosition, toPosition)
    }

//    fun animateMove(fromPosition: Vector2, toPosition: Vector2) {
//        animatingRow = toPosition.x.roundToInt()
//        animatingCol = toPosition.y.roundToInt()
//
//        translationOffset = Vector2(fromPosition - toPosition)
//
//        transitionDistance = toPosition - fromPosition
//        println("ANIMATING: $fromPosition $toPosition ::: ${translationOffset} ${transitionDistance}")
//    }
//
//    fun update(delta: Float): Boolean {
//        val increment = transitionDistance * delta * 5.0f
//
//        translationOffset += increment
//
//        if (abs(translationOffset.x) < abs(increment.x / 5.0f) || abs(translationOffset.y) < abs(increment.y / 5.0f)) {
//            println("DONE ANIMATING")
//            stopAnimating = true
//        }
//
//        return animatingCol != -1
//    }

    fun processAction(action: Action): Action {
        if (!currentlyMyMove) {
            return Action(action.clickedPosition, ActionType.NO_OP)
        }

        println(action)

        if (action.type == ActionType.SQUARE_DESELECTED) {
            return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED) // Square deselected
        }

        if (action.type == ActionType.SQUARE_SELECTED) {
            // No piece has been selected yet
            if (action.previouslySelectedPosition == null || action.previouslySelectedPosition.x == -1.0f || action.previouslySelectedPosition.y == -1.0f) {
                val piece = state[action.clickedPosition.x.roundToInt()][action.clickedPosition.y.roundToInt()] ?: return Action(action.clickedPosition, ActionType.SQUARE_DESELECTED)

                // Select a piece now, if the piece belongs to the team who's turn it is
                if (piece.team == team) {
                    return Action(action.clickedPosition, ActionType.SQUARE_SELECTED)
                }
            } else { // A piece is already selected

                // If the newly selected square belongs to the possible moves of the selected piece, we can move to that new square
                if (possibleMoves.contains(action.clickedPosition)) {
                    movePlayer(action.previouslySelectedPosition, action.clickedPosition)
                    return Action(action.clickedPosition, ActionType.PIECE_MOVED)
                }

                val currentlySelectedPiece = state[action.clickedPosition.x.roundToInt()][action.clickedPosition.y.roundToInt()] ?: return Action(action.clickedPosition, ActionType.NO_OP)

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

    fun isPlayerChecked(team: Team): Boolean {
        val kingsPosition = Vector2(-1f, -1f)
        var king: Piece? = null

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x][y] ?: continue
                if (piece.type == PieceType.KING && piece.team == team) {
                    king = piece
                    kingsPosition.x = x.toFloat()
                    kingsPosition.y = y.toFloat()
                    break
                }
            }
        }

        val possibleMovesForOpponent = ArrayList<Vector2>()

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = state[x][y] ?: continue

                if (piece.team != team) {
                    possibleMovesForOpponent += PieceType.getPossibleMoves(piece, Vector2(x, y), state)
                }
            }
        }

        if (possibleMovesForOpponent.contains(kingsPosition)) {
            println("CHECKED")
        } else {
            println("NOT CHECKED")
        }

//        val possibleMovesForKing = PieceType.getPossibleMoves(king!!, kingsPosition, state)

        return false
    }

    fun determinePossibleMoves(square: Vector2): ArrayList<Vector2> {
        val piece = state[square.x.roundToInt()][square.y.roundToInt()] ?: return arrayListOf()
        possibleMoves = PieceType.getPossibleMoves(piece, square, state)
        return possibleMoves
    }

//    fun draw(shaderProgram: ShaderProgram, aspectRatio: Float) {
//        shaderProgram.start()
//        shaderProgram.set("aspectRatio", aspectRatio)
//        sampler.bind(PieceTextures.getTextureArray())
//
//        for (row in 0 until 8) {
//            for (col in 0 until 8) {
//                val piece = state[row][col] ?: continue
//
//                val translation = if (row == animatingRow && col == animatingCol) {
//                    (Vector2((row + translationOffset.x) * aspectRatio * 2.0f, (col + translationOffset.y) * aspectRatio * 2.0f) / 8.0f) + Vector2(-aspectRatio, aspectRatio / 4.0f - aspectRatio)
//                } else {
//                    (Vector2(row * aspectRatio * 2.0f, col * aspectRatio * 2.0f) / 8.0f) + Vector2(-aspectRatio, aspectRatio / 4.0f - aspectRatio)
//                }
//
//                shaderProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 4.0f)
//                shaderProgram.set("textureId", piece.textureId.toFloat())
//                shaderProgram.set("textureMaps", sampler.index)
//                shaderProgram.set("translation", translation)
//                quad.draw()
//            }
//        }
//
//        if (stopAnimating) {
//            animatingRow = -1
//            animatingCol = -1
//            translationOffset = Vector2()
//            transitionDistance = Vector2()
//            stopAnimating = false
//        }
//
//        shaderProgram.stop()
//    }

    private fun initBoard() {
        for (row in 0 until 8) {
            state += ArrayList<Piece?>()

            for (col in 0 until 8) {
                state[row] += null
            }
        }

        val whiteIndex = if (isPlayingWhite) 0 else 7
        val blackIndex = if (isPlayingWhite) 7 else 0
        val whitePawnIndex = if (isPlayingWhite) 1 else 6
        val blackPawnIndex = if (isPlayingWhite) 6 else 1

        val queenOffset = if (isPlayingWhite) 0 else 1

        state[0][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)
        state[1][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
        state[2][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
        state[3 + queenOffset][whiteIndex] = Piece(PieceType.QUEEN, Team.WHITE)
        state[4 - queenOffset][whiteIndex] = Piece(PieceType.KING, Team.WHITE)
        state[5][whiteIndex] = Piece(PieceType.BISHOP, Team.WHITE)
        state[6][whiteIndex] = Piece(PieceType.KNIGHT, Team.WHITE)
        state[7][whiteIndex] = Piece(PieceType.ROOK, Team.WHITE)

        state[0][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)
        state[1][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
        state[2][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
        state[3 + queenOffset][blackIndex] = Piece(PieceType.QUEEN, Team.BLACK)
        state[4 - queenOffset][blackIndex] = Piece(PieceType.KING, Team.BLACK)
        state[5][blackIndex] = Piece(PieceType.BISHOP, Team.BLACK)
        state[6][blackIndex] = Piece(PieceType.KNIGHT, Team.BLACK)
        state[7][blackIndex] = Piece(PieceType.ROOK, Team.BLACK)

        for (i in 0 until 8) {
            state[i][whitePawnIndex] = Piece(PieceType.PAWN, Team.WHITE)
            state[i][blackPawnIndex] = Piece(PieceType.PAWN, Team.BLACK)
        }
    }

    override fun toString(): String {
        var string = ""

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state[row][col]
                string += "${piece?.type}"

                if (col == 7) {
                    string += ","
                }
            }
            string += "\n"
        }
        return string
    }

    companion object {

//        fun fromString(content: String): GameState {
//            val lines = content.split("\n")
//
//        }
    }
}