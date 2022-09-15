package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.res.Resources
import android.opengl.GLES20.glGetError
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.light.AmbientLight
import com.mjaruijs.fischersplayground.opengl.light.DirectionalLight
import com.mjaruijs.fischersplayground.opengl.model.Entity
import com.mjaruijs.fischersplayground.opengl.model.Material
import com.mjaruijs.fischersplayground.opengl.model.Mesh
import com.mjaruijs.fischersplayground.opengl.model.MeshLoader
import com.mjaruijs.fischersplayground.opengl.renderer.animation.AnimationData
import com.mjaruijs.fischersplayground.opengl.renderer.animation.PieceAnimator
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.util.RenderThread
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.PI

class PieceRenderer(resources: Resources, isPlayerWhite: Boolean, private val requestRender: () -> Unit, private val runOnUiThread: (() -> Unit) -> Unit, private val requestGame: () -> Game) {

    private val quad = Quad()
    private val sampler = Sampler(0)

    private val piece2DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_2d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.piece_2d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val piece3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_3d_vertex, ShaderType.VERTEX, resources),
        ShaderLoader.load(R.raw.piece_3d_fragment, ShaderType.FRAGMENT, resources)
    )

    private val pieceTextures2D = HashMap<Piece, Int>()
    private val pieceTextures3D = HashMap<Piece, Int>()

//    private val pawnResources = MeshLoader.preload(resources, R.raw.pawn_bytes)
//    private val pawnMesh = pawnResources.second
//    private var pawnMesh: Mesh

    private val pawnMesh = MeshLoader.preload(resources, R.raw.pawn_bytes)
    private val bishopMesh = MeshLoader.preload(resources, R.raw.bishop_bytes)
    private val knightMesh = MeshLoader.preload(resources, R.raw.knight_bytes)
    private val rookMesh = MeshLoader.preload(resources, R.raw.rook_bytes)
    private val queenMesh = MeshLoader.preload(resources, R.raw.queen_bytes)
    private val kingMesh = MeshLoader.preload(resources, R.raw.king_bytes)

    private val pawn = Entity(pawnMesh)
    private val bishop = Entity(bishopMesh)
    private val knight = Entity(knightMesh)
    private val rook = Entity(rookMesh)
    private val queen = Entity(queenMesh)
    private val king = Entity(kingMesh)

    private val ambientLight = AmbientLight(Color.DARK)
    private val directionalLight = DirectionalLight(Color.WHITE, Vector3(0.0f, -0.5f, 1f))

    private val whiteMaterial = Material(Color(213f / 255f, 184f / 255f, 147f / 255f), Color(213f / 255f, 184f / 255f, 147f / 255f), Color.GREY, 25.0f)
    private val blackMaterial = Material(Color(66f / 255f, 40f / 255f, 14f / 255f), Color(66f / 255f, 40f / 255f, 14f / 255f), Color.GREY, 25.0f)

    private val whiteKnightRotation = if (isPlayerWhite) ROTATION_MATRIX else Matrix4()
    private val blackKnightRotation = if (isPlayerWhite) Matrix4() else ROTATION_MATRIX

    var pieceScale = Vector3(1f, 1f, 1f)

    private val animationQueue = PriorityQueue<AnimationData>()
    private val animationRunning = AtomicBoolean(false)

    private val takenPieces = ArrayList<Pair<Piece, Vector2>>()

    private val animationThread: Thread

    private val runAnimationThread = AtomicBoolean(true)

    init {
        animationThread = Thread {
            var currentAnimation: AnimationData? = null
            while (runAnimationThread.get()) {
                while (animationRunning.get()) {
                    Thread.sleep(1)
                }

                if (currentAnimation?.nextAnimation != null) {
                    currentAnimation = currentAnimation.nextAnimation!!
                    startAnimation(currentAnimation)
                } else {
                    if (animationQueue.isNotEmpty()) {
                        currentAnimation = animationQueue.poll()
                        startAnimation(currentAnimation)
                    }
                }
            }
        }
        animationThread.start()
    }

    private fun startAnimation(currentAnimation: AnimationData?) {
        if (currentAnimation == null) {
            return
        }

        animationRunning.set(true)
        if (currentAnimation.takenPiece != null) {
            takenPieces += Pair(currentAnimation.takenPiece, currentAnimation.takenPiecePosition!!)
        }

        runOnUiThread {
            val animator = PieceAnimator(requestGame().state, currentAnimation.piecePosition, currentAnimation.translation, requestRender, currentAnimation.onStart)
            animator.addOnFinishCall {
                animationRunning.set(false)
            }
            animator.addOnFinishCall {
                currentAnimation.onFinish()
            }
            animator.addOnFinishCall {
                takenPieces.remove(Pair(currentAnimation.takenPiece, currentAnimation.takenPiecePosition))
            }

            animator.start()
        }
    }

    fun queueAnimation(game: Game, animationData: AnimationData) {
        animationQueue.add(animationData)
    }

    private fun getPieceTexture2d(piece: Piece, pieceTextures: PieceTextures): Int {
        if (pieceTextures2D.contains(piece)) {
            return pieceTextures2D[piece]!!
        }

        pieceTextures2D[piece] = pieceTextures.get2DTextureId(piece.type, piece.team)

        return pieceTextures2D[piece]!!
    }

    private fun getPieceTexture3d(piece: Piece, pieceTextures: PieceTextures): Int {
        if (pieceTextures3D.contains(piece)) {
            return pieceTextures3D[piece]!!
        }

        pieceTextures3D[piece] = pieceTextures.get3DTextureId(piece.type)

        return pieceTextures3D[piece]!!
    }

    fun render2D(game: Game, pieceTextures: PieceTextures, aspectRatio: Float) {
        piece2DProgram.start()
        piece2DProgram.set("textureMaps", sampler.index)
        piece2DProgram.set("aspectRatio", aspectRatio)

        sampler.bind(pieceTextures.get2DTextureArray())

        for (pieceData in takenPieces) {
            val piece = pieceData.first
            val piecePosition = pieceData.second

            val translation = (Vector2(piecePosition.x * 2.0f, piecePosition.y * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f)
            piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4.0f)
            piece2DProgram.set("textureId", getPieceTexture2d(piece, pieceTextures).toFloat())
            piece2DProgram.set("translation", translation)

            quad.draw()
        }

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = game[x, y] ?: continue

                val row = x - piece.translation.x
                val col = y - piece.translation.y

                val translation = (Vector2(row * 2.0f, col * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f)

                piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4.0f)
                piece2DProgram.set("textureId", getPieceTexture2d(piece, pieceTextures).toFloat())
                piece2DProgram.set("translation", translation)
                quad.draw()
            }
        }

        piece2DProgram.stop()
    }

    fun render3D(game: Game, camera: Camera, pieceTextures: PieceTextures, aspectRatio: Float) {
        piece3DProgram.start()

        piece3DProgram.set("projection", camera.projectionMatrix)
        piece3DProgram.set("view", camera.viewMatrix)
        piece3DProgram.set("cameraPosition", camera.getPosition())
        piece3DProgram.set("textureMaps", sampler.index)
        piece3DProgram.set("aspectRatio", aspectRatio)

        ambientLight.applyTo(piece3DProgram)
        directionalLight.applyTo(piece3DProgram)

        sampler.bind(pieceTextures.get3DTextureArray())

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = game[x, y] ?: continue
                val row = x - piece.translation.x
                val col = y - piece.translation.y

                if (piece.team == Team.WHITE) {
                    whiteMaterial.applyTo(piece3DProgram)
                } else {
                    blackMaterial.applyTo(piece3DProgram)
                }

                val translation = (Vector2(row * 2.0f, col * 2.0f) / 8.0f) - Vector2(7f / 8f, 7f / 8f)

                piece3DProgram.set("isWhite", if (piece.team == Team.WHITE) 1f else 0f)
                piece3DProgram.set("textureId", getPieceTexture3d(piece, pieceTextures).toFloat())
//                piece3DProgram.set("translation", translation)

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

        piece3DProgram.stop()
    }

    fun destroy() {
        quad.destroy()
        pawnMesh.destroy()
        knightMesh.destroy()
        bishopMesh.destroy()
        rookMesh.destroy()
        queenMesh.destroy()
        kingMesh.destroy()
        piece2DProgram.destroy()
        piece3DProgram.destroy()
        runAnimationThread.set(false)
    }

    companion object {
        private val ROTATION_MATRIX = Matrix4().rotateZ(PI.toFloat())
//        private var instance: PieceRenderer? = null
//
//        fun getInstance(resources: Resources, isPlayerWhite: Boolean): PieceRenderer {
//            if (instance == null) {
//                instance = PieceRenderer(resources, isPlayerWhite)
//            }
//            return instance!!
//        }
    }
}