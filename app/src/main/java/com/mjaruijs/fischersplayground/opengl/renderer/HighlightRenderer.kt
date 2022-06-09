package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import android.graphics.Camera
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.Board
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType

class HighlightRenderer(context: Context) {

    private val quad = Quad()

    private val highlight2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.highlight_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.highlight_2d_fragment, ShaderType.FRAGMENT, context)
    )

    fun render2D(board: Board, aspectRatio: Float, displayWidth: Int, displayHeight: Int) {
        highlight2DProgram.start()
        highlight2DProgram.set("aspectRatio", aspectRatio)
        highlight2DProgram.set("translation", Vector2())
//        (Vector2(4, 4) / 8.0f) * 2.0f - 1.0f
        highlight2DProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 8f)
        highlight2DProgram.set("viewPort", Vector2(displayWidth, displayHeight))

        quad.draw()
//        highlight2DProgram.set("color")


        highlight2DProgram.stop()
    }

    fun destroy() {
        quad.destroy()
        highlight2DProgram.destroy()
    }
}