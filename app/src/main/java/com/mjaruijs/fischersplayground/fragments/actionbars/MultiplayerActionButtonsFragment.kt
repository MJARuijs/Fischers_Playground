package com.mjaruijs.fischersplayground.fragments.actionbars

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.game.Game
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class MultiplayerActionButtonsFragment : GameBarFragment() {

    private lateinit var resignButton: UIButton2
    private lateinit var offerDrawButton: UIButton2
    private lateinit var redoButton: UIButton2
//    private lateinit var cancelMoveButton: UIButton2
//    private lateinit var confirmMoveButton: UIButton2
//    private lateinit var extraButtonsLayout: LinearLayout

//    private lateinit var hideButtonAnimator: ObjectAnimator
//    private lateinit var showButtonAnimator: ObjectAnimator

    private lateinit var onRequestUndo: () -> Unit
    private lateinit var onOfferDrawClicked: () -> Unit
    private lateinit var onResignClicked: () -> Unit
    private lateinit var onCancelMove: () -> Unit
    private lateinit var onConfirmMove: (String) -> Unit

    private var moveNotation: String? = null

//    private var showingExtraButtons = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)
        resignButton = UIButton2(requireContext())
        resignButton
            .setText("Resign")
            .setIcon(R.drawable.resign)
            .setIconPadding(0, 4, 0, 0)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onResignClicked()
            }

        offerDrawButton = UIButton2(requireContext())
        offerDrawButton
            .setText("Offer Draw")
            .setColor(buttonBackgroundColor)
            .setIcon(R.drawable.handshake_2)
            .setIconPadding(0, 4, 0, 0)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onOfferDrawClicked()
            }

        redoButton = UIButton2(requireContext())
        redoButton
            .setText("Undo")
            .setColor(buttonBackgroundColor)
            .setIcon(R.drawable.rewind)
            .setIconPadding(0, 4, 0, 0)
            .setTextSize(TEXT_SIZE)
            .setColor(BACKGROUND_COLOR)
            .setOnClickListener {
                onRequestUndo()
            }


        addButtonToLeft(redoButton)
        addButtonToLeft(offerDrawButton)
        addButtonToLeft(resignButton)

//        buttons += resignButton
//        buttons += offerDrawButton
//        buttons += redoButton

//        extraButtonsLayout = view.findViewById(R.id.extra_buttons_layout)

//        cancelMoveButton = UIButton2(requireContext())
//        cancelMoveButton
//            .setIcon(R.drawable.close_icon)
//            .setColor(BACKGROUND_COLOR)
//            .setOnClickListener {
//                hideExtraButtons()
//                onCancelMove()
//            }
//
//        confirmMoveButton = UIButton2(requireContext())
//        confirmMoveButton
//            .setIcon(R.drawable.check_mark_icon)
//            .setColorResource(R.color.accent_color)
//            .setOnClickListener {
//                hideExtraButtons()
//                if (moveNotation != null) {
//                    onConfirmMove(moveNotation!!)
//                }
//            }
    }

    fun disableResignButton() {
        resignButton.disable()
    }

    fun disableDrawButton() {
        offerDrawButton.disable()
    }

    fun disableUndoButton() {
        redoButton.disable()
    }
//
//    override fun onPause() {
//        super.onPause()
//
////        if (this::offerDrawDialog.isInitialized) {
////            offerDrawDialog.dismiss()
////        }
////        if (this::confirmResignationDialog.isInitialized) {
////            confirmResignationDialog.dismiss()
////        }
//    }
//
//    fun initializeAnimator(height: Int) {
////        extraButtonsLayout.translationY += height
//
////        hideButtonAnimator = ObjectAnimator.ofFloat(extraButtonsLayout, "y", 0f)
////        hideButtonAnimator.duration = 250L
//
////        showButtonAnimator = ObjectAnimator.ofFloat(extraButtonsLayout, "y", height.toFloat())
//    }
//
//    fun showExtraButtons(moveNotation: String, duration: Long = 250L) {
//        this.moveNotation = moveNotation
////        hideButtonAnimator.duration = duration
////        hideButtonAnimator.start()
////        showingExtraButtons = true
//    }
//
//    private fun hideExtraButtons(duration: Long = 250L) {
////        showButtonAnimator.duration = duration
////        showButtonAnimator.start()
////        showingExtraButtons = false
//    }

    override fun onDestroy() {
        super.onDestroy()
//        confirmResignationDialog.destroy()
//        offerDrawDialog.destroy()
    }

    companion object {

        fun getInstance(game: Game, evaluateNavigationButtons: () -> Unit, onRequestUndo: () -> Unit, onOfferDrawClicked: () -> Unit, onResignClicked: () -> Unit, onCancelMove: () -> Unit, onConfirmMove: (String) -> Unit): MultiplayerActionButtonsFragment {
            val fragment = MultiplayerActionButtonsFragment()
            fragment.init(game, evaluateNavigationButtons)
            fragment.onRequestUndo = onRequestUndo
            fragment.onOfferDrawClicked = onOfferDrawClicked
            fragment.onResignClicked = onResignClicked
            fragment.onCancelMove = onCancelMove
            fragment.onConfirmMove = onConfirmMove
            return fragment
        }

    }

}