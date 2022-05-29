package com.mjaruijs.fischersplayground.opengl.light

import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram

class AmbientLight(val color: Color) {

    fun applyTo(shaderProgram: ShaderProgram) {
        shaderProgram.set("ambientLight.color", color)
    }

}