package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import kotlin.math.abs
import kotlin.math.roundToInt

class GameRenderer(context: Context) {
    
    private val quad = Quad()
    private val sampler = Sampler(0)

    private val pieceProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.piece_fragment, ShaderType.FRAGMENT, context)
    )

    private val animations = ArrayList<AnimationValues>()

    fun render(game: Game, aspectRatio: Float) {

        val animationData = game.getAnimationData()

        for (animation in animationData) {
            startAnimation(animation)
        }

        game.resetAnimationData()

        pieceProgram.start()
        pieceProgram.set("aspectRatio", aspectRatio)
        sampler.bind(PieceTextures.getTextureArray())

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = game[row, col] ?: continue

                val animation = animations.find { animation -> animation.animatingRow == row && animation.animatingCol == col }

                val translation = if (animation == null) {
                    (Vector2(row * aspectRatio * 2.0f, col * aspectRatio * 2.0f) / 8.0f) + Vector2(-aspectRatio, aspectRatio / 4.0f - aspectRatio)
                } else {
                    (Vector2((row + animation.translation.x) * aspectRatio * 2.0f, (col + animation.translation.y) * aspectRatio * 2.0f) / 8.0f) + Vector2(-aspectRatio, aspectRatio / 4.0f - aspectRatio)
                }

                pieceProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 4.0f)
                pieceProgram.set("textureId", piece.textureId.toFloat())
                pieceProgram.set("textureMaps", sampler.index)
                pieceProgram.set("translation", translation)
                quad.draw()
            }
        }

        for (animation in animations) {
            if (animation.stopAnimating) {
                println("Animation data: ${animation.totalDistance} ${animation.translation}")
            }
        }
        animations.removeIf { animation -> animation.stopAnimating }

        pieceProgram.stop()
    }

    fun update(delta: Float): Boolean {
        for (animation in animations) {
            val increment = animation.totalDistance * delta * 5.0f

            if (abs(animation.translation.x - animation.totalDistance.x) < abs(increment.x) || abs(animation.translation.y - animation.totalDistance.y) < abs(increment.y)) {
                animation.translation = animation.totalDistance
                animation.stopAnimating = true
                animation.onFinish()
            } else {
                animation.translation += increment
            }
        }

        return animations.isNotEmpty()
    }

    private fun startAnimation(animationData: AnimationData) {
        val toPosition = animationData.toPosition
        val fromPosition = animationData.fromPosition

        val animatingRow = fromPosition.x.roundToInt()
        val animatingCol = fromPosition.y.roundToInt()
        val totalDistance = toPosition - fromPosition

        animations += AnimationValues(animatingRow, animatingCol, Vector2(), totalDistance, animationData.onAnimationFinished)
    }

    fun destroy() {
        quad.destroy()
        pieceProgram.destroy()
    }
    
}