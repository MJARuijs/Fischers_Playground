package com.mjaruijs.fischersplayground.opengl.model

import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram

class Entity(private val mesh: Mesh) {

    fun render(shaderProgram: ShaderProgram, translation: Vector2, scale: Vector3) {
        val transformation = Matrix4().translate(translation).scale(scale)
        shaderProgram.set("model", transformation)
        mesh.draw()
    }

    fun render(shaderProgram: ShaderProgram, translation: Vector2, rotation: Matrix4, scale: Vector3) {
        val transformation = Matrix4().translate(translation).dot(rotation).scale(scale)
        shaderProgram.set("model", transformation)
        mesh.draw()
    }

    fun destroy() {
        mesh.destroy()
    }

}