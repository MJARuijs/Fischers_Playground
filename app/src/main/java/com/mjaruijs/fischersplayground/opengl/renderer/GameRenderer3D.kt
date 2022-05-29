package com.mjaruijs.fischersplayground.opengl.renderer

import android.content.Context
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
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
import com.mjaruijs.fischersplayground.opengl.model.Material
import com.mjaruijs.fischersplayground.opengl.model.Mesh
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderLoader
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderProgram
import com.mjaruijs.fischersplayground.opengl.shaders.ShaderType
import java.util.concurrent.atomic.AtomicBoolean

class GameRenderer3D(context: Context) {

    private val isLocked = AtomicBoolean(false)
    private val piece3DProgram = ShaderProgram(
        ShaderLoader.load(R.raw.piece_3d_vertex, ShaderType.VERTEX, context),
        ShaderLoader.load(R.raw.piece_3d_fragment, ShaderType.FRAGMENT, context)
    )

    private val animations = ArrayList<AnimationValues>()

//    private val pawnMesh: Mesh
//    private val bishopMesh: Mesh
//    private val knightMesh: Mesh
//    private val rookMesh: Mesh
//    private val queenMesh: Mesh
//    private val kingMesh: Mesh

    private val ambientLight = AmbientLight(Color.DARK)
    private val directionalLight = DirectionalLight(Color.WHITE, Vector3(0.0f, -0.5f, 1f))

    private val whiteMaterial = Material(Color(0.8f, 0.8f, 0.8f), Color(0.8f, 0.8f, 0.8f), Color.BLACK, 10.0f)
    private val blackMaterial = Material(Color(0.2f, 0.2f, 0.2f), Color(0.2f, 0.2f, 0.2f), Color.BLACK, 10.0f)

    var pieceScale = Vector3(1f, 1f, 1f)

    init {
//        pawnMesh = Mesh(OBJLoader.get(context, R.raw.pawn))
//        bishopMesh = Mesh(OBJLoader.get(context, R.raw.bishop))
//        knightMesh = Mesh(OBJLoader.get(context, R.raw.knight))
//        rookMesh = Mesh(OBJLoader.get(context, R.raw.rook))
//        queenMesh = Mesh(OBJLoader.get(context, R.raw.queen))
//        kingMesh = Mesh(OBJLoader.get(context, R.raw.king))
    }

    fun render(game: Game, camera: Camera) {
        piece3DProgram.start()

        piece3DProgram.set("projection", camera.projectionMatrix)
        piece3DProgram.set("view", camera.viewMatrix)
        piece3DProgram.set("cameraPosition", camera.position)

        ambientLight.applyTo(piece3DProgram)
        directionalLight.applyTo(piece3DProgram)

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = game[row, col] ?: continue

                val animation = animations.find { animation -> animation.animatingRow == row && animation.animatingCol == col }

//                val translation = if (animation == null) {
                  val translation =  (Vector2(row .toFloat() , col  .toFloat()) / 8.0f) * 2.0f - 1.0f + Vector2(1/8f, 1/8f)
//                } else {
//                    (Vector2((row + animation.translation.x) * 2.0f, (col + animation.translation.y)  * 2.0f) / 8.0f)
//                }

                piece3DProgram.set("model", Matrix4().translate(translation).scale(pieceScale))

                if (piece.team == Team.WHITE) {
                    whiteMaterial.applyTo(piece3DProgram)
                } else {
                    blackMaterial.applyTo(piece3DProgram)
                }

                when (piece.type) {
//                    PieceType.PAWN -> pawnMesh.draw()
//                    PieceType.BISHOP -> bishopMesh.draw()
//                    PieceType.KNIGHT -> knightMesh.draw()
//                    PieceType.ROOK -> rookMesh.draw()
//                    PieceType.QUEEN -> queenMesh.draw()
//                    PieceType.KING -> kingMesh.draw()
                }

            }
        }

        piece3DProgram.stop()
    }
}