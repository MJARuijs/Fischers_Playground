package com.mjaruijs.fischersplayground.opengl

import android.opengl.GLES30.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class Quad {

    private val vertices = floatArrayOf(
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,

        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    private val vao: Int
    private val vbo: Int

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

    init {
        val buffers = IntBuffer.allocate(1)

        glGenVertexArrays(1, buffers)
        vao = buffers[0]

        glGenBuffers(1, buffers)
        vbo = buffers[0]

        glBindVertexArray(vao)

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.SIZE_BYTES, vertexBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindVertexArray(0)
    }

    fun draw() {
        glBindVertexArray(vao)
        glEnableVertexAttribArray(0)
        glDrawArrays(GL_TRIANGLES, 0, vertices.size)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    fun destroy() {
        glDeleteBuffers(1, IntBuffer.wrap(intArrayOf(vbo)))
        glDeleteVertexArrays(1, IntBuffer.wrap(intArrayOf(vao)))
    }

}