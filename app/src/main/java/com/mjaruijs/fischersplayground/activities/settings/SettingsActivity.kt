package com.mjaruijs.fischersplayground.activities.settings

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.GameSettingsSurface
import com.mjaruijs.fischersplayground.util.FileManager
import com.mjaruijs.fischersplayground.util.Time
import kotlin.math.roundToInt

@Suppress("SameParameterValue")
class SettingsActivity : ClientActivity() {

    private lateinit var game: SinglePlayerGame

    private lateinit var glView3D: GameSettingsSurface

    private lateinit var card2D: CardView
    private lateinit var card3D: CardView
    private lateinit var innerCard3D: CardView

    private lateinit var pieceSizeText: TextView
    private lateinit var pieceScaleSeekbar: SeekBar
    private lateinit var fovText: TextView
    private lateinit var fovSeekbar: SeekBar

    private lateinit var collapseCardButton: ImageView
    private lateinit var graphicsSettingsButton: ImageView
    private lateinit var settingsLayout: ConstraintLayout
    private lateinit var graphics3DLayout: ConstraintLayout
    private lateinit var graphicsControlsLayout: ConstraintLayout

    private lateinit var settings: SettingsCollection

    private val defaultGraphicsConstraints = ConstraintSet()
    private val expandedGraphicsConstraints = ConstraintSet()

    private val defaultSettingsConstraints = ConstraintSet()
    private val expandedSettingsConstraints = ConstraintSet()

    private var is3D = false
    private var expanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsLayout = findViewById(R.id.settings_layout)
        graphics3DLayout = findViewById(R.id.graphics_3d_layout)
        graphicsControlsLayout = findViewById(R.id.graphics_controls_layout)
        graphicsSettingsButton = findViewById(R.id.graphics_settings_button)

        collapseCardButton = findViewById(R.id.collapse_card_button)
        card2D = findViewById(R.id.graphics_2d_card)
        card3D = findViewById(R.id.graphics_3d_card)
        innerCard3D = findViewById(R.id.inner_card_3D)
//        fullScreenCard = findViewById(R.id.full_screen_card)
//        confirmMoveCard = findViewById(R.id.confirm_move_card)

        pieceSizeText = findViewById(R.id.piece_scale_text)
        pieceScaleSeekbar = findViewById(R.id.piece_scale_seekbar)
        fovText = findViewById(R.id.fov_text)
        fovSeekbar = findViewById(R.id.fov_seekbar)
//        fullScreenCheckbox = findViewById(R.id.full_screen_checkbox)
//        confirmMoveCheckbox = findViewById(R.id.confirm_move_checkbox)

        settings = SettingsCollection(settingsLayout, card3D.id)

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
        glView3D.init(::runOnUIThread, ::onContextCreated, ::onCameraRotated, ::saveGraphicsPreference, ::onExceptionThrown)
        glView3D.getRenderer().set3D(true)

        defaultGraphicsConstraints.clone(this, R.layout.graphics_settings)
        expandedGraphicsConstraints.clone(this, R.layout.graphics_settings_expanded)

        defaultSettingsConstraints.clone(this, R.layout.activity_settings)
        expandedSettingsConstraints.clone(this, R.layout.activity_settings_expanded)

//        restoreGamePreferences()
//        restoreFullscreenPreference()
        restore3DPreference()

        initSettings()

        findViewById<Button>(R.id.delete_user_data_button).setOnClickListener {
            val preferenceFiles = arrayListOf(
                "fcm_token",
                "user_data",
                "fire_base",
                "FirebaseAppHeartBeat",
                "com.google.android.gms.appid"
            )

            for (file in preferenceFiles) {
                getSharedPreferences(file, MODE_PRIVATE).edit().clear().commit()
                FileManager.delete("../shared_prefs/$file.xml")
            }

            val dataFiles = arrayListOf(
                "mp_games",
                "received_invites",
                "recent_opponents"
            )

            for (file in dataFiles) {
                FileManager.delete("$file.txt")
            }

        }
    }

    private fun initSettings() {
        settings += Setting(applicationContext, "Fullscreen", GRAPHICS_PREFERENCES_KEY, FULL_SCREEN_KEY, ::toggleActivityDecorations)
        settings += Setting(applicationContext, "Confirm moves", GAME_PREFERENCES_KEY, CONFIRM_MOVES_KEY)
        settings += Setting(applicationContext, "Vibrate on click", GAME_PREFERENCES_KEY, VIBRATE_KEY)
        settings += Setting(applicationContext, "Show finished games", GAME_PREFERENCES_KEY, SHOW_FINISHED_GAMES_KEY)
    }

    override fun onBackPressed() {
        if (expanded) {
            collapse()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        glView3D.destroy()
        super.onDestroy()
    }

//    private fun runOnUIThread(runnable: () -> Unit) {
//        runOnUiThread {
//            runnable()
//        }
//    }

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
        transition.doOnEnd {
            graphicsSettingsButton.visibility = View.GONE
            glView3D.isActive = true
            expanded = true
        }

        expandedGraphicsConstraints.applyTo(graphics3DLayout)
        expandedSettingsConstraints.applyTo(settingsLayout)
        TransitionManager.beginDelayedTransition(settingsLayout, transition)

        for (animator in animators) {
            animator.duration = ANIMATION_DURATION
            animator.start()
        }
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
        transition.doOnEnd {
            graphicsSettingsButton.visibility = View.VISIBLE
            glView3D.isActive = false
            expanded = false
        }

        defaultGraphicsConstraints.applyTo(graphics3DLayout)
        defaultSettingsConstraints.applyTo(settingsLayout)
        TransitionManager.beginDelayedTransition(settingsLayout, transition)

        for (animator in animators) {
            animator.duration = ANIMATION_DURATION
            animator.start()
        }
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
            game = SinglePlayerGame(true, Time.getFullTimeStamp(), true)
            glView3D.setGame(game)
            restorePreferences()
        }
    }

    private fun onCameraRotated() {
        saveGraphicsPreference(CAMERA_ROTATION_KEY, glView3D.getRenderer().getCameraRotation())
    }

    private fun restore3DPreference() {
        val preferences = getSharedPreferences(GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)

        is3D = preferences.getBoolean(GRAPHICS_3D_KEY, false)
        selectGraphicsType()
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences(GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)

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
            card3D.setCardBackgroundColor(resources.getColor(R.color.accent_color, null))
            graphicsSettingsButton.visibility = View.VISIBLE
            collapseCardButton.visibility = View.VISIBLE
        } else {
            card2D.setCardBackgroundColor(resources.getColor(R.color.accent_color, null))
            card3D.setCardBackgroundColor(Color.argb(1.0f, 0.25f, 0.25f, 0.25f))
            graphicsSettingsButton.visibility = View.GONE
            collapseCardButton.visibility = View.GONE
        }
        saveGraphicsPreference(GRAPHICS_3D_KEY, is3D)
    }

    private fun toggleActivityDecorations(isFullscreen: Boolean) {
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
        collapseCardButton.setOnClickListener {
            if (is3D && expanded) {
                collapse()
            }
        }

        graphicsSettingsButton.setOnClickListener {
            if (is3D && !expanded) {
                expand()
            } else if (expanded) {
                glView3D.getRenderer().renderCircle = !glView3D.getRenderer().renderCircle
                glView3D.requestRender()
            }
        }

        fovSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar == null || seekBar.id != fovSeekbar.id) {
                    return
                }

                if (!expanded) {
                    return
                }

                val actualProgress = progress * FOV_SCALE + FOV_OFFSET
                glView3D.getRenderer().setFoV(actualProgress)
                glView3D.requestRender()
                saveGraphicsPreference(FOV_KEY, actualProgress)
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

                if (!expanded) {
                    return
                }

                val actualProgress = progress.toFloat() / 100.0f
                glView3D.getRenderer().setPieceScale(actualProgress)
                glView3D.requestRender()
                saveGraphicsPreference(PIECE_SCALE_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }

    private fun onExceptionThrown(fileName: String, e: Exception) {

    }

    private fun getDisplayWidth(): Int {
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize.x
    }

    private fun saveGraphicsPreference(key: String, value: Vector3) {
        savePreference(GRAPHICS_PREFERENCES_KEY, key, value)
    }

    private fun saveGraphicsPreference(key: String, value: Int) {
        savePreference(GRAPHICS_PREFERENCES_KEY, key, value)
    }

    private fun saveGraphicsPreference(key: String, value: Float) {
        savePreference(GRAPHICS_PREFERENCES_KEY, key, value)
    }

    private fun saveGraphicsPreference(key: String, value: Boolean) {
        savePreference(GRAPHICS_PREFERENCES_KEY, key, value)
    }

    private fun savePreference(preferenceFile: String, key: String, value: Vector3) {
        val preferences = getSharedPreferences(preferenceFile, MODE_PRIVATE)

        with(preferences.edit()) {
            putString(key, value.toString())
            apply()
        }
    }

    private fun savePreference(preferenceFile: String, key: String, value: Int) {
        val preferences = getSharedPreferences(preferenceFile, MODE_PRIVATE)

        with(preferences.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun savePreference(preferenceFile: String, key: String, value: Float) {
        val preferences = getSharedPreferences(preferenceFile, MODE_PRIVATE)

        with(preferences.edit()) {
            putFloat(key, value)
            apply()
        }
    }

    private fun savePreference(preferenceFile: String, key: String, value: Boolean) {
        val preferences = getSharedPreferences(preferenceFile, MODE_PRIVATE)

        with(preferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    companion object {
        const val GRAPHICS_PREFERENCES_KEY = "graphics_preferences"
        const val GAME_PREFERENCES_KEY = "game_preferences"
        const val PRACTICE_PREFERENCES_KEY = "practice_preferences"

        const val GRAPHICS_3D_KEY = "3D_graphics_enabled"
        const val CAMERA_ROTATION_KEY = "camera_rotation"
        const val CAMERA_ZOOM_KEY = "camera_zoom"
        const val FOV_KEY = "camera_fov"
        const val PIECE_SCALE_KEY = "piece_scale"
        const val FULL_SCREEN_KEY = "full_screen_enabled"
        const val CONFIRM_MOVES_KEY = "confirm_moves"
        const val VIBRATE_KEY = "vibrate_on_click"
        const val SHOW_FINISHED_GAMES_KEY = "show_finished_games"

        private const val FOV_OFFSET = 10
        private const val FOV_SCALE = 5
        
        private const val ANIMATION_DURATION = 250L
    }
}