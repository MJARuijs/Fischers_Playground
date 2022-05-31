package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.SettingsActivity
import com.mjaruijs.fischersplayground.activities.SettingsActivity.Companion.CAMERA_ROTATION_KEY
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.GameSettingsSurface

@Suppress("SameParameterValue")
class GraphicSettingsDialog {

    private lateinit var dialog: Dialog

    private lateinit var glView: GameSettingsSurface
    private lateinit var game: SinglePlayerGame
//    private lateinit var fovSeekbar: SeekBar
//    private lateinit var pieceScaleSeekbar: SeekBar

//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

    fun create(context: Activity) {
//        val view = inflater.inflate(R.layout.graphics_settings_layout, null)

        dialog = Dialog(context)
        dialog.setContentView(R.layout.graphics_settings_layout)

        glView = dialog.findViewById(R.id.opengl_view)
        glView.init(::onContextCreated, ::onCameraRotated, ::savePreference)
//
//        fovSeekbar = dialog.findViewById(R.id.fov_seekbar)
//        fovSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                val actualProgress = progress * 5 + 20
////                glView.getRenderer().setFoV(actualProgress)
////                glView.requestRender()
//                savePreference(SettingsActivity.FOV_KEY, actualProgress)
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//            }
//        })
//
//        pieceScaleSeekbar = dialog.findViewById(R.id.piece_scale_seekbar)
//        pieceScaleSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                val actualProgress = progress.toFloat() / 100.0f
////                glView.getRenderer().setPieceScale(actualProgress)
////                glView.requestRender()
//                savePreference(SettingsActivity.PIECE_SCALE_KEY, actualProgress)
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//            }
//
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//            }
//        })

        setLayout()
//        return view
    }

    fun setLayout() {
//        dialog.window?.apply {
//            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        }
    }

    fun show() {
        println("SHOWING")
        dialog.show()

//        glView.requestRender()

    }

    private fun onContextCreated() {
        game = SinglePlayerGame()
        glView.setGame(game)

        restorePreferences()
    }

    private fun onCameraRotated() {
//        savePreference(CAMERA_ROTATION_KEY, glView.getRenderer().getCameraRotation())
    }

    private fun restorePreferences() {
        val preferences = getPreferences("graphics_preferences")

        val cameraRotation = preferences.getString(CAMERA_ROTATION_KEY, "") ?: ""
        val fov = preferences.getInt(SettingsActivity.FOV_KEY, 45)
        val pieceScale = preferences.getFloat(SettingsActivity.PIECE_SCALE_KEY, 1.0f)

//        if (cameraRotation.isNotBlank()) {
//            glView.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
//        }
//
//        glView.getRenderer().setFoV(fov)
//        fovSeekbar.progress = (fov - 20) / 5
//
//        glView.getRenderer().setPieceScale(pieceScale)
//        pieceScaleSeekbar.progress = (pieceScale * 100).roundToInt()
    }

    private fun getPreferences(name: String) = dialog.context.getSharedPreferences(name, AppCompatActivity.MODE_PRIVATE)

    private fun savePreference(key: String, value: Vector3) {
        val preferences = getPreferences("graphics_preferences")

        with(preferences.edit()) {
            putString(key, value.toString())
            apply()
        }
    }

    private fun savePreference(key: String, value: Int) {
        val preferences = getPreferences("graphics_preferences")

        with(preferences.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun savePreference(key: String, value: Float) {
        val preferences = getPreferences("graphics_preferences")

        with(preferences.edit()) {
            putFloat(key, value)
            apply()
        }
    }

}