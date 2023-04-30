package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.res.Resources
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.pieces.Piece
import com.mjaruijs.fischersplayground.opengl.texture.PieceTextures
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
import com.mjaruijs.fischersplayground.opengl.renderer.animation.MyPieceAnimator
import com.mjaruijs.fischersplayground.opengl.renderer.animation.TakenPieceData
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.util.Logger
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.math.PI
import kotlin.math.roundToInt

class PieceRenderer(resources: Resources, isPlayerWhite: Boolean, private val requestRender: () -> Unit, private val runOnUiThread: (() -> Unit) -> Unit, private val requestGame: () -> Game, private val onExceptionThrown: (String, Exception) -> Unit) {

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

//    private val pawnMesh = MeshLoader.preload(resources, R.raw.pawn_bytes)
//    private val bishopMesh = MeshLoader.preload(resources, R.raw.bishop_bytes)
//    private val knightMesh = MeshLoader.preload(resources, R.raw.knight_bytes)
//    private val rookMesh = MeshLoader.preload(resources, R.raw.rook_bytes)
//    private val queenMesh = MeshLoader.preload(resources, R.raw.queen_bytes)
//    private val kingMesh = MeshLoader.preload(resources, R.raw.king_bytes)
//
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

    private val animator = MyPieceAnimator(requestRender)
    private val animationQueue = LinkedList<AnimationData>()
    private val animationRunning = AtomicBoolean(false)
    private val runAnimationThread = AtomicBoolean(true)

    private var takenPieceData: TakenPieceData? = null

//    private val animationThread: Thread

    var pieceScale = Vector3(1f, 1f, 1f)

    init {
        Thread {
            var currentAnimation: AnimationData? = null
            while (runAnimationThread.get()) {
                while (animationRunning.get()) {
//                    Logger.debug(TAG, "Animation Running")
                    Thread.sleep(1)
                }

                if (currentAnimation?.nextAnimation != null) {
                    currentAnimation = currentAnimation.nextAnimation!!
                    startAnimation(currentAnimation)
                } else {
                    if (animationQueue.isNotEmpty()) {
                        currentAnimation = animationQueue.poll()
                        Logger.debug(TAG, "Polling new animation!")
                        startAnimation(currentAnimation)
                    }
                }
            }
        }.start()
//        animationThread.start()
    }

    private fun startAnimation(currentAnimation: AnimationData?) {
        if (currentAnimation == null) {
            Logger.warn(TAG, "Tried to play animation but was null..")
            return
        }

        animationRunning.set(true)
        if (currentAnimation.takenPiece != null) {
            val alpha = if (currentAnimation.isReversed) 0.0f else 1.0f
            takenPieceData = TakenPieceData(currentAnimation.takenPiece, currentAnimation.takenPiecePosition!!, alpha)
        }

        try {
//            val animator = PieceAnimator(requestGame().state, currentAnimation.piecePosition, currentAnimation.translation, requestRender, currentAnimation.onStartCalls, currentAnimation.onFinishCalls, currentAnimation.animationSpeed)
            val onFinishCalls = ArrayList<() -> Unit>()
            onFinishCalls.addAll(currentAnimation.onFinishCalls)
            onFinishCalls += {
                animationRunning.set(false)
                takenPieceData = null
                requestRender()
            }
//            animator.addOnFinishCall(
//                {
//                    animationRunning.set(false)
//                    Logger.debug(TAG, "Finished animating ${requestGame().state[currentAnimation.piecePosition]?.type}")
//                },
//                {
//                    takenPieceData = null
//                    requestRender()
//                }
//            )

            runOnUiThread {
                val piece = requestGame().state[currentAnimation.piecePosition]
                if (piece == null) {
                    Logger.error(TAG, "Tried to animate piece from ${currentAnimation.piecePosition} to ${currentAnimation.piecePosition + currentAnimation.translation}, but no piece was found at the starting square..")
                    return@runOnUiThread
                }

                Logger.debug(TAG, "Playing animation: moving ${piece.type} from ${vectorToChessSquares(currentAnimation.piecePosition)} to ${vectorToChessSquares(currentAnimation.translation + currentAnimation.piecePosition)}")
                animator.startAnimation(requestGame().state, currentAnimation.piecePosition, currentAnimation.translation, currentAnimation.onStartCalls, onFinishCalls, currentAnimation.animationSpeed)
//                animator.start()
            }
        } catch (e: Exception) {
            onExceptionThrown("crash_piece_renderer_start_animation.txt", e)
        }
    }

    fun update(deltaTime: Float) {
        animator.update(deltaTime)
    }

    private fun vectorToChessSquares(position: Vector2): String {
        var square = ""

        square += when (position.x.roundToInt()) {
            0 -> "a"
            1 -> "b"
            2 -> "c"
            3 -> "d"
            4 -> "e"
            5 -> "f"
            6 -> "g"
            7 -> "h"
            else -> ""
        }

        square += position.y.roundToInt() + 1
        return square
    }

    fun queueAnimation(animationData: AnimationData) {
        Logger.debug(TAG, "Queued animation: ${animationData.piecePosition}")
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

        if (takenPieceData != null) run {
            val piece = takenPieceData?.piece ?: return@run
            val piecePosition = takenPieceData?.position ?: return@run

            val translation = (Vector2(piecePosition.x * 2.0f, piecePosition.y * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f) + Vector2(HALF_PIECE_SCALE, -HALF_PIECE_SCALE)

            piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4f - Vector2(PIECE_SCALE_OFFSET, PIECE_SCALE_OFFSET))
            piece2DProgram.set("textureId", getPieceTexture2d(piece, pieceTextures).toFloat())
            piece2DProgram.set("translation", translation)

            quad.draw()
        }

        for (x in 0 until 8) {
            for (y in 0 until 8) {
                val piece = game[x, y] ?: continue

                val row = x - piece.translation.x
                val col = y - piece.translation.y

                val translation = (Vector2(row * 2.0f, col * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f) + Vector2(HALF_PIECE_SCALE, -HALF_PIECE_SCALE)

                piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4f - Vector2(PIECE_SCALE_OFFSET, PIECE_SCALE_OFFSET))
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
                piece3DProgram.set("translation", translation)

//                when (piece.type) {
//                    PieceType.PAWN -> pawn.render(piece3DProgram, translation, pieceScale)
//                    PieceType.BISHOP -> bishop.render(piece3DProgram, translation, pieceScale)
//                    PieceType.KNIGHT -> {
//                        val rotation = if (piece.team == Team.WHITE) whiteKnightRotation else blackKnightRotation
//                        knight.render(piece3DProgram, translation, rotation, pieceScale)
//                    }
//                    PieceType.ROOK -> rook.render(piece3DProgram, translation, pieceScale)
//                    PieceType.QUEEN -> queen.render(piece3DProgram, translation, pieceScale)
//                    PieceType.KING -> king.render(piece3DProgram, translation, pieceScale)
//                }
            }
        }

        piece3DProgram.stop()
    }

    fun destroy() {
        quad.destroy()
//        pawnMesh.destroy()
//        knightMesh.destroy()
//        bishopMesh.destroy()
//        rookMesh.destroy()
//        queenMesh.destroy()
//        kingMesh.destroy()
        piece2DProgram.destroy()
        piece3DProgram.destroy()
        runAnimationThread.set(false)
    }

    companion object {
        private const val TAG = "PieceRenderer"
        private val ROTATION_MATRIX = Matrix4().rotateZ(PI.toFloat())
        private const val PIECE_SCALE_OFFSET = 0.03f
        private const val HALF_PIECE_SCALE = PIECE_SCALE_OFFSET / 2f
    }
}