package com.mjaruijs.fischersplayground.activities.game

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.ClientActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningAdapter
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.CreateOpeningDialog
import com.mjaruijs.fischersplayground.userinterface.UIButton

class OpeningMenuActivity : ClientActivity() {

    private lateinit var openingAdapter: OpeningAdapter

    private val createOpeningDialog = CreateOpeningDialog(::onTeamSelected)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practise_setup)

        createOpeningDialog.create(this)

        initUIComponents()
    }

    override fun onResume() {
        super.onResume()
        hideActivityDecorations()

        Thread {
            while (dataManager.isLocked()) {
                Thread.sleep(1)
            }
            runOnUiThread {
                restoreSavedOpenings(dataManager.getSavedOpenings())
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        createOpeningDialog.dismiss()
    }

    private fun onTeamSelected(openingName: String, team: Team) {
        val intent = Intent(this, CreateOpeningActivity::class.java)
            .putExtra("opening_team", team.toString())
            .putExtra("opening_name", openingName)

        startActivity(intent)
    }

    private fun restoreSavedOpenings(openings: ArrayList<Opening>) {
        for (opening in openings) {
            if (!openingAdapter.contains(opening)) {
                openingAdapter += opening
            }
        }
    }

    private fun hideActivityDecorations() {
        val preferences = getSharedPreferences(SettingsActivity.GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)
        val isFullscreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

        supportActionBar?.hide()

        if (isFullscreen) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun onOpeningClicked(openingName: String, openingTeam: Team) {
        val intent = Intent(this, CreateOpeningActivity::class.java)
            .putExtra("opening_team", openingTeam.toString())
            .putExtra("opening_name", openingName)

        startActivity(intent)
    }

    private fun initUIComponents() {
        openingAdapter = OpeningAdapter(::onOpeningClicked)

        val openingRecyclerView = findViewById<RecyclerView>(R.id.opening_list)
        openingRecyclerView.layoutManager = LinearLayoutManager(this)
        openingRecyclerView.adapter = openingAdapter

        findViewById<UIButton>(R.id.create_new_opening_button)
            .setText("Create New Opening")
            .setButtonTextSize(70.0f)
            .setColor(235, 186, 145)
            .setCornerRadius(45.0f)
            .setChangeTextColorOnHover(false)
            .setOnClick {
                stayingInApp = true
                createOpeningDialog.show()
            }
    }
}