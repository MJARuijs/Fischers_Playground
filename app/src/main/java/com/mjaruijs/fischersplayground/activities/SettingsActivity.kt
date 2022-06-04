package com.mjaruijs.fischersplayground.activities

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.GameSettingsSurface
import kotlin.math.roundToInt

@Suppress("SameParameterValue", "ControlFlowWithEmptyBody")
class SettingsActivity : AppCompatActivity() {

    private lateinit var game: SinglePlayerGame

    private lateinit var glView3D: GameSettingsSurface

    private lateinit var card2D: CardView
    private lateinit var card3D: CardView
    private lateinit var innerCard3D: CardView
    private lateinit var fullScreenCard: CardView

    private lateinit var pieceSizeText: TextView
    private lateinit var pieceScaleSeekbar: SeekBar
    private lateinit var fovText: TextView
    private lateinit var fovSeekbar: SeekBar

    private lateinit var fullScreenCheckbox: CheckBox

    private lateinit var collapseCardButton: ImageView
    private lateinit var graphicsSettingsButton: ImageView
    private lateinit var settingsLayout: ConstraintLayout
    private lateinit var graphics3DLayout: ConstraintLayout
    private lateinit var graphicsControlsLayout: ConstraintLayout

    private val defaultGraphicsConstraints = ConstraintSet()
    private val expandedGraphicsConstraints = ConstraintSet()

    private val defaultSettingsConstraints = ConstraintSet()
    private val expandedSettingsConstraints = ConstraintSet()

    private var is3D = false
    private var expanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        PieceTextures.init(this)

        settingsLayout = findViewById(R.id.settings_layout)
        graphics3DLayout = findViewById(R.id.graphics_3d_layout)
        graphicsControlsLayout = findViewById(R.id.graphics_controls_layout)
        graphicsSettingsButton = findViewById(R.id.graphics_settings_button)

        collapseCardButton = findViewById(R.id.collapse_card_button)
        card2D = findViewById(R.id.graphics_2d_card)
        card3D = findViewById(R.id.graphics_3d_card)
        innerCard3D = findViewById(R.id.inner_card_3D)
        fullScreenCard = findViewById(R.id.full_screen_card)

        pieceSizeText = findViewById(R.id.piece_scale_text)
        pieceScaleSeekbar = findViewById(R.id.piece_scale_seekbar)
        fovText = findViewById(R.id.fov_text)
        fovSeekbar = findViewById(R.id.fov_seekbar)
        fullScreenCheckbox = findViewById(R.id.full_screen_checkbox)

        setupUIElements()

        card2D.setOnClickListener {
            is3D = false
            selectGraphicsType()
        }

        card3D.setOnClickListener {
            is3D = true
            selectGraphicsType()
        }

        glView3D = findViewById(R.id.preview_3d)
        glView3D.init(::onContextCreated, ::onCameraRotated, ::savePreference)
        glView3D.getRenderer().set3D(true)

        defaultGraphicsConstraints.clone(this, R.layout.graphics_settings)
        expandedGraphicsConstraints.clone(this, R.layout.graphics_settings_expanded)

        defaultSettingsConstraints.clone(this, R.layout.activity_settings)
        expandedSettingsConstraints.clone(this, R.layout.activity_settings_expanded)

        restoreFullscreenPreference()
        restore3DPreference()
    }

    override fun onBackPressed() {
        if (expanded) {
            collapse()
        } else {
            super.onBackPressed()
        }
    }

    @SuppressLint("Recycle")
    private fun expand() {
        val animators = ArrayList<ObjectAnimator>()

        animators += ObjectAnimator.ofFloat(collapseCardButton, "alpha", 1.0f)
        animators += ObjectAnimator.ofFloat(graphicsSettingsButton, "alpha", 0.0f)
        animators += ObjectAnimator.ofFloat(pieceSizeText, "alpha", 1.0f)
        animators += ObjectAnimator.ofFloat(pieceScaleSeekbar, "alpha", 1.0f)
        animators += ObjectAnimator.ofFloat(fovText, "alpha", 1.0f)
        animators += ObjectAnimator.ofFloat(fovSeekbar, "alpha", 1.0f)

        val transition = ChangeBounds()
        transition.duration = ANIMATION_DURATION

        expandedGraphicsConstraints.applyTo(graphics3DLayout)
        expandedSettingsConstraints.applyTo(settingsLayout)
        TransitionManager.beginDelayedTransition(settingsLayout, transition)

        for (animator in animators) {
            animator.duration = ANIMATION_DURATION
            animator.start()
        }

        glView3D.isActive = true
        expanded = true
    }

    @SuppressLint("Recycle")
    private fun collapse() {
        val animators = ArrayList<ObjectAnimator>()

        animators += ObjectAnimator.ofFloat(collapseCardButton, "alpha", 0.0f)
        animators += ObjectAnimator.ofFloat(graphicsSettingsButton, "alpha", 1.0f)
        animators += ObjectAnimator.ofFloat(collapseCardButton, "alpha", 0.0f)
        animators += ObjectAnimator.ofFloat(graphicsSettingsButton, "alpha", 1.0f)
        animators += ObjectAnimator.ofFloat(pieceSizeText, "alpha", 0.0f)
        animators += ObjectAnimator.ofFloat(pieceScaleSeekbar, "alpha", 0.0f)
        animators += ObjectAnimator.ofFloat(fovText, "alpha", 0.0f)
        animators += ObjectAnimator.ofFloat(fovSeekbar, "alpha", 0.0f)

        val transition = ChangeBounds()
        transition.duration = ANIMATION_DURATION

        defaultGraphicsConstraints.applyTo(graphics3DLayout)
        defaultSettingsConstraints.applyTo(settingsLayout)
        TransitionManager.beginDelayedTransition(settingsLayout, transition)

        for (animator in animators) {
            animator.duration = ANIMATION_DURATION
            animator.start()
        }

        glView3D.isActive = false
        expanded = false
    }

    private fun resetLayout() {
        if (expanded) {
            expandedGraphicsConstraints.applyTo(graphics3DLayout)
            expandedSettingsConstraints.applyTo(settingsLayout)
            collapseCardButton.alpha = 1.0f
            graphicsSettingsButton.alpha = 0.0f
        } else {
            defaultGraphicsConstraints.applyTo(graphics3DLayout)
            defaultSettingsConstraints.applyTo(settingsLayout)
            collapseCardButton.alpha = 0.0f
            graphicsSettingsButton.alpha = 1.0f
        }
    }

    private fun onContextCreated() {
        runOnUiThread {
            glView3D.holder.setFixedSize(getDisplayWidth(), getDisplayWidth())

            game = SinglePlayerGame()
            glView3D.setGame(game)
            restorePreferences()
        }
    }

    private fun onCameraRotated() {
        savePreference(CAMERA_ROTATION_KEY, glView3D.getRenderer().getCameraRotation())
    }

    private fun restoreFullscreenPreference() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val fullScreen = preferences.getBoolean(FULL_SCREEN_KEY, false)

        hideActivityDecorations(fullScreen)
        fullScreenCheckbox.isChecked = fullScreen
    }

    private fun restore3DPreference() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        is3D = preferences.getBoolean(GRAPHICS_3D_KEY, false)
        selectGraphicsType()
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        val cameraRotation = preferences.getString(CAMERA_ROTATION_KEY, "") ?: ""
        val fov = preferences.getInt(FOV_KEY, 45)
        val pieceScale = preferences.getFloat(PIECE_SCALE_KEY, 1.0f)
        val zoom = preferences.getFloat(CAMERA_ZOOM_KEY, 4.0f)

        if (cameraRotation.isNotBlank()) {
            glView3D.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
        }

        glView3D.getRenderer().setFoV(fov)
        fovSeekbar.progress = (fov - FOV_OFFSET) / FOV_SCALE

        glView3D.getRenderer().setPieceScale(pieceScale)
        pieceScaleSeekbar.progress = (pieceScale * 100).roundToInt()

        glView3D.getRenderer().setCameraZoom(zoom)
    }

    private fun selectGraphicsType() {
        if (is3D) {
            card2D.setCardBackgroundColor(Color.argb(1.0f, 0.25f, 0.25f, 0.25f))
            card3D.setCardBackgroundColor(Color.rgb(235, 186, 145))
            graphicsSettingsButton.visibility = View.VISIBLE
            collapseCardButton.visibility = View.VISIBLE
        } else {
            card2D.setCardBackgroundColor(Color.rgb(235, 186, 145))
            card3D.setCardBackgroundColor(Color.argb(1.0f, 0.25f, 0.25f, 0.25f))
            graphicsSettingsButton.visibility = View.GONE
            collapseCardButton.visibility = View.GONE
        }
        savePreference(GRAPHICS_3D_KEY, is3D)
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

    private fun savePreference(key: String, value: Boolean) {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        with(preferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    private fun hideActivityDecorations(isFullscreen: Boolean) {
        supportActionBar?.hide()

        if (isFullscreen) {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        resetLayout()
    }

    private fun setupUIElements() {
        fullScreenCard.findViewById<CheckBox>(R.id.full_screen_checkbox).setOnCheckedChangeListener { _, isChecked ->
            savePreference(FULL_SCREEN_KEY, isChecked)
            hideActivityDecorations(isChecked)
        }

        collapseCardButton.setOnClickListener {
            if (is3D && expanded) {
                collapse()
            }
        }

        graphicsSettingsButton.setOnClickListener {
            if (is3D && !expanded) {
                expand()
            }
        }

        fovSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar == null || seekBar.id != fovSeekbar.id) {
                    return
                }

                val actualProgress = progress * FOV_SCALE + FOV_OFFSET
                glView3D.getRenderer().setFoV(actualProgress)
                glView3D.requestRender()
                savePreference(FOV_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        pieceScaleSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar == null || seekBar.id != pieceScaleSeekbar.id) {
                    return
                }

                val actualProgress = progress.toFloat() / 100.0f
                glView3D.getRenderer().setPieceScale(actualProgress)
                glView3D.requestRender()
                savePreference(PIECE_SCALE_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun getDisplayWidth(): Int {
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize.x
    }

    companion object {
        internal const val GRAPHICS_3D_KEY = "3D_graphics_enabled"
        internal const val CAMERA_ROTATION_KEY = "camera_rotation"
        internal const val CAMERA_ZOOM_KEY = "camera_zoom"
        internal const val FOV_KEY = "camera_fov"
        internal const val PIECE_SCALE_KEY = "piece_scale"
        internal const val FULL_SCREEN_KEY = "full_screen_enabled"

        private const val FOV_OFFSET = 10
        private const val FOV_SCALE = 5
        
        private const val ANIMATION_DURATION = 250L
    }
}