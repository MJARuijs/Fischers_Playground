package com.mjaruijs.fischersplayground.opengl

import android.opengl.GLES30.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class BoardMesh(vertices: FloatArray, is3D: Boolean) {

    private val count = vertices.size / 3

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

    private val vao: Int
    private val vbo: Int

    init {
        val buffers = IntBuffer.allocate(1)
        val size = if (is3D) 4 else 3

        glGenVertexArrays(1, buffers)
        vao = buffers[0]

        glGenBuffers(1, buffers)
        vbo = buffers[0]

        glBindVertexArray(vao)

        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.SIZE_BYTES, vertexBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, size, GL_FLOAT, false, 0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindVertexArray(0)
    }

    fun draw() {
        glBindVertexArray(vao)
        glEnableVertexAttribArray(0)
        glDrawArrays(GL_TRIANGLES, 0, count)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    fun destroy() {
        glDeleteBuffers(1, IntBuffer.wrap(intArrayOf(vbo)))
        glDeleteVertexArrays(1, IntBuffer.wrap(intArrayOf(vao)))
    }

}