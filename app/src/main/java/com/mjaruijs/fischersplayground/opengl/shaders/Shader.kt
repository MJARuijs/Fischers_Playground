package com.mjaruijs.fischersplayground.opengl.shaders

import android.opengl.GLES20.*
import java.nio.IntBuffer

class Shader(type: ShaderType, sourceCode: String) {

    val handle = glCreateShader(type.index)

    init {
        glShaderSource(handle, sourceCode)

        glCompileShader(handle)

        val compileStatus = IntBuffer.allocate(1)
        glGetShaderiv(handle, GL_COMPILE_STATUS, compileStatus)

        if (compileStatus[0] != GL_TRUE) {
            println("SHADER COMPILATION FAILED")
            println(glGetShaderInfoLog(handle))
        }
    }

    fun destroy() {
        glDeleteShader(handle)
    }

}