package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.math.vectors.Vector4
import com.mjaruijs.fischersplayground.opengl.Camera
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI

class OpenGLRenderer(private val context: Context, private val onContextCreated: () -> Unit, var is3D: Boolean) : GLSurfaceView.Renderer {

    private lateinit var board: Board
    private lateinit var game: Game

    private lateinit var gameRenderer: GameRenderer2D
    private lateinit var boardRenderer: BoardRenderer
    private lateinit var gameRenderer3D: GameRenderer3D

    private val camera = Camera()

    private var aspectRatio = 1.0f
    private var displayWidth = 0
    private var displayHeight = 0

    private var initialized = false

    var onDisplaySizeChanged: (Int, Int) -> Unit = { _, _ -> }

    override fun onSurfaceCreated(p0: GL10?, config: EGLConfig?) {
        glClearColor(0.25f, 0.25f, 0.25f, 1f)
        glEnable(GL_BLEND)
        glEnable(GL_CULL_FACE)
        glEnable(GL_DEPTH_TEST)

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        PieceTextures.createTextureArray()

        gameRenderer = GameRenderer2D(context)
        gameRenderer3D = GameRenderer3D(context)

        boardRenderer = BoardRenderer(context)

        onContextCreated()
        initialized = true
    }

    fun setPieceScale(scale: Float) {
        gameRenderer3D.pieceScale = Vector3(scale, scale, scale)
    }

    fun setGame(game: Game) {
        this.game = game
        this.board = game.board

        board.is3D = is3D
    }

    fun update(delta: Float): Boolean {
        return if (initialized) {
            Thread {
                gameRenderer.startAnimations(game)
            }.start()
            gameRenderer.update(delta)
        } else {
            false
        }
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        displayWidth = width
        displayHeight = height
        aspectRatio = width.toFloat() / height.toFloat()

        onDisplaySizeChanged(displayWidth, displayHeight)
    }

    override fun onDrawFrame(p0: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        if (is3D) {
            boardRenderer.render3D(board, camera)
            gameRenderer3D.render(game, camera)
        } else {
            boardRenderer.render(board, aspectRatio)
            gameRenderer.render(game, aspectRatio)
        }
    }

    private fun updateBoardCamera() {
        board.updateCamera(camera)
    }

    fun getCameraPosition() = camera.position

    fun getCameraRotation() = camera.rotation

    fun setCameraPosition(position: Vector3) {
        camera.position = position
        updateBoardCamera()
    }

    fun setCameraRotation(rotation: Vector3) {
        camera.rotation = rotation
        updateBoardCamera()
    }

    fun setFoV(fov: Int) {
        camera.fieldOfView = fov.toFloat()
        updateBoardCamera()
    }

    fun setFoV(fov: Float) {
        camera.fieldOfView = fov
    }

    fun getCameraDirection(): Vector3 {
        val clipCoords = Vector4(0f, 0f, -1f, 1f)
        val eyeSpace = camera.projectionMatrix.inverse().dot(clipCoords)
        eyeSpace.z = -1f
        eyeSpace.w = 0f

        return camera.viewMatrix.inverse().dot(eyeSpace).xyz().normal()
    }

    fun zoomCamera(zoomLevel: Float) {
        val cameraDirection = getCameraDirection()

        camera.zoom = cameraDirection * zoomLevel * 0.001f
//        println(camera.zoom)
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

    fun rotateCamera(angle: Float) {
        camera.rotation.y += angle
    }

    fun translateCameraY(translation: Float) {
        camera.translate(Vector3(0f, translation, 0f))
        updateBoardCamera()
    }

    fun translateCameraZ(translation: Float) {
        camera.translate(Vector3(0f, 0f, translation))
        updateBoardCamera()
    }

    fun destroy() {
//        gameRenderer.destroy()
//        boardRenderer.destroy()
    }
}