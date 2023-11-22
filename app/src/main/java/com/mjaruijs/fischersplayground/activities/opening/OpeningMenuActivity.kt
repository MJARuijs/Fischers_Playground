package com.mjaruijs.fischersplayground.activities.opening

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningAdapter
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.CreateOpeningDialog
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

class OpeningMenuActivity : AppCompatActivity() {

    var activityName = "opening_menu_activity"

    private lateinit var openingAdapter: OpeningAdapter
    private lateinit var dataManager: DataManager

    private val createOpeningDialog = CreateOpeningDialog(::onTeamSelected)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opening_menu)

        createOpeningDialog.create(this)
        dataManager = DataManager.getInstance(this)
        initUIComponents()
    }

    override fun onResume() {
        super.onResume()
        hideActivityDecorations()
        restoreSavedOpenings(dataManager.getSavedOpenings())
    }

    override fun onDestroy() {
        super.onDestroy()
        createOpeningDialog.dismiss()
    }

    private fun onTeamSelected(openingName: String, team: Team) {
//        val intent = Intent(this, CreateOpeningActivity::class.java)
//            .putExtra("opening_team", team.toString())
//            .putExtra("opening_name", openingName)

        createOpeningDialog.dismiss()

        val intent = Intent(this, VariationMenuActivity::class.java)
            .putExtra("opening_team", team.toString())
            .putExtra("opening_name", openingName)

        startActivity(intent)
    }

    private fun restoreSavedOpenings(openings: ArrayList<Opening>) {
        for (opening in openings) {
            if (!openingAdapter.contains(opening)) {
                openingAdapter += opening
                Logger.debug(activityName, "Added opening to recycler: ${opening.name}")
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
        val intent = Intent(this, VariationMenuActivity::class.java)
            .putExtra("opening_team", openingTeam.toString())
            .putExtra("opening_name", openingName)

        createOpeningDialog.dismiss()

        startActivity(intent)
    }

    private fun onDeleteOpening(opening: Opening) {
        dataManager.deleteOpening(opening.name, opening.team, applicationContext)
        openingAdapter.deleteOpening(opening)
    }

    private fun initUIComponents() {
        openingAdapter = OpeningAdapter(::onOpeningClicked, ::onDeleteOpening)

        val openingRecyclerView = findViewById<RecyclerView>(R.id.opening_list)
        openingRecyclerView.layoutManager = LinearLayoutManager(this)
        openingRecyclerView.adapter = openingAdapter

        findViewById<UIButton2>(R.id.create_new_opening_button)
            .setText("Create New Opening")
            .setColorResource(R.color.accent_color)
            .setTextSize(28f)
            .setCornerRadius(45.0f)
            .setOnClickListener {
                createOpeningDialog.show()
            }
    }
}