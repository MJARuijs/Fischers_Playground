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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

class GameRenderer2D(context: Context) {
    
    private val quad = Quad()
    private val sampler = Sampler(0)

    private val isLocked = AtomicBoolean(false)

    private val piece2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_2d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.piece_2d_fragment, ShaderType.FRAGMENT, context)
    )

    private val animations = ArrayList<AnimationValues>()

    private fun startAnimation(animationData: AnimationData) {
        val toPosition = animationData.toPosition
        val fromPosition = animationData.fromPosition

        val animatingRow = toPosition.x.roundToInt()
        val animatingCol = toPosition.y.roundToInt()
        val translation = fromPosition - toPosition
        val totalDistance = fromPosition - toPosition

        animations += AnimationValues(animatingRow, animatingCol, translation, totalDistance, animationData.onAnimationFinished)
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun startAnimations(game: Game) {
        while (isLocked.get()) {}

        isLocked.set(true)
        val animationData = game.getAnimationData()

        for (animation in animationData) {
            startAnimation(animation)
        }

        game.resetAnimationData()
        isLocked.set(false)
    }

    fun render(game: Game, aspectRatio: Float) {
        startAnimations(game)

        piece2DProgram.start()
        piece2DProgram.set("aspectRatio", aspectRatio)
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

                piece2DProgram.set("scale", Vector2(aspectRatio, aspectRatio) / 4.0f)
                piece2DProgram.set("textureId", piece.textureId.toFloat())
                piece2DProgram.set("textureMaps", sampler.index)
                piece2DProgram.set("translation", translation)
                quad.draw()
            }
        }

        for (animation in animations) {
            if (animation.stopAnimating) {
                animation.onFinish()
            }
        }

        animations.removeIf { animation -> animation.stopAnimating }

        piece2DProgram.stop()
    }

    fun update(delta: Float): Boolean {
        for (animation in animations) {
            val increment = animation.totalDistance * delta * 5f

            if (abs(animation.translation.x) < abs(increment.x) || abs(animation.translation.y) < abs(increment.y)) {
                animation.translation = Vector2()
                animation.stopAnimating = true
            } else {
                animation.translation -= increment
            }
        }

        return animations.isNotEmpty()
    }

    fun destroy() {
        quad.destroy()
        piece2DProgram.destroy()
    }
    
}