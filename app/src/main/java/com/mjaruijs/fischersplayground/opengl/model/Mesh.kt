package com.mjaruijs.fischersplayground.opengl.model

import android.opengl.GLES30.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class Mesh(vertices: FloatArray, normals: FloatArray, textureCoordinates: FloatArray) {

    constructor(data: MeshData) : this(data.vertices, data.normals, data.textureCoordinates)

    private val count = vertices.size / 3

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

    private val normalBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(normals.size * Float.SIZE_BYTES).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(normals)
                position(0)
            }
        }

    private val textureBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(textureCoordinates.size * Float.SIZE_BYTES).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(textureCoordinates)
                position(0)
            }
        }

    private val vao: Int
    private val vbo: Int
    private val nbo: Int
    private val tbo: Int

    init {
        val buffers = IntBuffer.allocate(1)

        glGenVertexArrays(1, buffers)
        vao = buffers[0]

        glGenBuffers(1, buffers)
        vbo = buffers[0]

        glGenBuffers(1, buffers)
        nbo = buffers[0]

        glGenBuffers(1, buffers)
        tbo = buffers[0]

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.SIZE_BYTES, vertexBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)

        glBindBuffer(GL_ARRAY_BUFFER, nbo)
        glBufferData(GL_ARRAY_BUFFER, normalBuffer.capacity() * Float.SIZE_BYTES, normalBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)

        glBindBuffer(GL_ARRAY_BUFFER, tbo)
        glBufferData(GL_ARRAY_BUFFER, textureBuffer.capacity() * Float.SIZE_BYTES, textureBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        glBindVertexArray(vao)
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)

        glDrawArrays(GL_TRIANGLES, 0, count)

        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)

        glBindVertexArray(0)
    }

    fun destroy() {
//        println("Destroying mesh! $vao $vbo $nbo $tbo")
        glDeleteBuffers(1, IntBuffer.wrap(intArrayOf(tbo)))
        glDeleteBuffers(1, IntBuffer.wrap(intArrayOf(nbo)))
        glDeleteBuffers(1, IntBuffer.wrap(intArrayOf(vbo)))
        glDeleteVertexArrays(1, IntBuffer.wrap(intArrayOf(vao)))
    }

}