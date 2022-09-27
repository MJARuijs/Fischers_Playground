package com.mjaruijs.fischersplayground.opengl.shaders

import android.opengl.GLES20.*
import android.util.Log
import java.nio.IntBuffer

class Shader(type: ShaderType, sourceCode: String) {

    val handle = glCreateShader(type.index)

    init {
        glShaderSource(handle, sourceCode)

        glCompileShader(handle)

        val compileStatus = IntBuffer.allocate(1)
        glGetShaderiv(handle, GL_COMPILE_STATUS, compileStatus)

        if (compileStatus[0] != GL_TRUE) {
            Log.e("Shader", "SHADER COMPILATION FAILED")
            Log.e("Shader", glGetShaderInfoLog(handle))
        }
    }

    fun destroy() {
        glDeleteShader(handle)
    }

}