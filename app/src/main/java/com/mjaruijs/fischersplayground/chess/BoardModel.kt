package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.math.vectors.Vector4
import com.mjaruijs.fischersplayground.opengl.BoardMesh

class BoardModel(is3D: Boolean) {

    private val mesh: BoardMesh

    init {
        var vertices = FloatArray(0)

        val scaleFactor = 1.0f / 4.0f

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                // Triangle 1:
                vertices += (x + 1) * scaleFactor - VERTEX_OFFSET      // x = 1
                vertices += y * scaleFactor - VERTEX_OFFSET            // y = 0
                if (is3D) {
                    vertices += BOARD_HEIGHT
                    vertices += 0.0f
                }

                vertices += (x + 1) * scaleFactor - VERTEX_OFFSET      // x = 1
                vertices += (y + 1) * scaleFactor - VERTEX_OFFSET      // y = 1
                if (is3D) {
                    vertices += BOARD_HEIGHT
                    vertices += 0.0f
                }

                vertices += x * scaleFactor - VERTEX_OFFSET            // x = 0
                vertices += y * scaleFactor - VERTEX_OFFSET            // y = 0
                if (is3D) {
                    vertices += BOARD_HEIGHT
                    vertices += 0.0f
                }

                // Triangle 2:
                vertices += (x + 1) * scaleFactor - VERTEX_OFFSET      // x = 1
                vertices += (y + 1) * scaleFactor - VERTEX_OFFSET      // y = 1
                if (is3D) {
                    vertices += BOARD_HEIGHT
                    vertices += 0.0f
                }

                vertices += x * scaleFactor - VERTEX_OFFSET            // x = 0
                vertices += (y + 1) * scaleFactor - VERTEX_OFFSET      // y = 1
                if (is3D) {
                    vertices += BOARD_HEIGHT
                    vertices += 0.0f
                }

                vertices += x * scaleFactor - VERTEX_OFFSET            // x = 0
                vertices += y * scaleFactor - VERTEX_OFFSET            // y = 0
                if (is3D) {
                    vertices += BOARD_HEIGHT
                    vertices += 0.0f
                }
            }
        }

        if (is3D) {
            vertices += createFrame()
        }

        mesh = BoardMesh(vertices, is3D)
    }

    fun draw() {
        mesh.draw()
    }

    fun destroy() {
        mesh.destroy()
    }

    private fun createFrame(): FloatArray {
        val points = ArrayList<Vector4>()

        // Bottom
        points += Vector4(-VERTEX_OFFSET, -VERTEX_OFFSET, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET, -VERTEX_OFFSET, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, -VERTEX_OFFSET, BOARD_HEIGHT, POSITIVE_Z_INDEX)

        // Bottom-front
        points += Vector4(-VERTEX_OFFSET - BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, NEGATIVE_Y_INDEX)
        points += Vector4(-VERTEX_OFFSET - BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT - BOARD_FRAME_HEIGHT, NEGATIVE_Y_INDEX)
        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT - BOARD_FRAME_HEIGHT, NEGATIVE_Y_INDEX)
        points += Vector4(-VERTEX_OFFSET - BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, NEGATIVE_Y_INDEX)
        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT - BOARD_FRAME_HEIGHT, NEGATIVE_Y_INDEX)
        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, NEGATIVE_Y_INDEX)

        // Top
        points += Vector4(-VERTEX_OFFSET, VERTEX_OFFSET, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET, VERTEX_OFFSET, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, VERTEX_OFFSET, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)

        // Left
        points += Vector4(-VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET - BOARD_FRAME_WIDTH, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET - BOARD_FRAME_WIDTH, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(-VERTEX_OFFSET - BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)

        // Right
        points += Vector4(VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)

//        points += Vector4(VERTEX_OFFSET, VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
//        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, VERTEX_OFFSET + BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)
//        points += Vector4(VERTEX_OFFSET + BOARD_FRAME_WIDTH, -VERTEX_OFFSET - BOARD_FRAME_WIDTH, BOARD_HEIGHT, POSITIVE_Z_INDEX)

        val vertices = FloatArray(points.size * 4)
        
        var i = 0
        for (point in points) {
            vertices[i++] = point.x
            vertices[i++] = point.y
            vertices[i++] = point.z
            vertices[i++] = point.w
        }
        
        return vertices
    }

    companion object {
        private const val VERTEX_OFFSET = 1.0f
        private const val BOARD_HEIGHT = 0.0f
        private const val BOARD_FRAME_WIDTH = 0.05f
        private const val BOARD_FRAME_HEIGHT = 0.1f

        private const val POSITIVE_X_INDEX = 1f
        private const val NEGATIVE_X_INDEX = 2f
        private const val POSITIVE_Y_INDEX = 3f
        private const val NEGATIVE_Y_INDEX = 4f
        private const val POSITIVE_Z_INDEX = 5f
        private const val NEGATIVE_Z_INDEX = 6f
    }

}