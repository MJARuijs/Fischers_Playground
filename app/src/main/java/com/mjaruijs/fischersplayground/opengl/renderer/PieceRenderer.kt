package com.mjaruijs.fischersplayground.opengl.renderer

import android.animation.ValueAnimator
import android.content.res.Resources
import androidx.core.animation.doOnEnd
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
import com.mjaruijs.fischersplayground.opengl.renderer.animation.TakenPieceData
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import com.mjaruijs.fischersplayground.opengl.texture.Sampler
import com.mjaruijs.fischersplayground.util.Logger
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
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

//    private val animator = MyPieceAnimator(requestRender)
    private val animationQueue = LinkedList<AnimationData>()
    private val animationRunning = AtomicBoolean(false)
    private val runAnimationThread = AtomicBoolean(true)
    private var animating = false
    private var animatingPiece: Piece? = null
    private var takenPieceAlpha = 1.0f

    private var takenPieceData: TakenPieceData? = null
    private var currentAnimationData: AnimationData? = null

    var pieceScale = Vector3(1f, 1f, 1f)

    init {
        Thread {
            while (runAnimationThread.get()) {
                while (animationRunning.get()) {
                    Thread.sleep(10)
                }

                if (animationQueue.isNotEmpty()) {
                    val currentAnimation = animationQueue.poll()
                    if (currentAnimation != null) {
                        startAnimation(currentAnimation)
                    } else {
                        Logger.warn(TAG, "Polled animation, but AnimationData was null..")
                    }
                }
            }
        }.start()
    }

    fun isAnimating() = animating

    private fun startAnimation(animationData: AnimationData) {
        val piece = requestGame().state[animationData.piecePosition] ?: throw IllegalArgumentException("No piece was found at square: ${animationData.piecePosition}.. Failed to animate..\n${requestGame().state}\n")
        currentAnimationData = animationData
        animationTranslation = null
        animatingPiece = piece

        counter = 0

        val translation = animationData.translation
        piece.translationOffset = translation
        animationData.invokeOnStartCalls()

        animationRunning.set(true)
        animating = true

        takenPieceAlpha = if (animationData.isReversed) {
            0.0f
        } else {
            1.0f
        }

        if (animationData.takenPiece != null) {
            val alpha = if (animationData.isReversed) 0.0f else 1.0f
            takenPieceData = TakenPieceData(animationData.takenPiece, animationData.takenPiecePosition!!, alpha)
        }

        Logger.debug(TAG, "Starting animation in state: ${requestGame().state}")

        val pieceAnimator = ValueAnimator.ofFloat(1.0f, 0.0f)
        pieceAnimator.addUpdateListener {
            val progress = it.animatedValue as Float
            piece.translationOffset = translation * progress
            requestRender()

            takenPieceAlpha = if (animationData.isReversed) {
                1.0f - it.animatedValue as Float
            } else {
                it.animatedValue as Float
            }
        }

        pieceAnimator.doOnEnd {
            animationData.invokeOnFinishCalls()

            if (animationData.nextAnimation != null) {
                startAnimation(animationData.nextAnimation!!)
            } else {
                animationRunning.set(false)
            }
            animating = false
            requestRender()
            takenPieceData = null
            currentAnimationData = null
        }
        pieceAnimator.duration = animationData.animationSpeed.toLong()

        runOnUiThread {
            pieceAnimator.start()
        }
    }

    fun queueAnimation(animationData: AnimationData) {
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
    var counter = 0

    private var animationTranslation: Vector2? = null

    fun render2D(game: Game, pieceTextures: PieceTextures, aspectRatio: Float) {
        piece2DProgram.start()
        piece2DProgram.set("textureMaps", sampler.index)
        piece2DProgram.set("aspectRatio", aspectRatio)

        sampler.bind(pieceTextures.get2DTextureArray())



        Logger.debug(TAG, "TakenPieceData: ${takenPieceData?.piece?.type}")

//        for (x in 0 until 8) {
//            for (y in 0 until 8) {
//                val piece = game[x, y] ?: continue


        // TODO: LOW PRIORITY: Use this code (or rewrite it) to fade out pieces that are taken (or fade them back in when a capturing move is undone)
//        if (takenPieceData != null) run {
//            val piece = takenPieceData?.piece ?: return@run
//            val piecePosition = takenPieceData?.position ?: return@run
//            val translation = (Vector2(piecePosition.x * 2.0f, piecePosition.y * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f) + Vector2(HALF_PIECE_SCALE, -HALF_PIECE_SCALE)
//
//            piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4f - Vector2(PIECE_SCALE_OFFSET, PIECE_SCALE_OFFSET))
//            piece2DProgram.set("textureId", getPieceTexture2d(piece, pieceTextures).toFloat())
//            piece2DProgram.set("translation", translation)
//            piece2DProgram.set("alpha", takenPieceAlpha)
//
//            quad.draw()
//        }

        for (piece in game.state.getPieces()) {
            val x = piece.square.x.roundToInt()
            val y = piece.square.y.roundToInt()
            val row = x - piece.translationOffset.x
            val col = y - piece.translationOffset.y
            val translation = (Vector2(row * 2.0f, col * 2.0f) / 8.0f) + Vector2(-1f, 1f / 4.0f - 1.0f) + Vector2(HALF_PIECE_SCALE, -HALF_PIECE_SCALE)

            Logger.warn(TAG, "Rendering animating piece at ${piece.translationOffset} row=$row col=$col x=$x y=$y. PreviousTranslation: $animationTranslation")

//                if (animatingPiece != null) {
//                    if (piece == animatingPiece) {
//                        if (counter < 2) {
//                            if (animationTranslation == null) {
//                                animationTranslation = piece.translation
//                            } else {
//                                if (animationTranslation != piece.translation) {
//                                    Logger.warn(TAG, "Rendering animating piece at $translation ${piece.translation} row=$row col=$col x=$x y=$y. PreviousTranslation: $animationTranslation")
//                                    Logger.warn(TAG, "Animating piece! GameState: ${requestGame().state}")
//                                    Logger.warn(TAG, "State of Game parameter: ${game.state}")
//                                }
//                            }
//                        }
//                        counter++
//                    }
//                }

                piece2DProgram.set("scale", Vector2(1.0f, 1.0f) / 4f - Vector2(PIECE_SCALE_OFFSET, PIECE_SCALE_OFFSET))
                piece2DProgram.set("textureId", getPieceTexture2d(piece, pieceTextures).toFloat())
                piece2DProgram.set("translation", translation)
                piece2DProgram.set("alpha", 1.0f)

                if (currentAnimationData != null) {
                    if (currentAnimationData!!.isReversed) {
                        if (takenPieceData != null) {
                            if (takenPieceData?.position?.x?.roundToInt() == x && takenPieceData?.position?.y?.roundToInt() == y) {
                                Logger.debug(TAG, "Rendering taken piece with alpha: $takenPieceAlpha")
                                piece2DProgram.set("alpha", takenPieceAlpha)
                            }
//                    if (takenPieceData!!.piece == piece) {
//                        Logger.debug(TAG, "Rendering taken piece with alpha: $takenPieceAlpha")
//                        piece2DProgram.set("alpha", takenPieceAlpha)
//                    }
                        }
                    }
                }

                quad.draw()
//            }
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
                val row = x - piece.translationOffset.x
                val col = y - piece.translationOffset.y

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