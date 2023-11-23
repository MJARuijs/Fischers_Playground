package com.mjaruijs.fischersplayground.activities.opening

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.activities.game.GameActivity
import com.mjaruijs.fischersplayground.activities.settings.SettingsActivity
import com.mjaruijs.fischersplayground.adapters.openingadapter.Opening
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.adapters.variationadapter.Variation
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.chess.game.SinglePlayerGame
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.chess.pieces.PieceType
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.dialogs.CreateVariationDialog
import com.mjaruijs.fischersplayground.dialogs.PieceChooserDialog
import com.mjaruijs.fischersplayground.dialogs.PracticeSettingsDialog
import com.mjaruijs.fischersplayground.fragments.OpeningMovePagerFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.ActionBarFragment.Companion.BACKGROUND_COLOR
import com.mjaruijs.fischersplayground.fragments.actionbars.CreateOpeningActionButtonsFragment
import com.mjaruijs.fischersplayground.fragments.actionbars.GameBarFragment
import com.mjaruijs.fischersplayground.math.vectors.Vector2
import com.mjaruijs.fischersplayground.math.vectors.Vector3
import com.mjaruijs.fischersplayground.opengl.surfaceviews.SurfaceView
import com.mjaruijs.fischersplayground.services.DataManager
import com.mjaruijs.fischersplayground.userinterface.BoardOverlay
import com.mjaruijs.fischersplayground.util.Logger
import com.mjaruijs.fischersplayground.util.Time

class CreateOpeningActivity : AppCompatActivity() {

     var activityName = "create_opening_activity"

     var isSinglePlayer = true

    private var hasUnsavedChanges = false
    private var arrowModeEnabled = false
    private var arrowStartSquare = Vector2(-1, -1)

    private var selectedLine: OpeningLine? = null

    private lateinit var openingName: String
    private lateinit var openingTeam: Team
    private lateinit var variationName: String
    private lateinit var opening: Opening
    private lateinit var variation: Variation

    private lateinit var practiceSettingsDialog: PracticeSettingsDialog
    private lateinit var openingMovesFragment: OpeningMovePagerFragment

    private lateinit var boardOverlay: BoardOverlay

    private lateinit var arrowMenuItem: MenuItem

    private lateinit var renameVariationDialog: CreateVariationDialog

    private lateinit var dataManager: DataManager


    lateinit var glView: SurfaceView

    //    protected lateinit var gameLayout: ConstraintLayout
    protected lateinit var vibrator: Vibrator

    private val pieceChooserDialog = PieceChooserDialog(::onPawnUpgraded)

    protected var displayWidth = 0
    protected var displayHeight = 0

    protected var isPlayingWhite = true

    open lateinit var game: Game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_opening)
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)
        val fullScreen = preferences.getBoolean(SettingsActivity.FULL_SCREEN_KEY, false)

        hideActivityDecorations(fullScreen)

        pieceChooserDialog.create(this)

        glView = findViewById(R.id.opengl_view)
        glView.init(::runOnUIThread, ::onContextCreated, ::onClick, ::onDisplaySizeChanged, isPlayingWhite, ::onExceptionThrown)

        boardOverlay = findViewById(R.id.board_overlay)

        dataManager = DataManager.getInstance(this)

        openingName = intent.getStringExtra("opening_name") ?: "default_opening_name"

        openingTeam = Team.fromString(intent.getStringExtra("opening_team") ?: throw IllegalArgumentException("Failed to create CreateOpeningActivity. Missing essential information: opening_team.."))
        variationName = intent.getStringExtra("variation_name") ?: "default_variation_name"

        opening = dataManager.getOpening(openingName, openingTeam)
        variation = opening.getVariation(variationName) ?: throw IllegalArgumentException("Could not find variation with name: $variationName in opening with name: $openingName")

        if (variation.lines.isNotEmpty()) {
            selectedLine = variation.lines[0]
        }

        isPlayingWhite = openingTeam == Team.WHITE
        game = SinglePlayerGame(isPlayingWhite, Time.getFullTimeStamp(), true)

        practiceSettingsDialog = PracticeSettingsDialog(::onStartPracticing)
        practiceSettingsDialog.create(this as Activity)

        openingMovesFragment = OpeningMovePagerFragment.getInstance(::onLineSelected, ::onLineCleared, ::onMoveClicked, variation.lines)

        renameVariationDialog = CreateVariationDialog(::onVariationRenamed)
        renameVariationDialog.create(this as Activity)

        if (!isPlayingWhite) {
            boardOverlay.swapCharactersForBlack()
        }
        loadCreatingActionButtons()

        supportActionBar?.show()
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.action_bar_view)
        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.text = "$openingName, $variationName"
        supportActionBar?.setBackgroundDrawable(ColorDrawable(BACKGROUND_COLOR))
    }

    override fun onResume() {
        super.onResume()
        Logger.debug(activityName, "Finished creating activity!")

//        dataManager.loadData(applicationContext)
    }

    private fun restorePreferences() {
        val preferences = getSharedPreferences("graphics_preferences", MODE_PRIVATE)

        val cameraRotation = preferences.getString(SettingsActivity.CAMERA_ROTATION_KEY, "") ?: ""
        val fov = preferences.getInt(SettingsActivity.FOV_KEY, 45)
        val pieceScale = preferences.getFloat(SettingsActivity.PIECE_SCALE_KEY, 1.0f)

        if (cameraRotation.isNotBlank()) {
            glView.getRenderer().setCameraRotation(Vector3.fromString(cameraRotation))
        }

        glView.getRenderer().setFoV(fov)
        glView.getRenderer().setPieceScale(pieceScale)
    }

    override fun onDestroy() {
        glView.destroy()
        super.onDestroy()
    }

    inline fun <reified T>findFragment(): T? {
        val fragment = supportFragmentManager.fragments.find { fragment -> fragment is T } ?: return null
        return fragment as T
    }

    protected fun requestRender() {
        glView.requestRender()
    }

    private fun onPawnUpgraded(square: Vector2, pieceType: PieceType, team: Team) {
        game.upgradePawn(square, pieceType, team)
        Thread {
            Thread.sleep(10)
            glView.invalidate()
            requestRender()
        }.start()
    }

    override fun onBackPressed() {
        Logger.debug(activityName, "BACK CLICKED")
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_opening_menu, menu)
        if (menu != null) {
            arrowMenuItem = menu.findItem(R.id.toggle_arrow_mode)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toggle_arrow_mode -> {
                toggleArrowMode(item)
                return true
            }
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
            R.id.rename_variation -> {
                renameVariationDialog.setText(variationName)
                renameVariationDialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onContextCreated() {
        restorePreferences()

//        super.onContextCreated()
//        runOnUiThread {
//            val constraints = ConstraintSet()
//            constraints.clone(gameLayout)
//            constraints.clear(R.id.opengl_view, ConstraintSet.TOP)
//            constraints.clear(R.id.opengl_view, ConstraintSet.BOTTOM)
//
//            constraints.applyTo(gameLayout)
//            gameLayout.invalidate()
//        }
//        boardOverlay.invalidate()

        setGameCallbacks()
        setGameForRenderer()
        Logger.debug(activityName, "Finished creating context!")

    }
    fun setGameForRenderer() {
        glView.setGame(game)
    }

    private fun onPromotePawn(square: Vector2, team: Team): PieceType {
        runOnUiThread {
            pieceChooserDialog.show(square, team)
        }
        return PieceType.QUEEN
    }

    fun setGameCallbacks() {
        game.onPawnPromoted = ::onPromotePawn
        game.onMoveMade = ::onMoveMade
//        super.setGameCallbacks()
        game.onAnimationStarted = ::onMoveAnimationStarted
        game.onAnimationFinished = ::onMoveAnimationFinished
    }

    override fun onPause() {
        if (hasUnsavedChanges) {
            saveOpening()
        }
        super.onPause()
    }

    fun onClick(x: Float, y: Float) {
        boardOverlay.draw()
        if (arrowModeEnabled) {
            if (arrowStartSquare.x == -1f) {
                arrowStartSquare = game.determineSelectedSquare(x, y, displayWidth, displayHeight)
                glView.highlightSquare(arrowStartSquare)
            } else {
                val arrowEndSquare = game.determineSelectedSquare(x, y, displayWidth, displayHeight)
                onArrowToggled(arrowStartSquare, arrowEndSquare)
                arrowStartSquare = Vector2(-1, -1)
                glView.clearHighlightedSquares()
            }
        } else {
            try {
                val vibrateOnClick = getSharedPreferences(SettingsActivity.GAME_PREFERENCES_KEY, MODE_PRIVATE).getBoolean(SettingsActivity.VIBRATE_KEY, false)
                if (vibrateOnClick) {
                    vibrate()
                }
                game.onClick(x, y, displayWidth, displayHeight)
            } catch (e: Exception) {
//            networkManager.sendCrashReport("crash_onclick_log.txt", e.stackTraceToString(), applicationContext)
            }
        }
    }

    private fun vibrate() {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun onMoveAnimationStarted() {
        Logger.debug(activityName, "Move animation started!")
        boardOverlay.clearArrows()
    }

    private fun onMoveAnimationFinished(moveIndex: Int) {
        Logger.debug(activityName, "Move animation finished!")
        boardOverlay.drawArrows(selectedLine!!.arrows[moveIndex] ?: ArrayList())
//        boardOverlay.draw()
    }

    fun getActionBarFragment() = findFragment<GameBarFragment>()

    fun evaluateNavigationButtons() {
        if (game.moves.isNotEmpty()) {
            if (game.getMoveIndex() != -1) {
                (getActionBarFragment())?.enableBackButton()
            } else {
                (getActionBarFragment())?.disableBackButton()
            }
            if (!game.isShowingCurrentMove()) {
                (getActionBarFragment())?.enableForwardButton()
            } else {
                (getActionBarFragment())?.disableForwardButton()
            }
        }
        requestRender()
    }

    fun onMoveMade(move: Move) {
//        super.onMoveMade(move)
        Thread {
            while (getActionBarFragment() == null) {
                Logger.warn(activityName, "Move made, but thread is looping because actionBarFragment is null..")
                Thread.sleep(1000)
            }

            runOnUiThread {
                evaluateNavigationButtons()
            }
        }.start()

//        glView.clearHighlightedSquares()

        runOnUiThread {
            openingMovesFragment.getCurrentOpeningFragment().addMove(move)
        }

        hasUnsavedChanges = true
    }

    private fun onLineSelected(line: OpeningLine, selectedMoveIndex: Int) {
        selectedLine = line
        game.swapMoves(line.getAllMoves(), selectedMoveIndex)
        boardOverlay.drawArrows(line.arrows[selectedMoveIndex] ?: ArrayList())

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
        arrowModeEnabled = false
        arrowStartSquare = Vector2(-1, -1)
        arrowMenuItem.iconTintList = ColorStateList(arrayOf(intArrayOf(0)), intArrayOf(Color.WHITE))

        findFragment<OpeningMovePagerFragment>()?.addLine(selectedLine!!.setupMoves, selectedLine!!.lineMoves, selectedLine!!.arrows)

        requestRender()
    }

    private fun onMoveClicked(move: Move) {
        game.goToMove(move)
        evaluateNavigationButtons()
        openingMovesFragment.getCurrentOpeningFragment().selectMove(game.currentMoveIndex, true)
    }

    private fun onStartRecording() {
        runOnUiThread {
            openingMovesFragment.getCurrentOpeningFragment().setLineHeader()
            hasUnsavedChanges = true
        }
    }

    private fun onArrowToggled(startSquare: Vector2, endSquare: Vector2) {
        val arrow = MoveArrow(startSquare, endSquare)
        if (!arrow.isValidArrow()) {
            return
        }

        boardOverlay.toggleArrow(arrow)
        openingMovesFragment.getCurrentOpeningFragment().toggleArrow(arrow)

        boardOverlay.draw()
        hasUnsavedChanges = true
    }

    private fun onStartPracticing() {
        practiceSettingsDialog.show(applicationContext)
    }

    private fun onStartPracticing(useShuffle: Boolean, practiceArrows: Boolean) {
        if (hasUnsavedChanges) {
            saveOpening()
        }

        practiceSettingsDialog.dismiss()

        val intent = Intent(this, PracticeActivity::class.java)
        intent.putExtra("opening_name", openingName)
        intent.putExtra("opening_team", openingTeam.toString())
        intent.putExtra("resume_session", false)
        intent.putExtra("use_shuffle", useShuffle)
        intent.putExtra("practice_arrows", practiceArrows)
        intent.putStringArrayListExtra("variation_name", arrayListOf(variationName))

        startActivity(intent)
    }

    private fun toggleArrowMode(item: MenuItem) {
        arrowModeEnabled = !arrowModeEnabled
        if (arrowModeEnabled) {
            val enabledColor = ResourcesCompat.getColor(resources, R.color.accent_color, null)
            item.iconTintList = ColorStateList(arrayOf(intArrayOf(0)), intArrayOf(enabledColor))
            arrowStartSquare = Vector2(-1, -1)
            game.clearBoardData()
            requestRender()
        } else {
            glView.clearHighlightedSquares()
            arrowStartSquare = Vector2(-1, -1)
            item.iconTintList = ColorStateList(arrayOf(intArrayOf(0)), intArrayOf(Color.WHITE))
            requestRender()
        }
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

    private fun onVariationRenamed(newName: String) {
        opening.getVariation(variationName)!!.name = newName
        variationName = newName
        supportActionBar?.customView?.findViewById<TextView>(R.id.title_view)?.text = "$openingName, $newName"

        saveOpening()
    }

    private fun saveOpening() {
        Logger.debug(activityName, "Startin Save Opening()")
        opening.getVariation(variationName)!!.clear()

        val openingFragments = openingMovesFragment.getFragments()

        for (fragment in openingFragments) {
            val line = fragment.getOpeningLine()
            opening.getVariation(variationName)!!.addLine(line)
        }

        dataManager.setOpening(openingName, openingTeam, opening, applicationContext)
        dataManager.saveOpenings(applicationContext)
        hasUnsavedChanges = false
        Logger.debug(activityName, "finished Save Opening()")
    }

    private fun hideActivityDecorations(isFullscreen: Boolean) {
        supportActionBar?.hide()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        if (isFullscreen) {
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun runOnUIThread(runnable: () -> Unit) {
        runOnUiThread {
            Logger.debug(activityName, "Running on ui thread")
            runnable()
            Logger.debug(activityName, "Finished running on ui thread")
        }
    }

    open fun onDisplaySizeChanged(width: Int, height: Int) {
        displayWidth = width
        displayHeight = height
    }

    private fun onExceptionThrown(fileName: String, e: Exception) {
//        networkManager.sendCrashReport(fileName, e.stackTraceToString(), applicationContext)
    }

}