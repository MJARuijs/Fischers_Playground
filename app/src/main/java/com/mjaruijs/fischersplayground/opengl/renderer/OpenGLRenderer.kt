package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.GameState
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(private val context: Context, private val gameId: String, private val isPlayingWhite: Boolean, private var onContextCreated: () -> Unit) : GLSurfaceView.Renderer {

    private lateinit var board: Board
    private lateinit var gameState: GameState

    private lateinit var gameStateRenderer: GameStateRenderer
//    private lateinit var pieceProgram: ShaderProgram

    private var aspectRatio = 1.0f
    private var displayWidth = 0
    private var displayHeight = 0

    override fun onSurfaceCreated(p0: GL10?, config: EGLConfig?) {
        glClearColor(0.25f, 0.25f, 0.25f, 1f)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

//        pieceProgram = ShaderProgram(
//            ShaderLoader.load(R.raw.piece_vertex, ShaderType.VERTEX, context),
//            ShaderLoader.load(R.raw.piece_fragment, ShaderType.FRAGMENT, context)
//        )

        PieceTextures.init(context)

        board = Board(context, ::requestPossibleMoves)
        gameState = GameState(gameId, isPlayingWhite)
        gameStateRenderer = GameStateRenderer(context)

        onContextCreated()
    }

    fun move(fromPosition: Vector2, toPosition: Vector2) {
        gameState.moveOpponent(fromPosition, toPosition)
    }

    private fun requestPossibleMoves(square: Vector2) {
        val possibleMoves = gameState.determinePossibleMoves(square)
        board.updatePossibleMoves(possibleMoves)
    }

    fun update(delta: Float): Boolean {
        return gameStateRenderer.update(delta)
//        return gameState.update(delta)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        displayWidth = width
        displayHeight = height
        aspectRatio = width.toFloat() / height.toFloat()
    }

    fun onTouch(x: Float, y: Float) {
        val clickAction = board.onClick(x, y, displayWidth, displayHeight)
        val boardAction = gameState.processAction(clickAction)

        board.processAction(boardAction)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT)

        board.render(aspectRatio)
        gameStateRenderer.render(gameState, aspectRatio)
//        gameState.draw(pieceProgram, aspectRatio)
    }

    fun destroy() {
//        pieceProgram.destroy()

        board.destroy()
    }
}