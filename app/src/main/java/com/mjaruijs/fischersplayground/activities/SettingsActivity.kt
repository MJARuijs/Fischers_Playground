package com.mjaruijs.fischersplayground.activities

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.GameSettingsSurface
import kotlin.math.roundToInt

@Suppress("SameParameterValue", "ControlFlowWithEmptyBody")
class SettingsActivity : AppCompatActivity() {

    private lateinit var game: SinglePlayerGame
    private lateinit var fovSeekbar: SeekBar
    private lateinit var pieceScaleSeekbar: SeekBar

    private lateinit var glView3D: GameSettingsSurface

    private lateinit var card2D: CardView
    private lateinit var card3D: CardView
    private lateinit var innerCard3D: CardView
    private lateinit var fullScreenCard: CardView

    private lateinit var collapseCardButton: ImageView
    private lateinit var graphicsSettingsButton: ImageView
    private lateinit var settingsLayout: ConstraintLayout
    private lateinit var graphicsSettingsLayout: ConstraintLayout

    private val defaultGraphicsConstraints = ConstraintSet()
    private val expandedGraphicsConstraints = ConstraintSet()

    private var is3D = false
    private var expanded = false

    private var initialized = false

    private var initialX = 0f
    private var initialY = 0f

    private var padding = 0f
    private var scaledPadding = 0f
    private var collapsedViewWidth = 0f
    private var viewXRatio = 1f
    private var fullScreenToggleY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_expanded)
        PieceTextures.init(this)

        hideActivityDecorations()

        settingsLayout = findViewById(R.id.settings_layout)
        graphicsSettingsLayout = findViewById(R.id.graphics_3d_layout)
        graphicsSettingsButton = findViewById(R.id.graphics_settings_button)

        collapseCardButton = findViewById(R.id.collapse_card_button)
        card2D = findViewById(R.id.graphics_2d_card)
        card3D = findViewById(R.id.graphics_3d_card)
        innerCard3D = findViewById(R.id.inner_card_3D)
        fullScreenCard = findViewById(R.id.full_screen_card)

        fovSeekbar = findViewById(R.id.fov_seekbar)
        pieceScaleSeekbar = findViewById(R.id.piece_scale_seekbar)

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

        padding = dpToPx(8f)
        val totalPadding = padding * 3
        val displayWidth = getDisplayWidth().toFloat()
        val remainingViewWidth = displayWidth - totalPadding

        collapsedViewWidth = remainingViewWidth / 2f
        viewXRatio = collapsedViewWidth / displayWidth
        scaledPadding = padding / displayWidth

        fullScreenToggleY = fullScreenCard.y

        window.decorView.post {
            collapse2DView()
            collapse3DView()
        }

        restore3DPreference()
    }

    override fun onBackPressed() {
        if (expanded) {
            collapse()
        } else {
            super.onBackPressed()
        }
    }

    private fun collapse2DView() {
        card2D.scaleX = viewXRatio
        card2D.scaleY = viewXRatio
        card2D.x = -card2D.width / 4f + padding / 4
        card2D.y = -card2D.width / 4f + padding / 4
    }

    private fun collapse3DView() {
        val transition = ChangeBounds()
        transition.duration = 0

        defaultGraphicsConstraints.applyTo(graphicsSettingsLayout)
        TransitionManager.beginDelayedTransition(graphicsSettingsLayout, transition)

        graphicsSettingsButton.scaleX = 2f
        graphicsSettingsButton.scaleY = 2f

        card3D.scaleX = viewXRatio
        card3D.scaleY = viewXRatio
        card3D.x = card3D.width / 4f - padding / 4
        card3D.y = -card3D.width / 4f + padding / 4
    }

    private fun expand() {
        collapseCardButton.alpha = 0.0f

        val cardScaleXAnimator = ObjectAnimator.ofFloat(card3D, "scaleX", viewXRatio * 2.0f + scaledPadding)
        val cardScaleYAnimator = ObjectAnimator.ofFloat(card3D, "scaleY", viewXRatio * 2.0f + scaledPadding)
        val cardXAnimator = ObjectAnimator.ofFloat(card3D, "x", 0f)
        val cardYAnimator = ObjectAnimator.ofFloat(card3D, "y", 0f)
        val radiusAnimator = ObjectAnimator.ofFloat(card3D, "radius", card3D.radius * viewXRatio)
        val innerRadiusAnimator = ObjectAnimator.ofFloat(innerCard3D, "radius", innerCard3D.radius * viewXRatio)
//        val buttonXScaleAnimator = ObjectAnimator.ofFloat(graphicsSettingsButton, "scaleX", 2.0f)
//        val buttonYScaleAnimator = ObjectAnimator.ofFloat(graphicsSettingsButton, "scaleY", 2.0f)
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(collapseCardButton, "alpha", 1.0f)
        val settingsButtonAlphaAnimator = ObjectAnimator.ofFloat(graphicsSettingsButton, "alpha", 0.0f)

        cardScaleXAnimator.duration = ANIMATION_DURATION
        cardScaleYAnimator.duration = ANIMATION_DURATION
        cardXAnimator.duration = ANIMATION_DURATION
        cardYAnimator.duration = ANIMATION_DURATION
        radiusAnimator.duration = ANIMATION_DURATION
        innerRadiusAnimator.duration = ANIMATION_DURATION
//        buttonXScaleAnimator.duration = ANIMATION_DURATION
//        buttonYScaleAnimator.duration = ANIMATION_DURATION
        closeButtonAlphaAnimator.duration = ANIMATION_DURATION
        settingsButtonAlphaAnimator.duration = ANIMATION_DURATION

        cardScaleXAnimator.start()
        cardScaleYAnimator.start()
        cardXAnimator.start()
        cardYAnimator.start()
        radiusAnimator.start()
        innerRadiusAnimator.start()
//        buttonXScaleAnimator.start()
//        buttonYScaleAnimator.start()
        closeButtonAlphaAnimator.start()
        settingsButtonAlphaAnimator.start()

        expandedGraphicsConstraints.applyTo(graphicsSettingsLayout)

        glView3D.isActive = true
        collapseCardButton.visibility = View.VISIBLE

        expanded = true
    }

    private fun collapse() {
        val cardScaleXAnimator = ObjectAnimator.ofFloat(card3D, "scaleX", viewXRatio)
        val cardScaleYAnimator = ObjectAnimator.ofFloat(card3D, "scaleY", viewXRatio)
        val cardXAnimator = ObjectAnimator.ofFloat(card3D, "x", initialX)
        val cardYAnimator = ObjectAnimator.ofFloat(card3D, "y", initialY)
        val radiusAnimator = ObjectAnimator.ofFloat(card3D, "radius", card3D.radius / viewXRatio)
        val innerRadiusAnimator = ObjectAnimator.ofFloat(innerCard3D, "radius", innerCard3D.radius / viewXRatio)
        val buttonXScaleAnimator = ObjectAnimator.ofFloat(graphicsSettingsButton, "scaleX", 2.0f)
        val buttonYScaleAnimator = ObjectAnimator.ofFloat(graphicsSettingsButton, "scaleY", 2.0f)
        val closeButtonAlphaAnimator = ObjectAnimator.ofFloat(collapseCardButton, "alpha", 0.0f)
        val settingsButtonAlphaAnimator = ObjectAnimator.ofFloat(graphicsSettingsButton, "alpha", 1.0f)

        cardScaleXAnimator.duration = ANIMATION_DURATION
        cardScaleYAnimator.duration = ANIMATION_DURATION
        cardXAnimator.duration = ANIMATION_DURATION
        cardYAnimator.duration = ANIMATION_DURATION
        radiusAnimator.duration = ANIMATION_DURATION
        innerRadiusAnimator.duration = ANIMATION_DURATION
        buttonXScaleAnimator.duration = ANIMATION_DURATION
        buttonYScaleAnimator.duration = ANIMATION_DURATION
        closeButtonAlphaAnimator.duration = ANIMATION_DURATION
        settingsButtonAlphaAnimator.duration = ANIMATION_DURATION

        cardScaleXAnimator.start()
        cardScaleYAnimator.start()
        cardXAnimator.start()
        cardYAnimator.start()
        radiusAnimator.start()
        innerRadiusAnimator.start()
        buttonXScaleAnimator.start()
        buttonYScaleAnimator.start()
        closeButtonAlphaAnimator.start()
        settingsButtonAlphaAnimator.start()

        val transition = ChangeBounds()
        transition.duration = ANIMATION_DURATION

        defaultGraphicsConstraints.applyTo(graphicsSettingsLayout)
        TransitionManager.beginDelayedTransition(graphicsSettingsLayout, transition)

        glView3D.isActive = false
//        collapseCardButton.visibility = View.INVISIBLE
//        graphicsSettingsButton.visibility = View.VISIBLE

        expanded = false
    }

    private fun onContextCreated() {
        runOnUiThread {
            game = SinglePlayerGame()
            glView3D.setGame(game)
            restorePreferences()
        }
    }

    private fun onCameraRotated() {
        savePreference(CAMERA_ROTATION_KEY, glView3D.getRenderer().getCameraRotation())
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
        } else {
            card2D.setCardBackgroundColor(Color.rgb(235, 186, 145))
            card3D.setCardBackgroundColor(Color.argb(1.0f, 0.25f, 0.25f, 0.25f))
            graphicsSettingsButton.visibility = View.GONE
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

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
//        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        supportActionBar?.hide()
    }

    private fun setupUIElements() {
        collapseCardButton.setOnClickListener {
            collapse()
        }

        graphicsSettingsButton.setOnClickListener {
            if (!initialized) {
                initialX = card3D.x
                initialY = card3D.y

                initialized = true
            }

            if (expanded) collapse() else expand()
        }

        fovSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar == null || seekBar.id != fovSeekbar.id) {
                    return
                }
                println("FOV: ${seekBar.id} ${fromUser}")

                val actualProgress = progress * FOV_SCALE + FOV_OFFSET
                glView3D.getRenderer().setFoV(actualProgress)
                glView3D.requestRender()
                savePreference(FOV_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                println("FOV: START TRACKING ${seekBar?.id}")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        pieceScaleSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (seekBar == null || seekBar.id != pieceScaleSeekbar.id) {
                    return
                }
                println("PIECESCALE: ${seekBar.id} ${fromUser}")

                val actualProgress = progress.toFloat() / 100.0f
                glView3D.getRenderer().setPieceScale(actualProgress)
                glView3D.requestRender()
                savePreference(PIECE_SCALE_KEY, actualProgress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                println("SCALE:  START TRACKING ${seekBar?.id}")

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

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    companion object {
        internal const val GRAPHICS_3D_KEY = "3D_graphics_enabled"
        internal const val CAMERA_ROTATION_KEY = "camera_rotation"
        internal const val CAMERA_ZOOM_KEY = "camera_zoom"
        internal const val FOV_KEY = "camera_fov"
        internal const val PIECE_SCALE_KEY = "piece_scale"

        private const val FOV_OFFSET = 10
        private const val FOV_SCALE = 5
        
        private const val ANIMATION_DURATION = 500L
    }
}