package com.mjaruijs.fischersplayground.opengl.model

import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram

class Material(private val ambient: Color, private val diffuse: Color, private val specular: Color, private val shininess: Float) {

    fun applyTo(shaderProgram: ShaderProgram) {
        shaderProgram.set("material.ambient", ambient)
        shaderProgram.set("material.diffuse", diffuse)
        shaderProgram.set("material.specular", specular)
        shaderProgram.set("material.shininess", shininess)
    }

}