package com.mjaruijs.fischersplayground.opengl.shaders

import android.opengl.GLES20.*
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import java.nio.IntBuffer

class ShaderProgram(vararg shaders: Shader) {

    private val uniforms = HashMap<String, Int>()

    private val handle = glCreateProgram()

    init {

        for (shader in shaders) {
            glAttachShader(handle, shader.handle)
        }

        glLinkProgram(handle)

        val linkStatus = IntBuffer.allocate(1)
        glGetProgramiv(handle, GL_LINK_STATUS, linkStatus)

        if (linkStatus[0] != GL_TRUE) {
            println("SHADERPROGRAM LINKING FAILED")
            println(glGetProgramInfoLog(handle))
        }

        glValidateProgram(handle)

        val validateStatus = IntBuffer.allocate(1)
        glGetProgramiv(handle, GL_VALIDATE_STATUS, validateStatus)

        if (validateStatus[0] != GL_TRUE) {
            println("SHADERPROGRAM VALIDATION FAILED")
            println(glGetProgramInfoLog(handle))
        }
    }

    fun start() {
        glUseProgram(handle)
    }

    fun stop() {
        glUseProgram(0)
    }

    private fun getUniformLocation(name: String) = uniforms.computeIfAbsent(name) {
        glGetUniformLocation(handle, name)
    }

//    private fun <T> set(name: String, value: T, setter: (Int, Int, T, Int) -> Unit) {
//        val location = getUniformLocation(name)
//        setter.invoke(location, value)
//    }

//    private fun <T, R> set(name: String, value1: T, value2: R, setter: (Int, T, R) -> Unit) {
//        val location = getUniformLocation(name)
//        setter.invoke(location, value1, value2)
//    }

    fun set(name: String, value: Int) {
        val location = getUniformLocation(name)
        glUniform1i(location, value)
    }

    fun set(name: String, value: Float) {
        val location = getUniformLocation(name)
        glUniform1f(location, value)
    }

    fun set(name: String, vector: Vector2) {
        val location = getUniformLocation(name)
        glUniform2fv(location, 1, vector.toArray(), 0)
    }

    fun set(name: String, vector: Vector3) {
        val location = getUniformLocation(name)
        glUniform3fv(location, 1, vector.toArray(), 0)
    }

    fun set(name: String, matrix: Matrix4) {
        val location = getUniformLocation(name)
        glUniformMatrix4fv(location, 1, true, matrix.toArray(), 0)
    }
//    fun set(name: String, vector: Vector3) = set(name, vector.toArray(), ::glUniform3fv)
//    fun set(name: String, vector: Vector4) = set(name, vector.toArray(), ::glUniform4fv)
//
//    fun set(name: String, matrix: Matrix2) = set(name, true, matrix.elements, ::glUniformMatrix2fv)
//    fun set(name: String, matrix: Matrix3) = set(name, true, matrix.elements, ::glUniformMatrix3fv)
//    fun set(name: String, matrix: Matrix4) = set(name, true, matrix.elements, ::glUniformMatrix4fv)

    fun set(name: String, value: Boolean) = set(name, if (value) 1 else 0)

    fun set(name: String, color: Color) {
        val location = getUniformLocation(name)
        glUniform4fv(location, 1, color.toArray(), 0)
    }

//    fun set(name: String, color: Color) = set(name, color.toArray(), ::glUniform4fv)
//    fun set(name: String, sampler: Sampler) = set(name, sampler.index)

    fun getAttribLocation(name: String): Int {
        return glGetAttribLocation(handle, name)
    }

    fun destroy() {
        glDeleteProgram(handle)
    }

}