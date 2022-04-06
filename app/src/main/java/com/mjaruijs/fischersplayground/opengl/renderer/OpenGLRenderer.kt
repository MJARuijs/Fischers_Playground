package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.gamedata.Board
import com.mjaruijs.fischersplayground.gamedata.GameState
import com.mjaruijs.fischersplayground.gamedata.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var board: Board
    private lateinit var gameState: GameState

    private lateinit var boardProgram: ShaderProgram
    private lateinit var pieceProgram: ShaderProgram

    private var aspectRatio = 1.0f
    private var displayWidth = 0
    private var displayHeight = 0

    override fun onSurfaceCreated(p0: GL10?, config: EGLConfig?) {
        glClearColor(0.25f, 0.25f, 0.25f, 1f)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        boardProgram = ShaderProgram(
            ShaderLoader.load(R.raw.board_vertex, ShaderType.VERTEX, context),
            ShaderLoader.load(R.raw.board_fragment, ShaderType.FRAGMENT, context)
        )

        pieceProgram = ShaderProgram(
            ShaderLoader.load(R.raw.piece_vertex, ShaderType.VERTEX, context),
            ShaderLoader.load(R.raw.piece_fragment, ShaderType.FRAGMENT, context)
        )

        PieceTextures.init(context)

        board = Board(boardProgram, ::requestPossibleMoves)
        gameState = GameState()
    }

    private fun requestPossibleMoves(square: Vector2) {
        val possibleMoves = gameState.determinePossibleMoves(square)
        board.updatePossibleMoves(possibleMoves)
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

        pieceProgram.start()
        pieceProgram.set("aspectRatio", aspectRatio)

        gameState.draw(pieceProgram, aspectRatio)
        pieceProgram.stop()
    }

    fun destroy() {
        boardProgram.destroy()
        pieceProgram.destroy()

        board.destroy()
    }
}