package com.mjaruijs.fischersplayground.opengl.shaders

import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_VERTEX_SHADER

enum class ShaderType(val index: Int) {

    VERTEX(GL_VERTEX_SHADER),
    FRAGMENT(GL_FRAGMENT_SHADER)

}