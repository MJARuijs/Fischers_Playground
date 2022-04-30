package com.mjaruijs.fischersplayground.chess

import com.mjaruijs.fischersplayground.opengl.Mesh

class BoardModel {

    private val mesh: Mesh

    init {
        var vertices = FloatArray(0)

        val scaleFactor = 1.0f / 4.0f
        val offset = 1.0f

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val whiteTiles = (x + y) % 2 != 0

                // Triangle 1:

                vertices += (x + 1) * scaleFactor - offset      // x = 1
                vertices += y * scaleFactor - offset            // y = 0

                vertices += if (whiteTiles) {
                    1f
                } else {
                    0f
                }

                vertices += (x + 1) * scaleFactor - offset      // x = 1
                vertices += (y + 1) * scaleFactor - offset      // y = 1

                vertices += if (whiteTiles) {
                    1f
                } else {
                    0f
                }

                vertices += x * scaleFactor - offset            // x = 0
                vertices += y * scaleFactor - offset            // y = 0

                vertices += if (whiteTiles) {
                    1f
                } else {
                    0f
                }

                // Triangle 2:

                vertices += (x + 1) * scaleFactor - offset      // x = 1
                vertices += (y + 1) * scaleFactor - offset      // y = 1

                vertices += if (whiteTiles) {
                    1f
                } else {
                    0f
                }

                vertices += x * scaleFactor - offset            // x = 0
                vertices += (y + 1) * scaleFactor - offset      // y = 1

                vertices += if (whiteTiles) {
                    1f
                } else {
                    0f
                }

                vertices += x * scaleFactor - offset            // x = 0
                vertices += y * scaleFactor - offset            // y = 0

                vertices += if (whiteTiles) {
                    1f
                } else {
                    0f
                }
            }
        }

        mesh = Mesh(vertices)
    }

    fun draw() {
        mesh.draw()
    }

    fun destroy() {
        mesh.destroy()
    }

}