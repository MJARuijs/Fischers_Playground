package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.opengl.BoardMesh

class BoardModel(is3D: Boolean) {

    private val mesh: BoardMesh

    init {
        var vertices = FloatArray(0)

        val scaleFactor = 1.0f / 4.0f
        val offset = 1.0f

        val height = -0.02f

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val whiteTiles = (x + y) % 2 != 0

                // Triangle 1:
                vertices += (x + 1) * scaleFactor - offset      // x = 1
                vertices += y * scaleFactor - offset            // y = 0
                if (is3D) {
                    vertices += height
                }
                vertices += if (whiteTiles) 1f else 0f

                vertices += (x + 1) * scaleFactor - offset      // x = 1
                vertices += (y + 1) * scaleFactor - offset      // y = 1
                if (is3D) {
                    vertices += height
                }
                vertices += if (whiteTiles) 1f else 0f

                vertices += x * scaleFactor - offset            // x = 0
                vertices += y * scaleFactor - offset            // y = 0
                if (is3D) {
                    vertices += height
                }
                vertices += if (whiteTiles) 1f else 0f

                // Triangle 2:
                vertices += (x + 1) * scaleFactor - offset      // x = 1
                vertices += (y + 1) * scaleFactor - offset      // y = 1
                if (is3D) {
                    vertices += height
                }
                vertices += if (whiteTiles) 1f else 0f

                vertices += x * scaleFactor - offset            // x = 0
                vertices += (y + 1) * scaleFactor - offset      // y = 1
                if (is3D) {
                    vertices += height
                }
                vertices += if (whiteTiles) 1f else 0f

                vertices += x * scaleFactor - offset            // x = 0
                vertices += y * scaleFactor - offset            // y = 0
                if (is3D) {
                    vertices += height
                }
                vertices += if (whiteTiles) 1f else 0f
            }
        }

        mesh = BoardMesh(vertices, is3D)
    }

    fun draw() {
        mesh.draw()
    }

    fun destroy() {
        mesh.destroy()
    }

}