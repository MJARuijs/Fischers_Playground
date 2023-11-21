package com.mjaruijs.fischersplayground.activities.opening

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.variationadapter.Variation
import com.mjaruijs.fischersplayground.adapters.variationadapter.VariationAdapter
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.CreateVariationDialog
import com.mjaruijs.fischersplayground.dialogs.PracticeSettingsDialog
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionBarFragment
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.userinterface.PopupBar
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class VariationMenuActivity : AppCompatActivity() {

    private var activityName = "variation_menu_activity"

    private lateinit var createVariationDialog: CreateVariationDialog
    private lateinit var variationAdapter: VariationAdapter
    private lateinit var popupBar: PopupBar
    private lateinit var practiceSettingsDialog: PracticeSettingsDialog

    private lateinit var addVariationButton: UIButton2

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var opening: Opening

    private val selectedVariations = ArrayList<String>()

    private lateinit var variationsLayout: ConstraintLayout

    private lateinit var dataManager: DataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_variation_menu)

        dataManager = DataManager.getInstance(this)

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"
        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create $activityName. Missing essential information: opening_team.."))

        opening = dataManager.getOpening(openingName, openingTeam)
        practiceSettingsDialog = PracticeSettingsDialog(::onStartPracticing)
        practiceSettingsDialog.create(this as Activity)

        initComponents()

        createVariationDialog = CreateVariationDialog(::onVariationCreated)
        createVariationDialog.create(this)

        supportActionBar?.show()
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar_view)
        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.text = openingName
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ActionBarFragment.BACKGROUND_COLOR))
    }

    override fun onResume() {
        super.onResume()
        hideActivityDecorations()

        variationAdapter.notifyDataSetChanged()

        Thread {
            while (dataManager.isLocked()) {
                Thread.sleep(1)
            }

            runOnUiThread {
                opening = dataManager.getOpening(openingName, openingTeam)

                for (variation in opening.variations) {
                    if (!variationAdapter.contains(variation)) {
                        variationAdapter += variation
                    }
                }
            }
        }.start()

        val hasSession = dataManager.getPracticeSession(openingName, openingTeam) != null
        if (hasSession) {
            Thread {
                Thread.sleep(250)
                runOnUiThread {
                    popupBar.show()
                }
            }.start()
        }
    }

    private fun onVariationCreated(variationName: String) {
        opening.addVariation(Variation(variationName))
        dataManager.setOpening(openingName, openingTeam, opening, applicationContext)

        createVariationDialog.dismiss()

        val intent = Intent(this, CreateOpeningActivity::class.java)
            .putExtra("opening_team", openingTeam.toString())
            .putExtra("opening_name", openingName)
            .putExtra("variation_name", variationName)

        startActivity(intent)
    }

    private fun onVariationClicked(variationName: String) {
        val intent = Intent(this, CreateOpeningActivity::class.java)
            .putExtra("opening_team", openingTeam.toString())
            .putExtra("opening_name", openingName)
            .putExtra("variation_name", variationName)

        startActivity(intent)
    }

    private fun onVariationSelected(variationName: String, selected: Boolean) {
        if (selected) {
            if (selectedVariations.isEmpty()) {
                showPracticeButton()
            }
            selectedVariations += variationName
        } else {
            selectedVariations.remove(variationName)
            if (selectedVariations.isEmpty()) {
                hidePracticeButton()
            }
        }
    }

    private fun showPracticeButton() {
        addVariationButton
            .setIcon(R.drawable.next_arrow_icon)
            .setOnClickListener {
                practiceSettingsDialog.show(applicationContext)
            }
    }

    private fun hidePracticeButton() {
        addVariationButton
            .setIcon(R.drawable.add_icon)
            .setOnClickListener {
                createVariationDialog.show()
            }
    }

    private fun onStartPracticing(useShuffle: Boolean, practiceArrows: Boolean) {
        practiceSettingsDialog.dismiss()

        val intent = Intent(this, PracticeActivity::class.java)
        intent.putExtra("opening_name", openingName)
        intent.putExtra("opening_team", openingTeam.toString())
        intent.putExtra("resume_session", false)
        intent.putExtra("use_shuffle", useShuffle)
        intent.putExtra("practice_arrows", practiceArrows)
        intent.putStringArrayListExtra("variation_name", selectedVariations)

        startActivity(intent)
    }

    private fun hideActivityDecorations() {
        val preferences = getSharedPreferences(SettingsActivity.GRAPHICS_PREFERENCES_KEY, MODE_PRIVATE)
        val isFullscreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

        if (isFullscreen) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun initComponents() {
        variationsLayout = findViewById(R.id.variations_layout)
        variationAdapter = VariationAdapter(::onVariationClicked, ::onVariationSelected)

        val variationRecyclerView = findViewById<RecyclerView>(R.id.variation_recycler)
        variationRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        variationRecyclerView.adapter = variationAdapter

        addVariationButton = findViewById(R.id.add_variation_button)
        addVariationButton.setIcon(R.drawable.add_icon)
            .setColorResource(R.color.accent_color)
            .setIconPadding(8, 8, 8, 8)
            .setCornerRadius(180.0f)
            .hideText()
            .setOnClickListener {
                createVariationDialog.show()
            }


        val resumeSessionButton = UIButton2(applicationContext)
        resumeSessionButton
            .setText("Resume practice session")
            .setTextSize(24f)
            .setColorResource(R.color.accent_color)
            .setOnClickListener {
                val intent = Intent(this, PracticeActivity::class.java)

                intent.putExtra("opening_name", openingName)
                intent.putExtra("opening_team", openingTeam.toString())
                intent.putExtra("resume_session", true)

                startActivity(intent)

                popupBar.hide()
            }

        popupBar = findViewById(R.id.extra_buttons_popup_bar)
        popupBar.addButton(resumeSessionButton)
        popupBar.attachToLayout(variationsLayout)
//        popupBar
//            .setText()
//            .attachToLayout(variationsLayout)
//            .setOnClickListener {
//
//            }
    }
}