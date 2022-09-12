package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.res.Resources
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.math.Color
import com.mjaruijs.fischersplayground.math.matrices.Matrix4
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.Camera
import com.mjaruijs.fischersplayground.opengl.Quad
import com.mjaruijs.fischersplayground.opengl.light.AmbientLight
import com.mjaruijs.fischersplayground.opengl.light.DirectionalLight
import com.mjaruijs.fischersplayground.opengl.model.Material
import com.mjaruijs.fischersplayground.opengl.renderer.animation.AnimationData
import com.mjaruijs.fischersplayground.opengl.renderer.animation.PieceAnimator
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.HashMap
import kotlin.math.PI

class PieceRenderer(resources: Resources, isPlayerWhite: Boolean, private val requestRender: () -> Unit, private val runOnUiThread: (() -> Unit) -> Unit) {

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

//    private val pawnMesh = MeshLoader.preload(resources, R.raw.pawn_bytes).second
//    private val bishopMesh = MeshLoader.preload(resources, R.raw.bishop_bytes).second
//    private val knightMesh = MeshLoader.preload(resources, R.raw.knight_bytes).second
//    private val rookMesh = MeshLoader.preload(resources, R.raw.rook_bytes).second
//    private val queenMesh = MeshLoader.preload(resources, R.raw.queen_bytes).second
//    private val kingMesh = MeshLoader.preload(resources, R.raw.king_bytes).second

//    private val pawn = Entity(pawnMesh)
//    private val bishop = Entity(bishopMesh)
//    private val knight = Entity(knightMesh)
//    private val rook = Entity(rookMesh)
//    private val queen = Entity(queenMesh)
//    private val king = Entity(kingMesh)

    private val ambientLight = AmbientLight(Color.DARK)
    private val directionalLight = DirectionalLight(Color.WHITE, Vector3(0.0f, -0.5f, 1f))

    private val whiteMaterial = Material(Color(213f / 255f, 184f / 255f, 147f / 255f), Color(213f / 255f, 184f / 255f, 147f / 255f), Color.GREY, 25.0f)
    private val blackMaterial = Material(Color(66f / 255f, 40f / 255f, 14f / 255f), Color(66f / 255f, 40f / 255f, 14f / 255f), Color.GREY, 25.0f)

    private val whiteKnightRotation = if (isPlayerWhite) ROTATION_MATRIX else Matrix4()
    private val blackKnightRotation = if (isPlayerWhite) Matrix4() else ROTATION_MATRIX

    var pieceScale = Vector3(1f, 1f, 1f)

    private val animationQueue = PriorityQueue<AnimationData>()
    private val animationRunning = AtomicBoolean(false)

    var rChannel = 1.0f
    var gChannel = 1.0f
    var bChannel = 1.0f

    init {
        Thread {
            while (true) {
                while (animationRunning.get()) {
                    Thread.sleep(1)
                }

                if (animationQueue.isNotEmpty()) {
                    val animationData = animationQueue.poll()!!
                    animationRunning.set(true)
//                    println("Starting animation: ${animationData.piece.type} ${animationData.fromPosition} ${animationData.toPosition}")
                    animationData.piece.translation = animationData.toPosition - animationData.fromPosition
                    val animator = PieceAnimator(animationData.piece, requestRender, animationData.onAnimationFinished)
                    animator.addOnFinishCall {
                        animationRunning.set(false)
                    }
                    runOnUiThread {
                        animator.start()
                    }
                }

            }
        }.start()
    }

    fun queueAnimation(game: Game, animationData: AnimationData) {
//        println("Queuing animation: ${animationData.piece.type} ${animationData.piece.team} ${animationData.piece.boardPosition} ${animationData.piece.animatedPosition}")
        animationQueue.add(animationData)
    }

    private fun getPieceTexture2d(piece: Piece): Int {
        if (pieceTextures2D.contains(piece)) {
            return pieceTextures2D[piece]!!
        }

        pieceTextures2D[piece] = PieceTextures.get2DTextureId(piece.type, piece.team)

        return pieceTextures2D[piece]!!
    }

    private fun getPieceTexture3d(piece: Piece): Int {
        if (pieceTextures3D.contains(piece)) {
            return pieceTextures3D[piece]!!
        }

        pieceTextures3D[piece] = PieceTextures.get3DTextureId(piece.type)

        return pieceTextures3D[piece]!!
    }

    fun render2D(game: Game, aspectRatio: Float) {
//        startAnimations(game)

        piece2DProgram.start()
        piece2DProgram.set("textureMaps", sampler.index)
//        piece2DProgram.set("aspectRatio", aspectRatio)

        sampler.bind(PieceTextures.get2DTextureArray())

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = game[x, y] ?: continue

//        println("RENDERING")

//        val pieces = game.getPieces().iterator()
//        for (piece in pieces) {
//            val position = piece.animatedPosition

            val row = x - piece.getAnimatedX()
            val col = y - piece.getAnimatedY()

//            println("${piece.type} ${piece.team} $position ${piece.boardPosition}")

//                if (piece.type == PieceType.QUEEN && piece.team == Team.WHITE) {
//                    println("Piece: ${piece.type} ${piece.team} $row $col")
//                }
//
//                val animation = runningAnimations[Vector2(row, col)]
//                if (runningAnimations.size != 0) {
////                    println("SIZE: ${runningAnimations.size}. Looking for $row $col")
//                    if (piece.type == PieceType.QUEEN && piece.team == Team.WHITE) {
//                        for (entry in runningAnimations) {
//                            println("${entry.key} : ${entry.value} $row $col")
//                        }
//                    }
//                }
//                if (animation != null) {
//                    if (piece.type == PieceType.QUEEN && piece.team == Team.WHITE) {
//                        println("Actual value: ${animation?.x} ${animation?.y}")
//                    }
//                }
//                val animation = animations.find { animation -> animation.animatingRow == row && animation.animatingCol == col }

//                val animation = animations.find { animation -> animation.row == row && animation.col == col }

//                val translation = if (animation == null) {
//                    (Vector2(row * 2.0f, col * 2.0f) / 8.0f) + Vector2(-1.0f, 1.0f / 4.0f - 1.0f)
//                } else {
//                    (Vector2((row + animation.x) * 2.0f, (col + animation.y) * 2.0f) / 8.0f) + Vector2(-1.0f, 1.0f / 4.0f - 1.0f)
//                }


            val translation = (Vector2(row * 2.0f, col * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f)

//                if (animation != null) {
//                    println("Translation: $translation")
//                }

//                val animator = ObjectAnimator.ofFloat(0.0f, 1.0f)
//                animator.addUpdateListener({
//                    it.
//                })

                piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4.0f)
                piece2DProgram.set("textureId", getPieceTexture2d(piece).toFloat())
                piece2DProgram.set("translation", translation)
                quad.draw()
            }
        }

//        game.unlockData()

//        for (animation in animations) {
//            if (animation.stopAnimating) {
//                animation.onFinish()
//            }
//        }

//        animations.removeIf { animation -> animation.stopAnimating }

        piece2DProgram.stop()
    }

    fun render3D(game: Game, camera: Camera, aspectRatio: Float) {
//        startAnimations(game)

        piece3DProgram.start()
        piece3DProgram.set("projection", camera.projectionMatrix)
        piece3DProgram.set("view", camera.viewMatrix)
        piece3DProgram.set("cameraPosition", camera.getPosition())
        piece3DProgram.set("textureMaps", sampler.index)
//        piece3DProgram.set("aspectRatio", aspectRatio)

        piece3DProgram.set("rChannel", rChannel)
        piece3DProgram.set("gChannel", gChannel)
        piece3DProgram.set("bChannel", bChannel)

        ambientLight.applyTo(piece3DProgram)
        directionalLight.applyTo(piece3DProgram)

        sampler.bind(PieceTextures.get3DTextureArray())

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = game[row, col] ?: continue
//        val pieces = game.getPieces()
//        game.unlockData()

//        for (piece in pieces) {
//                val animation = animations.find { animation -> animation.row == row && animation.col == col }

//                val translation = if (animation == null) {
//                    (Vector2(row, col) / 8.0f) * 2.0f - 1.0f + Vector2(1 / 8f, 1 / 8f)
//                } else {
//                    (Vector2(row + animation.translation.x, col + animation.translation.y) / 8.0f) * 2.0f - 1.0f + Vector2(1 / 8f, 1 / 8f)
//                }

                if (piece.team == Team.WHITE) {
                    whiteMaterial.applyTo(piece3DProgram)
                } else {
                    blackMaterial.applyTo(piece3DProgram)
                }

                piece3DProgram.set("isWhite", if (piece.team == Team.WHITE) 1f else 0f)
                piece3DProgram.set("textureId", getPieceTexture3d(piece).toFloat())

//                println("Mesh trying to render on thread: ${Thread.currentThread().id}")

                when (piece.type) {
//                    PieceType.PAWN -> pawn.render(piece3DProgram, translation, pieceScale)
//                    PieceType.BISHOP -> bishop.render(piece3DProgram, translation, pieceScale)
//                    PieceType.KNIGHT -> {
//                        val rotation = if (piece.team == Team.WHITE) whiteKnightRotation else blackKnightRotation
//                        knight.render(piece3DProgram, translation, rotation, pieceScale)
//                    }
//                    PieceType.ROOK -> rook.render(piece3DProgram, translation, pieceScale)
//                    PieceType.QUEEN -> queen.render(piece3DProgram, translation, pieceScale)
//                    PieceType.KING -> king.render(piece3DProgram, translation, pieceScale)
                }
            }
        }

//        for (animation in animations) {
//            if (animation.stopAnimating) {
//                animation.onFinish()
//            }
//        }

//        animations.removeIf { animation -> animation.stopAnimating }

//        piece3DProgram.stop()
    }

//    fun update(delta: Float): Boolean {
//        for (animation in animations) {
//            val increment = animation.totalDistance * delta * 5f
//
//            if (abs(animation.translation.x) < abs(increment.x) || abs(animation.translation.y) < abs(increment.y)) {
//                animation.translation = Vector2()
//                animation.stopAnimating = true
//            } else {
//                animation.translation -= increment
//            }
//        }
//
//        return animations.isNotEmpty()
//    }

    fun destroy() {
//        println("destroying pieceRenderer")
        quad.destroy()
//        pawnMesh.destroy()
//        knightMesh.destroy()
//        bishopMesh.destroy()
//        rookMesh.destroy()
//        queenMesh.destroy()
//        kingMesh.destroy()
        piece2DProgram.destroy()
        piece3DProgram.destroy()
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