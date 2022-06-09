package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.OBJLoader
import com.mjaruijs.fischersplayground.opengl.light.AmbientLight
import com.mjaruijs.fischersplayground.opengl.light.DirectionalLight
import com.mjaruijs.fischersplayground.opengl.model.Entity
import com.mjaruijs.fischersplayground.opengl.model.Material
import com.mjaruijs.fischersplayground.opengl.model.Mesh
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

class GameRenderer3D(context: Context, isPlayerWhite: Boolean) {

    private val sampler = Sampler(0)
    private val piece3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_3d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.piece_3d_fragment, ShaderType.FRAGMENT, context)
    )

    private val animations = ArrayList<AnimationValues>()

    private val pawnMesh = Mesh(OBJLoader.get(context, R.raw.pawn_bytes))
    private val bishopMesh = Mesh(OBJLoader.get(context, R.raw.bishop_bytes))
    private val knightMesh = Mesh(OBJLoader.get(context, R.raw.knight_bytes))
    private val rookMesh = Mesh(OBJLoader.get(context, R.raw.rook_bytes))
    private val queenMesh = Mesh(OBJLoader.get(context, R.raw.queen_bytes))
    private val kingMesh = Mesh(OBJLoader.get(context, R.raw.king_bytes))
//
    private val pawn = Entity(pawnMesh)
    private val bishop = Entity(bishopMesh)
    private val knight = Entity(knightMesh)
    private val rook = Entity(rookMesh)
    private val queen = Entity(queenMesh)
    private val king = Entity(kingMesh)

    private val ambientLight = AmbientLight(Color.DARK)
    private val directionalLight = DirectionalLight(Color.WHITE, Vector3(0.0f, -0.5f, 1f))

    private val whiteMaterial = Material(Color(0.8f, 0.8f, 0.8f), Color(0.8f, 0.8f, 0.8f), Color.WHITE, 50.0f)
    private val blackMaterial = Material(Color(0.2f, 0.2f, 0.2f), Color(0.2f, 0.2f, 0.2f), Color.WHITE, 50.0f)

    private val whiteKnightRotation = if (isPlayerWhite) ROTATION_MATRIX else Matrix4()
    private val blackKnightRotation = if (isPlayerWhite) Matrix4() else ROTATION_MATRIX

    private val isLocked = AtomicBoolean(false)

    var pieceScale = Vector3(1f, 1f, 1f)

    var rChannel = 1.0f
    var gChannel = 1.0f
    var bChannel = 1.0f

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

    fun render(game: Game, camera: Camera) {
        startAnimations(game)

        piece3DProgram.start()
        piece3DProgram.set("projection", camera.projectionMatrix)
        piece3DProgram.set("view", camera.viewMatrix)
        piece3DProgram.set("cameraPosition", camera.getPosition())
        piece3DProgram.set("textureMaps", sampler.index)

        piece3DProgram.set("rChannel", rChannel)
        piece3DProgram.set("gChannel", gChannel)
        piece3DProgram.set("bChannel", bChannel)

        ambientLight.applyTo(piece3DProgram)
        directionalLight.applyTo(piece3DProgram)

        sampler.bind(PieceTextures.get3DTextureArray())

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = game[row, col] ?: continue

                val animation = animations.find { animation -> animation.animatingRow == row && animation.animatingCol == col }

                val translation = if (animation == null) {
                    (Vector2(row, col) / 8.0f) * 2.0f - 1.0f + Vector2(1 / 8f, 1 / 8f)
                } else {
                    (Vector2(row + animation.translation.x, col + animation.translation.y) / 8.0f) * 2.0f - 1.0f + Vector2(1 / 8f, 1 / 8f)
                }

                if (piece.team == Team.WHITE) {
                    whiteMaterial.applyTo(piece3DProgram)
                } else {
                    blackMaterial.applyTo(piece3DProgram)
                }

                piece3DProgram.set("isWhite", if (piece.team == Team.WHITE) 1f else 0f)
                piece3DProgram.set("textureId", piece.textureId3D.toFloat())

                when (piece.type) {
                    PieceType.PAWN -> pawn.render(piece3DProgram, translation, pieceScale)
                    PieceType.BISHOP -> bishop.render(piece3DProgram, translation, pieceScale)
                    PieceType.KNIGHT -> {
                        val rotation = if (piece.team == Team.WHITE) whiteKnightRotation else blackKnightRotation
                        knight.render(piece3DProgram, translation, rotation, pieceScale)
                    }
                    PieceType.ROOK -> rook.render(piece3DProgram, translation, pieceScale)
                    PieceType.QUEEN -> queen.render(piece3DProgram, translation, pieceScale)
                    PieceType.KING -> king.render(piece3DProgram, translation, pieceScale)
                }
            }
        }

        for (animation in animations) {
            if (animation.stopAnimating) {
                animation.onFinish()
            }
        }

        animations.removeIf { animation -> animation.stopAnimating }

        piece3DProgram.stop()
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
//        pawnMesh.destroy()
//        knightMesh.destroy()
//        bishopMesh.destroy()
//        rookMesh.destroy()
//        queenMesh.destroy()
//        kingMesh.destroy()
        piece3DProgram.destroy()
    }

    companion object {

        private val ROTATION_MATRIX = Matrix4().rotateZ(PI.toFloat())

    }

}