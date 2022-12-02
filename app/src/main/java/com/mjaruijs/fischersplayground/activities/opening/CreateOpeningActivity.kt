package com.mjaruijs.fischersplayground.activities.opening

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.GameActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.adapters.variationadapter.Variation
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.pieces.Move
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.fragments.OpeningMovePagerFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionBarFragment.Companion.BACKGROUND_COLOR
import com.mjaruijs.fischersplayground.fragments.actionbars.CreateOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.Time

class CreateOpeningActivity : GameActivity() {

    override var activityName = "create_opening_activity"

    override var isSinglePlayer = true

    private var selectedLine: OpeningLine? = null

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var variationName: String
    private lateinit var opening: Opening
    private lateinit var variation: Variation

    private var hasUnsavedChanges = false

    private lateinit var openingMovesFragment: OpeningMovePagerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<ImageView>(R.id.open_chat_button).visibility = View.GONE
        findViewById<FragmentContainerView>(R.id.upper_fragment_container).visibility = View.GONE

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"

        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create CreateOpeningActivity. Missing essential information: opening_team.."))
        variationName = intent.getStringExtra("variation_name") ?: "default_variation_name"

        opening = dataManager.getOpening(openingName, openingTeam)
        variation = opening.getVariation(variationName) ?: throw IllegalArgumentException("Could not find variation with name: $variationName in opening with name: $openingName")

        if (variation.lines.isNotEmpty()) {
            selectedLine = variation.lines[0]
        }

        isPlayingWhite = openingTeam == Team.WHITE
        game = SinglePlayerGame(isPlayingWhite, Time.getFullTimeStamp())

        openingMovesFragment = OpeningMovePagerFragment.getInstance(::onLineSelected, ::onLineCleared, ::onMoveClicked, variation.lines)

        loadCreatingActionButtons()

        supportActionBar?.show()
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar_view)
        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.text = "$openingName, $variationName"
//        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.setAutoSizeTextTypeUniformWithConfiguration(24, 100, 1, TypedValue.COMPLEX_UNIT_SP)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(BACKGROUND_COLOR))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_opening_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_line_button -> {
                openingMovesFragment.deleteCurrentLine()
                hasUnsavedChanges = true
                return true
            }
            R.id.copy_opening_to_clipboard -> {
                saveOpening()
                val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Opening", opening.toString())

                clipBoard.setPrimaryClip(clip)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onContextCreated() {
        super.onContextCreated()
        runOnUiThread {
            val constraints = ConstraintSet()
            constraints.clone(gameLayout)
            constraints.clear(R.id.opengl_view, ConstraintSet.TOP)
            constraints.clear(R.id.opengl_view, ConstraintSet.BOTTOM)

            constraints.applyTo(gameLayout)
            gameLayout.invalidate()
        }

        setGameCallbacks()
        setGameForRenderer()
    }

    override fun onPause() {
        if (hasUnsavedChanges) {
            saveOpening()
        }
        super.onPause()
    }

    override fun onMoveMade(move: Move) {
        super.onMoveMade(move)

        glView.clearHighlightedSquares()

        runOnUiThread {
            openingMovesFragment.getCurrentOpeningFragment().addMove(move)
        }

        hasUnsavedChanges = true
    }

    private fun onLineSelected(line: OpeningLine, selectedMoveIndex: Int) {
        selectedLine = line
        game.swapMoves(line.getAllMoves(), selectedMoveIndex)
        evaluateNavigationButtons()
        requestRender()
    }

    private fun onLineCleared() {
        game.resetMoves()
        selectedLine?.clearMoves()
        evaluateNavigationButtons()
        requestRender()
    }

    // TODO: Maybe add a dialog that allows copying options based on current line
    // Don't copy
    // Copy as is: setup moves -> setup moves & line moves -> line moves
    // Copy setup moves only: setup moves -> setup moves & no line moves
    // Copy line moves: setup moves -> setup moves & line moves -> setup moves. This is the current implementation
    // Maybe allow copying a selection of moves?
    private fun onLineAdded() {
        saveOpening()

        if (selectedLine == null) {
            return
        }
        if (selectedLine!!.setupMoves.isEmpty()) {
            return
        }
        findFragment<OpeningMovePagerFragment>()?.addLine(selectedLine!!.setupMoves, selectedLine!!.lineMoves)
        requestRender()
    }

    private fun onMoveClicked(move: Move, deleteModeActive: Boolean) {
        Logger.debug(activityName, "Move Clicked")
        if (deleteModeActive) {
            game.clearBoardData()
        } else {
            game.goToMove(move)

            evaluateNavigationButtons()
            openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex, true)
        }
    }

    private fun onStartRecording() {
        runOnUiThread {
            openingMovesFragment.getCurrentOpeningFragment().setLineHeader()
            hasUnsavedChanges = true
        }
    }

    private fun onStartPracticing() {
        stayingInApp = true

        if (hasUnsavedChanges) {
            saveOpening()
        }

        val intent = Intent(this, PracticeActivity::class.java)
        intent.putExtra("opening_name", openingName)
        intent.putExtra("opening_team", openingTeam.toString())
        intent.putStringArrayListExtra("variation_name", arrayListOf(variationName))

        startActivity(intent)
    }

    private fun onBackClicked() {
        openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex, true)
    }

    private fun onForwardClicked() {
        openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex, true)
    }

    private fun loadCreatingActionButtons() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.action_buttons_fragment, CreateOpeningActionButtonsFragment.getInstance(game, ::evaluateNavigationButtons, ::onStartRecording, ::onLineAdded, ::onStartPracticing, ::onBackClicked, ::onForwardClicked))
            replace(R.id.lower_fragment_container, openingMovesFragment)
        }
    }

    private fun saveOpening() {
        opening.getVariation(variationName)!!.clear()

        val openingFragments = openingMovesFragment.getFragments()

        for (fragment in openingFragments) {
            val line = fragment.getOpeningLine()
            opening.getVariation(variationName)!!.addLine(line)
        }
//        Logger.debug(activityName, "Going to save new opening with name: $convertedOpeningName")
        networkManager.sendMessage(NetworkMessage(Topic.NEW_OPENING, "$userId|$openingName|$openingTeam|$opening"))
        dataManager.setOpening(openingName, openingTeam, opening)
        dataManager.saveOpenings(applicationContext)
        hasUnsavedChanges = false
    }

}