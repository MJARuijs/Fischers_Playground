package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.content.res.Resources
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
import com.mjaruijs.fischersplayground.opengl.renderer.animation.AnimationData
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI

class OpenGLRenderer(context: Context, private val resources: Resources, private var onContextCreated: () -> Unit, private var is3D: Boolean) : GLSurfaceView.Renderer {

    companion object {

        private var instance: OpenGLRenderer? = null

        fun getInstance(context: Context, onContextCreated: () -> Unit, is3D: Boolean): OpenGLRenderer {
            if (instance == null) {
                instance = OpenGLRenderer(context, context.resources, onContextCreated, is3D)
            } else {
                instance!!.onContextCreated = onContextCreated
                instance!!.is3D = is3D
            }
            return instance!!
        }

    }

    private lateinit var board: Board
    private lateinit var game: Game

//    private lateinit var backgroundRenderer: BackgroundRenderer
    private lateinit var boardRenderer: BoardRenderer
    private lateinit var pieceRenderer: PieceRenderer
    private lateinit var highlightRenderer: HighlightRenderer

    private val camera = Camera()

    private var displayWidth = 0
    private var displayHeight = 0
    private var aspectRatio = 0f

    private var pixelsRequested = false

    var isPlayerWhite = true

    lateinit var runOnUiThread: (() -> Unit) -> Unit
    var requestRender: () -> Unit = {}
    var onPixelsRead: (ByteBuffer) -> Unit = {}
    var onDisplaySizeChanged: (Int, Int) -> Unit = { _, _ -> }

    init {
        val preferences = context.getSharedPreferences("graphics_preferences", AppCompatActivity.MODE_PRIVATE)
        camera.setZoom(preferences.getFloat(CAMERA_ZOOM_KEY, DEFAULT_ZOOM))
    }

    override fun onSurfaceCreated(p0: GL10?, config: EGLConfig?) {
        glClearColor(0.25f, 0.25f, 0.25f, 1f)
        glEnable(GL_BLEND)
        setOpenGLSettings()

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        PieceTextures.createTextureArrays()

//        PieceRenderer.init(resources, isPlayerWhite)
//        backgroundRenderer = BackgroundRenderer(context)
        pieceRenderer = PieceRenderer(resources, isPlayerWhite, ::requestRenderPieces, runOnUiThread, ::getGame)
        boardRenderer = BoardRenderer(resources)
        highlightRenderer = HighlightRenderer(resources)

        onContextCreated()
    }

    private fun requestRenderPieces() {
        requestRender()
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
        game.queueAnimation = ::queueAnimation

        board.is3D = is3D
    }

    private fun getGame(): Game {
        return game
    }

    private fun queueAnimation(animationData: AnimationData) {
        pieceRenderer.queueAnimation(game, animationData)
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        displayWidth = width
        displayHeight = height
        aspectRatio = width.toFloat() / height.toFloat()
        camera.aspectRatio = aspectRatio

        onDisplaySizeChanged(displayWidth, displayHeight)
    }

    var renderCircle = false

    override fun onDrawFrame(p0: GL10?) {
        if (!this::game.isInitialized) {
            return
        }

        try {
            if (is3D) {
                glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

//            backgroundRenderer.render3D(camera, aspectRatio)
                boardRenderer.render3D(board, camera, displayWidth, displayHeight, aspectRatio)
                highlightRenderer.renderSelectedSquares3D(board, camera)

                pieceRenderer.render3D(game, camera, aspectRatio)
                highlightRenderer.renderPossibleSquares3D(board, camera)
            } else {
                glClear(GL_COLOR_BUFFER_BIT)

//            backgroundRenderer.render2D(aspectRatio)
                boardRenderer.render2D(aspectRatio)
                highlightRenderer.renderSelectedSquares2D(board, displayWidth, displayHeight, aspectRatio)

                pieceRenderer.render2D(game, aspectRatio)
                highlightRenderer.renderPossibleSquares2D(board, displayWidth, displayHeight, aspectRatio)
            }

            if (pixelsRequested) {
                onPixelsRead(saveBuffer())
                pixelsRequested = false
                onPixelsRead = {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
//        backgroundRenderer.destroy()
        pieceRenderer.destroy()
        highlightRenderer.destroy()
        boardRenderer.destroy()
    }
}