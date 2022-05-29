package com.mjaruijs.fischersplayground.opengl.model

import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram

class Model(private val mesh: Mesh, private val material: Material, private val transformation: Matrix4 = Matrix4()) {

    fun render(shaderProgram: ShaderProgram) {
//        shaderProgram.set("model", transformation)
        material.applyTo(shaderProgram)

        mesh.draw()
    }

    fun destroy() {
        mesh.destroy()
    }

}