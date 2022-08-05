package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.chess.BoardModel
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.light.AmbientLight
import com.mjaruijs.fischersplayground.opengl.light.DirectionalLight
import com.mjaruijs.fischersplayground.opengl.model.Material
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader

class BoardRenderer(context: Context) {

    private val model2D = BoardModel(false)
    private val model3D = BoardModel(true)
    private val diffuseTexture = TextureLoader.load(context, R.drawable.wood_diffuse_texture)
    private val normalTexture = TextureLoader.load(context, R.drawable.wood_normal_texture)
    private val specularTexture = TextureLoader.load(context, R.drawable.wood_specular_texture)

    private val diffuseSampler = Sampler(0)
    private val normalSampler = Sampler(1)
    private val specularSampler = Sampler(2)

    private val ambientLight = AmbientLight(Color.DARK)
    private val directionalLight = DirectionalLight(Color.WHITE, Vector3(0.0f, -0.5f, 1f))

    private val boardFrameMaterial = Material(Color(61, 45, 32), Color(61, 45, 32), Color.WHITE, 20.0f)

    private val board2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.board_2d_fragment, ShaderType.FRAGMENT, context)
    )

    private val board3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_3d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.board_3d_fragment, ShaderType.FRAGMENT, context)
    )

    init {
        diffuseTexture.init()
        normalTexture.init()
        specularTexture.init()
    }

    fun render3D(board: Board, camera: Camera, displayWidth: Int, displayHeight: Int) {
        board3DProgram.start()
        board3DProgram.set("diffuseTexture", diffuseSampler.index)
        board3DProgram.set("normalTexture", normalSampler.index)
        board3DProgram.set("specularTexture", specularSampler.index)

        board3DProgram.set("projection", camera.projectionMatrix)
        board3DProgram.set("view", camera.viewMatrix)
        board3DProgram.set("selectedSquareCoordinates", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
        board3DProgram.set("checkedKingSquare", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)
        board3DProgram.set("cameraPosition", camera.getPosition())
        board3DProgram.set("viewPort", Vector2(displayWidth, displayHeight))

        diffuseSampler.bind(diffuseTexture)
        normalSampler.bind(normalTexture)
        specularSampler.bind(specularTexture)

        ambientLight.applyTo(board3DProgram)
        directionalLight.applyTo(board3DProgram)
        boardFrameMaterial.applyTo(board3DProgram)

        model3D.draw()
        board3DProgram.stop()
    }

    fun render2D(aspectRatio: Float) {
        board2DProgram.start()
        board2DProgram.set("aspectRatio", aspectRatio)
        diffuseSampler.bind(diffuseTexture)
        model2D.draw()
        board2DProgram.stop()
    }

    fun destroy() {
        model2D.destroy()
        model3D.destroy()
        board2DProgram.destroy()
    }

}