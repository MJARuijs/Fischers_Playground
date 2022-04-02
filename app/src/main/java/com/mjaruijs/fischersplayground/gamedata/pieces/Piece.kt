package com.mjaruijs.fischersplayground.gamedata.pieces

import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.texture.Sampler

class Piece(val type: PieceType, var position: Vector2, var scale: Vector2 = defaultScale) {

    private val quad = Quad()

    private val sampler = Sampler(0)

    val scaledPosition = position / 8.0f + originOffset

    fun draw(shaderProgram: ShaderProgram) {
        shaderProgram.set("translation", scaledPosition)
        shaderProgram.set("texture", sampler.index)
        shaderProgram.set("scale", scale)
        sampler.bind(type.texture)
        quad.draw()
    }

    fun destroy() {
        quad.destroy()
    }

    companion object {
        private val originOffset = Vector2(-0.5f, -0.5f + 0.125f)
        private val defaultScale = Vector2(0.125f, 0.125f)
    }

}