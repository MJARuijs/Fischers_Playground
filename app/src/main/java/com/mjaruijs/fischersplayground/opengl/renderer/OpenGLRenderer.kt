package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.gamedata.Board
import com.mjaruijs.fischersplayground.gamedata.Piece
import com.mjaruijs.fischersplayground.gamedata.PieceType
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OpenGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private lateinit var board: Board

    private lateinit var boardProgram: ShaderProgram
    private lateinit var pieceProgram: ShaderProgram

    private var aspectRatio = 1.0f
    private var displayWidth = 0
    private var displayHeight = 0

    private val pieces = ArrayList<Piece>()

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

        PieceType.init(context)

        board = Board()

        pieces += Piece(PieceType.WHITE_ROOK, Vector2(0, 0))
        pieces += Piece(PieceType.WHITE_KNIGHT, Vector2(1, 0))
        pieces += Piece(PieceType.WHITE_BISHOP, Vector2(2, 0))
        pieces += Piece(PieceType.WHITE_QUEEN, Vector2(3, 0))
        pieces += Piece(PieceType.WHITE_KING, Vector2(4, 0))
        pieces += Piece(PieceType.WHITE_BISHOP, Vector2(5, 0))
        pieces += Piece(PieceType.WHITE_KNIGHT, Vector2(6, 0))
        pieces += Piece(PieceType.WHITE_ROOK, Vector2(7, 0))

        pieces += Piece(PieceType.BLACK_ROOK, Vector2(0, 7))
        pieces += Piece(PieceType.BLACK_KNIGHT, Vector2(1, 7))
        pieces += Piece(PieceType.BLACK_BISHOP, Vector2(2, 7))
        pieces += Piece(PieceType.BLACK_QUEEN, Vector2(3, 7))
        pieces += Piece(PieceType.BLACK_KING, Vector2(4, 7))
        pieces += Piece(PieceType.BLACK_BISHOP, Vector2(5, 7))
        pieces += Piece(PieceType.BLACK_KNIGHT, Vector2(6, 7))
        pieces += Piece(PieceType.BLACK_ROOK, Vector2(7, 7))

        for (i in 0 until 8) {
            pieces += Piece(PieceType.WHITE_PAWN, Vector2(i, 1))
            pieces += Piece(PieceType.BLACK_PAWN, Vector2(i, 6))
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        displayWidth = width
        displayHeight = height
        aspectRatio = width.toFloat() / height.toFloat()
    }

    fun onTouch(x: Float, y: Float) {
        val scaledX = x / displayWidth
        val scaledY = y / displayHeight

        val selectedPoint = Vector2(scaledX, scaledY) * 2.0f - Vector2(1.0f)

        println("$scaledX $scaledY")
        println(selectedPoint)

        println(pieces[3].scaledPosition)
        println(pieces[3].type)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT)

        boardProgram.start()
        boardProgram.set("aspectRatio", aspectRatio)
        boardProgram.set("outColor", Color(0.25f, 0.25f, 1.0f, 1.0f))
        boardProgram.set("scale", Vector2(0.5f, 0.5f))
        board.draw()
        boardProgram.stop()

        pieceProgram.start()
        pieceProgram.set("aspectRatio", aspectRatio)

        for (piece in pieces) {
            piece.draw(pieceProgram)
        }

        pieceProgram.stop()
    }

    fun destroy() {
        boardProgram.destroy()
        pieceProgram.destroy()

        board.destroy()

        for (piece in pieces) {
            piece.destroy()
        }
    }
}