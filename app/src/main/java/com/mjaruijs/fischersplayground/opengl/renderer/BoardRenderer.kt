package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.res.Resources
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
import com.mjaruijs.fischersplayground.opengl.texture.Texture
import com.mjaruijs.fischersplayground.opengl.texture.TextureLoader
import com.mjaruijs.fischersplayground.util.Logger

class BoardRenderer(val resources: Resources) {

    private val tag = "BoardRenderer"

    private val model2D = BoardModel(false)
    private val model3D = BoardModel(true)
    private val diffuseTexture = TextureLoader.getInstance().get(resources, R.drawable.wood_diffuse_texture)

    private val diffuseSampler = Sampler(0)

    private val ambientLight = AmbientLight(Color.DARK)
    private val directionalLight = DirectionalLight(Color.WHITE, Vector3(0.0f, -0.5f, 1f))

    private val boardFrameMaterial = Material(Color(61, 45, 32), Color(61, 45, 32), Color.WHITE, 20.0f)

    private val board2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_2d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.board_2d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val board3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.board_3d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.board_3d_fragment, ShaderType.FRAGMENT, resources)
    )

    fun render2D() {
        board2DProgram.start()
        board2DProgram.set("textureMap", diffuseSampler.index)
        diffuseSampler.bind(diffuseTexture)
        model2D.draw()
        board2DProgram.stop()
    }

    fun render3D(board: Board, camera: Camera, displayWidth: Int, displayHeight: Int) {
//        board3DProgram.start()
//        board3DProgram.set("diffuseTexture", diffuseSampler.index)
//
//        board3DProgram.set("projection", camera.projectionMatrix)
//        board3DProgram.set("view", camera.viewMatrix)
//        board3DProgram.set("selectedSquareCoordinates", (board.selectedSquare / 8.0f) * 2.0f - 1.0f)
//        board3DProgram.set("checkedKingSquare", (board.checkedKingSquare / 8.0f) * 2.0f - 1.0f)
//        board3DProgram.set("cameraPosition", camera.getPosition())
//        board3DProgram.set("viewPort", Vector2(displayWidth, displayHeight))
//
//        diffuseSampler.bind(diffuseTexture)
//
//        ambientLight.applyTo(board3DProgram)
//        directionalLight.applyTo(board3DProgram)
//        boardFrameMaterial.applyTo(board3DProgram)
//
//        model3D.draw()
//        board3DProgram.stop()
    }

    fun destroy() {
        model2D.destroy()
        model3D.destroy()

        diffuseTexture.destroy()

        board2DProgram.destroy()
        board3DProgram.destroy()
    }

}