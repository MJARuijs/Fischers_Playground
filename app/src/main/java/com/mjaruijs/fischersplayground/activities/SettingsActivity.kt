package com.mjaruijs.fischersplayground.activities

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.marginTop
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.PieceTextures
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.GameSettingsSurface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@Suppress("SameParameterValue", "ControlFlowWithEmptyBody")
class SettingsActivity : AppCompatActivity() {

    private lateinit var game: SinglePlayerGame
    private lateinit var fovSeekbar: SeekBar
    private lateinit var pieceScaleSeekbar: SeekBar

    private lateinit var glView3D: GameSettingsSurface
//    private lateinit var previewImage3D: ImageView

    private lateinit var card2D: CardView
    private lateinit var card3D: CardView
    private lateinit var innerCard3D: CardView

    private lateinit var graphicsSettingsButton: ImageView
    private lateinit var settingsLayout: ConstraintLayout
    private lateinit var graphicsSettingsLayout: ConstraintLayout

    private val defaultSettingsConstraints = ConstraintSet()
    private val expandedSettingsConstraints = ConstraintSet()

    private val defaultGraphicsConstraints = ConstraintSet()
    private val expandedGraphicsConstraints = ConstraintSet()

    private var is3D = false
    private var expanded = false

    private var locked = AtomicBoolean(false)

    private var initialized = false

    private var initialX = 0f
    private var initialY = 0f

    private var maximumCardHeight = 0

    private var padding = 0f
    private var scaledPadding = 0f
    private var collapsedViewWidth = 0f
    private var uiItemsOffset = 0f
    private var viewXRatio = 1f
    private var view3DYRatio = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_expanded)
        PieceTextures.init(this)

        hideActivityDecorations()

        settingsLayout = findViewById(R.id.settings_layout)
        graphicsSettingsLayout = findViewById(R.id.graphics_3d_layout)
        graphicsSettingsButton = findViewById(R.id.graphics_settings_button)

        card2D = findViewById(R.id.graphics_2d_card)
        card3D = findViewById(R.id.graphics_3d_card)
        innerCard3D = findViewById(R.id.inner_card_3D)

        fovSeekbar = findViewById(R.id.fov_seekbar)
        pieceScaleSeekbar = findViewById(R.id.piece_scale_seekbar)

        setupSeekbars()

        card2D.setOnClickListener {
            is3D = false
            selectGraphicsType()
        }

        card3D.setOnClickListener {
            is3D = true
            selectGraphicsType()
        }

//        previewImage3D = findViewById(R.id.graphics_3d_preview_image)

        glView3D = findViewById(R.id.preview_3d)
        glView3D.init(::onContextCreated, ::onCameraRotated, ::savePreference)
        glView3D.getRenderer().set3D(true)

        defaultSettingsConstraints.clone(this, R.layout.activity_settings)
        expandedSettingsConstraints.clone(this, R.layout.activity_settings_alt)

        defaultGraphicsConstraints.clone(this, R.layout.graphics_settings)
        expandedGraphicsConstraints.clone(this, R.layout.graphics_settings_expanded)

        padding = dpToPx(8f)
        val totalPadding = padding * 3
        val displayWidth = getDisplayWidth().toFloat()
        val remainingViewWidth = displayWidth - totalPadding

        collapsedViewWidth = remainingViewWidth / 2f
        viewXRatio = collapsedViewWidth / displayWidth
        scaledPadding = padding / displayWidth

        println("$totalPadding, $displayWidth, $remainingViewWidth, $collapsedViewWidth, $viewXRatio")

        window.decorView.post {

            maximumCardHeight = card3D.height

            val displayHeight = getDisplayHeight().toFloat()
            val expandedViewHeight = card3D.height
            view3DYRatio = expandedViewHeight / displayHeight

            uiItemsOffset = (maximumCardHeight - displayWidth) / displayWidth
            println("MAX HEIGHT: $maximumCardHeight $uiItemsOffset $view3DYRatio ${card3D.y}")

            defaultGraphicsConstraints.applyTo(graphicsSettingsLayout)
            TransitionManager.beginDelayedTransition(graphicsSettingsLayout)

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

    private fun test() {
        graphicsSettingsButton.setOnClickListener {

            if (!initialized) {
                initialX = card3D.x
                initialY = card3D.y

                initialized = true
            }

            Thread {
                if (expanded) {
                    locked.set(true)

                    glView3D.getRenderer().requestScreenPixels(::onPixelsReceived)
                    glView3D.requestRender()

                    while (locked.get()) {

                    }
                }

                showPreview()

                runOnUiThread {
                    if (expanded) collapse() else expand()

                    expanded = !expanded
                }
            }.start()
        }
    }

    private fun onDrawFirstFrame() {
        glView3D.getRenderer().onDraw = {}

        runOnUiThread {
            test()
        }
    }

    private fun showPreview() {
        runOnUiThread {
//            previewImage3D.visibility = View.VISIBLE
            glView3D.isActive = false
        }
    }

    private fun showGLView() {
        runOnUiThread {
            glView3D.isActive = true
//            previewImage3D.visibility = View.INVISIBLE
            graphicsSettingsButton.visibility = View.VISIBLE
        }
    }

    private fun onPixelsReceived(pixelData: ByteBuffer) {
        val width = glView3D.getRenderer().displayWidth
        val height = glView3D.getRenderer().displayHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(pixelData)

        runOnUiThread {
//            findViewById<ImageView>(R.id.graphics_3d_preview_image).setImageBitmap(bitmap)
            locked.set(false)
        }
    }

    private fun collapse2DView() {
//        card3D.visibility = View.INVISIBLE

//        println("CARD2D width: ${card2D.width / 2} ${card2D.x} ${card2D.x + card2D.width}")

        card2D.scaleX = viewXRatio
        card2D.scaleY = viewXRatio
//        card2D.x -= card2D.width / 4
//        card2D.y -= card2D.width / 4
//        card2D.y -= card2D.width / 4
        card2D.x = -card2D.width / 4f + padding / 4
        card2D.y = -card2D.width / 4f + padding / 4
//        card2D.y = 0f
//        println("CARD2D width: ${card2D.width * viewXRatio} ${card2D.x} [${card2D.x - card2D.width / 4}, ${card2D.x + card2D.width / 4}] {${card2D.translationX}, ${card2D.right}}")
        println("Card2D Height: ${card2D.y} ")
    }

    private fun collapse3DView() {
//        println("CARD3D width: ${card3D.width / 2} ${card3D.x} ${card3D.x + card3D.width}")
        val transition = ChangeBounds()
        transition.duration = 0
        defaultGraphicsConstraints.applyTo(graphicsSettingsLayout)
        TransitionManager.beginDelayedTransition(graphicsSettingsLayout, transition)

        card3D.scaleX = viewXRatio
        card3D.scaleY = viewXRatio
//        graphicsSettingsButton.scaleX = 2f
//        graphicsSettingsButton.scaleY = 2f

        card3D.x = card3D.width / 4f - padding / 4
        card3D.y = -card3D.width / 4f + padding / 4

//        println("CARD3D width: ${card3D.width / 2} ${card3D.x}  [${card3D.x - card3D.width / 4}, ${card3D.x + card3D.width / 4}] {${card3D.translationX}, ${card3D.right}}")

    }

    private fun expand() {
        println("EXPANDING")
        val currentWidth = card3D.width
        val currentHeight = card3D.height

//        println(currentHeight)

        val transition = ChangeBounds()
        transition.duration = 250L


//        card3D.scaleX = viewXRatio * 2.0f + scaledPadding
//        card3D.scaleY = viewXRatio * 2.0f + scaledPadding
//
//        card3D.x = 0f
//        card3D.y = 0.0f

//        val marge = findViewById<CardView>(R.id.inner_card_3D).paddingTop
//        println("RADIUS: ${card3D.radius} ${card2D.radius} ${inn}")

//        findViewById<CardView>(R.id.inner_card_3D).setContentPadding(marge, marge, marge, marge)
//        card3D.radius *= viewXRatio
        val cardScaleXAnimator = ObjectAnimator.ofFloat(card3D, "scaleX", viewXRatio * 2.0f + scaledPadding)
        val cardScaleYAnimator = ObjectAnimator.ofFloat(card3D, "scaleY", viewXRatio * 2.0f + scaledPadding)
        val cardXAnimator = ObjectAnimator.ofFloat(card3D, "x", 0f)
        val cardYAnimator = ObjectAnimator.ofFloat(card3D, "y", 0f)

//        val cardYAnimator = ObjectAnimator.ofFloat(card3D, "y", 32f)
//        val cardYAnimator = ObjectAnimator.ofFloat(card3D, "y", (getDisplayWidth() - currentWidth) / 2f + (660 - 504) / 2)

        val radiusAnimator = ObjectAnimator.ofFloat(card3D, "radius", card3D.radius * viewXRatio)
        val innerRadiusAnimator = ObjectAnimator.ofFloat(innerCard3D, "radius", innerCard3D.radius * viewXRatio)

        cardScaleXAnimator.duration = 250L
        cardScaleYAnimator.duration = 250L
        cardXAnimator.duration = 250L
        cardYAnimator.duration = 250L
        radiusAnimator.duration = 250L
        innerRadiusAnimator.duration = 250L

        cardScaleXAnimator.start()
        cardScaleYAnimator.start()
        cardXAnimator.start()
        cardYAnimator.start()
        radiusAnimator.start()
        innerRadiusAnimator.start()

//        graphicsSettingsButton.visibility = View.GONE

        cardScaleXAnimator.doOnEnd {
            println(card3D.height)
            println(card3D.measuredHeight)

            expandedGraphicsConstraints.applyTo(graphicsSettingsLayout)
        }
//        TransitionManager.beginDelayedTransition(graphicsSettingsLayout, transition)
        showGLView()
    }

    private fun collapse() {
        println("COLLAPSING")

        val cardScaleXAnimator = ObjectAnimator.ofFloat(card3D, "scaleX", viewXRatio)
        val cardScaleYAnimator = ObjectAnimator.ofFloat(card3D, "scaleY", viewXRatio)
        val cardXAnimator = ObjectAnimator.ofFloat(card3D, "x", initialX)
        val cardYAnimator = ObjectAnimator.ofFloat(card3D, "y", initialY)
        val radiusAnimator = ObjectAnimator.ofFloat(card3D, "radius", card3D.radius / viewXRatio)
        val innerRadiusAnimator = ObjectAnimator.ofFloat(innerCard3D, "radius", innerCard3D.radius / viewXRatio)

//        val card2DScaleAnimator = ObjectAnimator.ofFloat(card2D, "scaleX", 1.0f)

        cardScaleXAnimator.duration = 250L
        cardScaleYAnimator.duration = 250L
        cardXAnimator.duration = 250L
        cardYAnimator.duration = 250L
//        card2DScaleAnimator.duration = 250L
        radiusAnimator.duration = 250L
        innerRadiusAnimator.duration = 250L

//        card3D.scaleX = viewXRatio
//        card3D.scaleY = viewXRatio
//        card3D.x = initialX
//        card3D.y = initialY

        cardScaleXAnimator.start()
        cardScaleYAnimator.start()
        cardXAnimator.start()
        cardYAnimator.start()
        radiusAnimator.start()
        innerRadiusAnimator.start()

//        card2DScaleAnimator.start()

        val transition = ChangeBounds()
        transition.duration = 250L

        defaultGraphicsConstraints.applyTo(graphicsSettingsLayout)
//        defaultSettingsConstraints.applyTo(settingsLayout)
        TransitionManager.beginDelayedTransition(graphicsSettingsLayout, transition)
//        TransitionManager.beginDelayedTransition(settingsLayout)
    }

    private fun onContextCreated() {
        runOnUiThread {
            game = SinglePlayerGame()
            glView3D.setGame(game)
            restorePreferences()

            glView3D.getRenderer().onDraw = ::onDrawFirstFrame
            glView3D.getRenderer().requestScreenPixels(::onPixelsReceived)
            glView3D.requestRender()
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

        if (cameraRotation.isNotBlank()) {
            glView3D.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
        }

        glView3D.getRenderer().setFoV(fov)
        fovSeekbar.progress = (fov - FOV_OFFSET) / FOV_SCALE

        glView3D.getRenderer().setPieceScale(pieceScale)
        pieceScaleSeekbar.progress = (pieceScale * 100).roundToInt()
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
//        card2D.setCardBackgroundColor(Color.argb(1.0f, 0.25f, 0.25f, 0.85f))
//        card3D.setCardBackgroundColor(Color.argb(1.0f, 0.85f, 0.25f, 0.25f))
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

    private fun setupSeekbars() {
        fovSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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

    private fun getDisplayHeight(): Int {
        val screenSize = Point()
        windowManager.defaultDisplay.getSize(screenSize)
        return screenSize.y
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
    }
}