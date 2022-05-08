package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.Game
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(private val context: Context, private val onContextCreated: () -> Unit) : GLSurfaceView.Renderer {


    private lateinit var board: Board
    private lateinit var game: Game

    private lateinit var gameRenderer: GameRenderer
    private lateinit var boardRenderer: BoardRenderer

    private var aspectRatio = 1.0f
    private var displayWidth = 0
    private var displayHeight = 0

    lateinit var onDisplaySizeChanged: (Int, Int) -> Unit

    override fun onSurfaceCreated(p0: GL10?, config: EGLConfig?) {
        glClearColor(0.25f, 0.25f, 0.25f, 1f)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        PieceTextures.createTextureArray()

        gameRenderer = GameRenderer(context)
        boardRenderer = BoardRenderer(context)

        onContextCreated()
    }

    fun setBoard(board: Board) {
        this.board = board
    }

    fun setGameState(game: Game) {
        this.game = game
    }

    fun update(delta: Float): Boolean {
        return gameRenderer.update(delta)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        displayWidth = width
        displayHeight = height
        aspectRatio = width.toFloat() / height.toFloat()

        onDisplaySizeChanged(displayWidth, displayHeight)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT)

        boardRenderer.render(board, aspectRatio)
        gameRenderer.render(game, aspectRatio)
    }

    fun destroy() {
        gameRenderer.destroy()
        boardRenderer.destroy()
    }
}