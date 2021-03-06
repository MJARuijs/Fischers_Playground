package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.CAMERA_ZOOM_KEY
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.Camera.Companion.DEFAULT_ZOOM
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI

class OpenGLRenderer(private val context: Context, private val onContextCreated: () -> Unit, private var is3D: Boolean) : GLSurfaceView.Renderer {

    private lateinit var board: Board
    private lateinit var game: Game

    private lateinit var boardRenderer: BoardRenderer
    private lateinit var pieceRenderer: PieceRenderer
    private lateinit var highlightRenderer: HighlightRenderer

    private val camera = Camera()

    private var displayWidth = 0
    private var displayHeight = 0

    private var pixelsRequested = false

    var isPlayerWhite = true

    var onDraw: () -> Unit = {}
    var onPixelsRead: (ByteBuffer) -> Unit = {}
    var onDisplaySizeChanged: (Int, Int) -> Unit = { _, _ -> }

    override fun onSurfaceCreated(p0: GL10?, config: EGLConfig?) {
        glClearColor(0.25f, 0.25f, 0.25f, 1f)
        glEnable(GL_BLEND)
        setOpenGLSettings()

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        PieceTextures.createTextureArrays()

        pieceRenderer = PieceRenderer(context, isPlayerWhite)
        boardRenderer = BoardRenderer(context)
        highlightRenderer = HighlightRenderer(context)

        onContextCreated()

        val preferences = context.getSharedPreferences("graphics_preferences", AppCompatActivity.MODE_PRIVATE)
        camera.setZoom(preferences.getFloat(CAMERA_ZOOM_KEY, DEFAULT_ZOOM))
    }

    fun setR(r: Float) {
//        gameRenderer3D.rChannel = r
//        println("r=${gameRenderer3D.rChannel}, g=${gameRenderer3D.gChannel}, b=${gameRenderer3D.bChannel}")
    }

    fun setG(g: Float) {
//        gameRenderer3D.gChannel = g
//        println("r=${gameRenderer3D.rChannel}, g=${gameRenderer3D.gChannel}, b=${gameRenderer3D.bChannel}")
    }

    fun setB(b: Float) {
//        gameRenderer3D.bChannel = b
//        println("r=${gameRenderer3D.rChannel}, g=${gameRenderer3D.gChannel}, b=${gameRenderer3D.bChannel}")
    }

    fun set3D(is3D: Boolean) {
        this.is3D = is3D

        if (this::board.isInitialized) {
            board.is3D = is3D
        }

        setOpenGLSettings()
    }

    private fun setOpenGLSettings()  {
        if (is3D) {
            glEnable(GL_CULL_FACE)
            glEnable(GL_DEPTH_TEST)
        } else {
            glDisable(GL_CULL_FACE)
            glDisable(GL_DEPTH_TEST)
        }
    }

    fun setPieceScale(scale: Float) {
        pieceRenderer.pieceScale = Vector3(scale, scale, scale)
    }

    fun setGame(game: Game) {
        this.game = game
        this.board = game.board

        board.is3D = is3D
    }

    fun update(delta: Float): Boolean {
        return if (this::game.isInitialized) {
            Thread {
                pieceRenderer.startAnimations(game)
            }.start()
            pieceRenderer.update(delta)
        } else {
            false
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        displayWidth = width
        displayHeight = height

        onDisplaySizeChanged(displayWidth, displayHeight)
    }

    var renderCircle = false

    override fun onDrawFrame(p0: GL10?) {
        if (!this::game.isInitialized) {
            return
        }

        if (is3D) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            boardRenderer.render3D(board, camera, displayWidth, displayHeight)
            highlightRenderer.renderSelectedSquares3D(board, camera)
            pieceRenderer.render3D(game, camera)
            highlightRenderer.renderPossibleSquares3D(board, camera)
        } else {
            glClear(GL_COLOR_BUFFER_BIT)

            boardRenderer.render2D()
            highlightRenderer.renderSelectedSquares2D(board, displayWidth, displayHeight)
            pieceRenderer.render2D(game)
            highlightRenderer.renderPossibleSquares2D(board, displayWidth, displayHeight)
        }

        if (pixelsRequested) {
            onPixelsRead(saveBuffer())
            pixelsRequested = false
            onPixelsRead = {}
        }

        onDraw()
    }

    private fun updateBoardCamera() {
        if (this::board.isInitialized) {
            board.updateCamera(camera)
        }
    }

    fun getCameraRotation() = camera.rotation

    fun setCameraRotation(rotation: Vector3) {
        camera.rotation = rotation
        updateBoardCamera()
    }

    fun setFoV(fov: Int) {
        camera.fieldOfView = fov.toFloat()
        updateBoardCamera()
    }

    fun rotateCamera(rotation: Vector3) {
        camera.rotation += rotation

        if (camera.rotation.x > 0.0f) {
            camera.rotation.x = 0.0f
        }
        if (camera.rotation.x < -PI.toFloat() / 2.0f) {
            camera.rotation.x = -PI.toFloat() / 2.0f
        }
        updateBoardCamera()
    }

    fun setCameraZoom(distance: Float) {
        camera.setZoom(distance)
        updateBoardCamera()
    }

    fun incrementCameraZoom(distance: Float) {
        camera.incrementZoom(distance)
        updateBoardCamera()
    }

    fun getCameraZoom() = camera.getZoom()

    private fun saveBuffer(): ByteBuffer {
        val pixelData = ByteBuffer.allocateDirect(displayWidth * displayHeight * 4)
        glReadPixels(0, 0, displayWidth, displayHeight, GL_RGBA, GL_UNSIGNED_BYTE, pixelData)
        pixelData.rewind()

        val bitmap = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(pixelData)

//        ImageUtils.saveBitmapToStorage(context, bitmap, "king_checked.png")

        return pixelData
    }

    fun destroy() {
        pieceRenderer.destroy()
        highlightRenderer.destroy()
        boardRenderer.destroy()
    }
}