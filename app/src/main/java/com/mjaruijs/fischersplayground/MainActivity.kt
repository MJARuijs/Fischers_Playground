package com.mjaruijs.fischersplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mjaruijs.fischersplayground.opengl.SurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var glView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glView = SurfaceView(this)
        setContentView(glView)

        hideActivityDecorations()
    }

    private fun hideActivityDecorations() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

//        supportActionBar?.hide()
    }

    override fun onDestroy() {
        glView.destroy()
        super.onDestroy()
    }
}