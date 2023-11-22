package com.mjaruijs.fischersplayground.opengl.light

import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram

class DirectionalLight(val color: Color, val direction: Vector3) {

    fun applyTo(shaderProgram: ShaderProgram) {
        shaderProgram.set("sun.color", color)
        shaderProgram.set("sun.direction", direction)
    }

}