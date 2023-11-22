package com.mjaruijs.fischersplayground.activities

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.opening.OpeningMenuActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.services.LoadResourcesWorker
import com.mjaruijs.fischersplayground.userinterface.RippleEffect
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.FileManager

class MainActivity : AppCompatActivity() {

    private var activityName = "main_activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FileManager.init(applicationContext)

        loadResources()

        initUIComponents()
    }

    override fun onResume() {
        super.onResume()
        hideActivityDecorations()
    }

    private fun hideActivityDecorations() {
        val preferences = getSharedPreferences(SettingsActivity.GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)
        val isFullscreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

        supportActionBar?.hide()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isFullscreen) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun initUIComponents() {
        val backgroundImage = findViewById<ImageView>(R.id.background_image)

        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.chess_background, null)!!
        drawable.colorFilter = BlendModeColorFilter(Color.rgb(0.5f, 0.4f, 0.35f), BlendMode.SOFT_LIGHT)
        backgroundImage.setImageDrawable(drawable)
        drawable.clearColorFilter()

        findViewById<TextView>(R.id.welcome_text_view)
            .setOnClickListener { textView ->
//                createUsernameDialog.show {
//                    (textView as TextView).text = "Welcome, $it"
//                    getPreference(USER_PREFERENCE_FILE).edit().putString(USER_NAME_KEY, it).apply()
//                    networkManager.sendMessage(NetworkMessage(Topic.CHANGE_USER_NAME, "$userId|$it"))
//                }
            }

        findViewById<UIButton2>(R.id.settings_button)
            .setIconScale(0.65f)
            .setIcon(R.drawable.settings_solid_icon)
            .setRippleEffect(RippleEffect.OVAL)
            .setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

//        val settingsButton = findViewById<ImageView>(R.id.settings_button)
//        settingsButton.setBackgroundResource(R.drawable.settings_solid_icon)
//        settingsButton.setOnClickListener {
//            stayingInApp = true
//            val intent = Intent(this, SettingsActivity::class.java)
//            startActivity(intent)
//        }


        findViewById<UIButton2>(R.id.practice_button)
            .setText("Practice Mode")
            .setColorResource(R.color.accent_color)
            .setCornerRadius(45.0f)
            .setTextSize(28f)
            .setOnClickListener {
                startActivity(Intent(this, OpeningMenuActivity::class.java))
            }

    }

    private fun loadResources() {
        val textures = arrayOf(
            R.drawable.wood_diffuse_texture,
            R.drawable.white_pawn,
            R.drawable.white_knight,
            R.drawable.white_bishop,
            R.drawable.white_rook,
            R.drawable.white_queen,
            R.drawable.white_king,
            R.drawable.black_pawn,
            R.drawable.black_knight,
            R.drawable.black_bishop,
            R.drawable.black_rook,
            R.drawable.black_queen,
            R.drawable.black_king,
            R.drawable.diffuse_map_pawn,
            R.drawable.diffuse_map_knight,
            R.drawable.diffuse_map_bishop,
            R.drawable.diffuse_map_rook,
            R.drawable.diffuse_map_queen,
            R.drawable.diffuse_map_king,
            R.drawable.king_checked,
        )

        val models = arrayOf(
            R.raw.pawn_bytes,
            R.raw.knight_bytes,
            R.raw.bishop_bytes,
            R.raw.rook_bytes,
            R.raw.queen_bytes,
            R.raw.king_bytes
        )
//
//        val textureLoader = TextureLoader.getInstance()
//
//        for (textureId in textures) {
//            textureLoader.load(applicationContext.resources, textureId)
//        }
//
//        for (modelId in models) {
//            OBJLoader.preload(applicationContext.resources, modelId)
//        }
        val worker = OneTimeWorkRequestBuilder<LoadResourcesWorker>()
            .setInputData(
                workDataOf(
                    Pair("texture_resources", textures),
                    Pair("model_resources", models)
                )
            ).build()

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(worker)
    }

}