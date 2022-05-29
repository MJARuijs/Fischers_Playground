package com.mjaruijs.fischersplayground.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.GamePreviewSurface
import com.mjaruijs.fischersplayground.userinterface.UIButton
import kotlin.math.roundToInt

@Suppress("SameParameterValue")
class SettingsActivity : AppCompatActivity() {

    private lateinit var glView: GamePreviewSurface
    private lateinit var game: SinglePlayerGame
    private lateinit var fovSeekbar: SeekBar
    private lateinit var pieceScaleSeekbar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        PieceTextures.init(this)

        glView = findViewById(R.id.opengl_view)
        glView.init(::onContextCreated, ::onCameraRotated)


        val positionUpButton = findViewById<UIButton>(R.id.positionUpButton)
        val positionDownButton = findViewById<UIButton>(R.id.positionDownButton)

        val positionForwardButton = findViewById<UIButton>(R.id.positionForwardButton)
        val positionBackwardButton = findViewById<UIButton>(R.id.positionBackwardButton)

        fovSeekbar = findViewById(R.id.fov_seekbar)
        fovSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualProgress = progress * 5 + 20
                glView.getRenderer().setFoV(actualProgress)
                glView.requestRender()
                savePreference(FOV_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        pieceScaleSeekbar = findViewById(R.id.piece_scale_seekbar)
        pieceScaleSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                println("PROGRES: $progress")
                val actualProgress = progress.toFloat() / 100.0f
                glView.getRenderer().setPieceScale(actualProgress)
                glView.requestRender()
                savePreference(PIECE_SCALE_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })


        val translationSpeed = 10f

        positionUpButton.setCornerRadius(20f)
            .setCenterVertically(true)
            .setColor(Color.rgb(235, 186, 145))
            .setTexturedDrawable(R.drawable.arrow_up)
            .setOnHoldListener {
                glView.getRenderer().translateCameraY(-translationSpeed)
                glView.requestRender()
            }
            .setOnReleaseListener {
                savePreference(CAMERA_POSITION_KEY, glView.getRenderer().getCameraPosition())
            }

        positionDownButton.setCornerRadius(20f)
            .setCenterVertically(true)
            .setColor(Color.rgb(235, 186, 145))
            .setTexturedDrawable(R.drawable.arrow_down)
            .setOnHoldListener {
                glView.getRenderer().translateCameraY(translationSpeed)
//                println(glView.getRenderer().getCameraPosition())
                glView.requestRender()
            }
            .setOnReleaseListener {
                savePreference(CAMERA_POSITION_KEY, glView.getRenderer().getCameraPosition())
//                println("SAVING ${glView.getRenderer().getCameraPosition()}")
            }

        positionForwardButton.setCornerRadius(20f)
            .setCenterVertically(true)
            .setColor(Color.rgb(235, 186, 145))
            .setTexturedDrawable(R.drawable.arrow_up)
            .setOnHoldListener {
                glView.getRenderer().translateCameraZ(-translationSpeed)
                glView.requestRender()
            }
            .setOnReleaseListener {
                savePreference(CAMERA_POSITION_KEY, glView.getRenderer().getCameraPosition())
            }

        positionBackwardButton.setCornerRadius(20f)
            .setCenterVertically(true)
            .setColor(Color.rgb(235, 186, 145))
            .setTexturedDrawable(R.drawable.arrow_down)
            .setOnHoldListener {
                glView.getRenderer().translateCameraZ(translationSpeed)
                glView.requestRender()
            }
            .setOnReleaseListener {
                savePreference(CAMERA_POSITION_KEY, glView.getRenderer().getCameraPosition())
            }
    }

    private fun onContextCreated() {
        game = SinglePlayerGame()
        glView.setGame(game)

        restorePreferences()
    }

    private fun onCameraRotated() {
        savePreference(CAMERA_ROTATION_KEY, glView.getRenderer().getCameraRotation())
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        val cameraPosition = preferences.getString(CAMERA_POSITION_KEY, "") ?: ""
        val cameraRotation = preferences.getString(CAMERA_ROTATION_KEY, "") ?: ""
        val fov = preferences.getInt(FOV_KEY, 45)
        val pieceScale = preferences.getFloat(PIECE_SCALE_KEY, 1.0f)

        if (cameraPosition.isNotBlank()) {
            glView.getRenderer().setCameraPosition(Vector3.fromString(cameraPosition))
        }

        if (cameraRotation.isNotBlank()) {
            glView.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
        }

        glView.getRenderer().setFoV(fov)
        fovSeekbar.progress = (fov - 20) / 5

        glView.getRenderer().setPieceScale(pieceScale)
        pieceScaleSeekbar.progress = (pieceScale * 100).roundToInt()
    }

    private fun savePreference(key: String, value: Vector3) {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        with(preferences.edit()) {
            putString(key, value.toString())
            apply()
        }
    }

    private fun savePreference(key: String, value: Int) {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        with(preferences.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun savePreference(key: String, value: Float) {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        with(preferences.edit()) {
            putFloat(key, value)
            apply()
        }
    }

    companion object {
        internal const val CAMERA_POSITION_KEY = "camera_position"
        internal const val CAMERA_ROTATION_KEY = "camera_rotation"
        internal const val FOV_KEY = "camera_fov"
        internal const val PIECE_SCALE_KEY = "piece_scale"
    }
}