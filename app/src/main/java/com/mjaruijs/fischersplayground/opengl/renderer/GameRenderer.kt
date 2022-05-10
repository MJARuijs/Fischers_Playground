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

class GameRenderer(context: Context) {
    
    private val quad = Quad()
    private val sampler = Sampler(0)

    private val isLocked = AtomicBoolean(false)

    private val pieceProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.piece_fragment, ShaderType.FRAGMENT, context)
    )

    private val animations = ArrayList<AnimationValues>()

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
//        Thread {
            startAnimations(game)
//        }.start()

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
//                println("End: ${Thread.currentThread().id} ${animation.piece} ${animation.totalDistance} ${animation.translation}")

                animation.onFinish()
            }
        }

        animations.removeIf { animation -> animation.stopAnimating }

        pieceProgram.stop()
    }

    fun update(delta: Float): Boolean {
        for (animation in animations) {
            val increment = animation.totalDistance * delta * 5f

//            println("Update: ${Thread.currentThread().id} ${animation.piece} ${animation.translation} ${animation.totalDistance} $increment")

            if (abs(animation.translation.x) < abs(increment.x) || abs(animation.translation.y) < abs(increment.y)) {
                animation.translation = Vector2()
                animation.stopAnimating = true
//                animation.onFinish()
            } else {
                animation.translation -= increment
            }
        }

        return animations.isNotEmpty()
    }

    private fun startAnimation(animationData: AnimationData) {
        val toPosition = animationData.toPosition
        val fromPosition = animationData.fromPosition

        val animatingRow = toPosition.x.roundToInt()
        val animatingCol = toPosition.y.roundToInt()
        val translation = fromPosition - toPosition
        val totalDistance = fromPosition - toPosition

//        println("Start: ${Thread.currentThread().id} ${animationData.pieceType} $fromPosition $toPosition $translation $totalDistance")

        animations += AnimationValues(animationData.pieceType, animatingRow, animatingCol, translation, totalDistance, animationData.onAnimationFinished)
    }

    fun destroy() {
        quad.destroy()
        pieceProgram.destroy()
    }
    
}