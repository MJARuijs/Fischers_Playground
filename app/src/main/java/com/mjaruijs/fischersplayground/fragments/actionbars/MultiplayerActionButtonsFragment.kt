package com.mjaruijs.fischersplayground.fragments.actionbars

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.dialogs.DoubleButtonDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.ScaleType
import com.mjaruijs.fischersplayground.userinterface.UIButton

class MultiplayerActionButtonsFragment(private val gameId: String, private val userId: String, private val isChatOpened: () -> Boolean, private val onOfferDraw: () -> Unit, private val onResign: () -> Unit, private val onCancelMove: () -> Unit, private val onConfirmMove: (String) -> Unit, requestRender: () -> Unit, networkManager: NetworkManager) : ActionButtonsFragment(R.layout.multiplayer_actionbar) {

    private lateinit var resignButton: UIButton
    private lateinit var offerDrawButton: UIButton
    private lateinit var redoButton: UIButton
    private lateinit var cancelMoveButton: UIButton
    private lateinit var confirmMoveButton: UIButton
    private lateinit var extraButtonsLayout: LinearLayout

    private lateinit var hideButtonAnimator: ObjectAnimator
    private lateinit var showButtonAnimator: ObjectAnimator

    private lateinit var confirmResignationDialog: DoubleButtonDialog
    private lateinit var offerDrawDialog: DoubleButtonDialog

    private var moveNotation: String? = null

    private var showingExtraButtons = false

    override var numberOfButtons = 5

    init {
        this.requestRender = requestRender
        this.networkManager = networkManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        confirmResignationDialog = DoubleButtonDialog(requireActivity(), "No Way Back", "Are you sure you want to resign?", "Cancel", "Yes", onResign)
        offerDrawDialog = DoubleButtonDialog(requireActivity(), "Offer Draw", "Are you sure you want to offer a draw?", "Cancel", "Yes", onOfferDraw)

        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)
        resignButton = view.findViewById(R.id.resign_button)
        resignButton
            .setText("Resign")
            .setColoredDrawable(R.drawable.resign)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                if (isChatOpened()) {
                    return@setOnClick
                }

                confirmResignationDialog.show()
            }

        offerDrawButton = view.findViewById(R.id.offer_draw_button)
        offerDrawButton
            .setText("Offer Draw")
            .setColor(buttonBackgroundColor)
            .setColoredDrawable(R.drawable.handshake_2)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                if (isChatOpened()) {
                    return@setOnClick
                }

                offerDrawDialog.setRightOnClick {
                    networkManager.sendMessage(NetworkMessage(Topic.DRAW_OFFERED, "$gameId|$userId"))
                }
                offerDrawDialog.show()
            }

        redoButton = view.findViewById(R.id.request_redo_button)
        redoButton
            .setText("Undo")
            .setColoredDrawable(R.drawable.rewind)
            .setButtonTextSize(50f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClick {
                if (isChatOpened()) {
                    return@setOnClick
                }

                networkManager.sendMessage(NetworkMessage(Topic.UNDO_REQUESTED, "$gameId|$userId"))
            }

        buttons += resignButton
        buttons += offerDrawButton
        buttons += redoButton

        extraButtonsLayout = view.findViewById(R.id.extra_buttons_layout)

        cancelMoveButton = view.findViewById(R.id.cancel_move_button)
        cancelMoveButton
            .setIconScaleType(ScaleType.SQUARE)
            .setColoredDrawable(R.drawable.close_icon)
            .setOnClick {
                hideExtraButtons()
                onCancelMove()
            }

        confirmMoveButton = view.findViewById(R.id.confirm_move_button)
        confirmMoveButton
            .setIconScaleType(ScaleType.SQUARE)
            .setColoredDrawable(R.drawable.check_mark_icon)
            .setColor(235, 186, 145)
            .setOnClick {
                hideExtraButtons()
                if (moveNotation != null) {
                    onConfirmMove(moveNotation!!)
                }
            }
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

    override fun onPause() {
        super.onPause()

        if (this::offerDrawDialog.isInitialized) {
            offerDrawDialog.dismiss()
        }
        if (this::confirmResignationDialog.isInitialized) {
            confirmResignationDialog.dismiss()
        }
    }

    fun initializeAnimator(height: Int) {
        extraButtonsLayout.translationY += height

        hideButtonAnimator = ObjectAnimator.ofFloat(extraButtonsLayout, "y", 0f)
        hideButtonAnimator.duration = 250L

        showButtonAnimator = ObjectAnimator.ofFloat(extraButtonsLayout, "y", height.toFloat())
    }

    fun showExtraButtons(moveNotation: String, duration: Long = 250L) {
        this.moveNotation = moveNotation
        hideButtonAnimator.duration = duration
        hideButtonAnimator.start()
        showingExtraButtons = true
    }

    private fun hideExtraButtons(duration: Long = 250L) {
        showButtonAnimator.duration = duration
        showButtonAnimator.start()
        showingExtraButtons = false
    }

    override fun onDestroy() {
        super.onDestroy()
        resignButton.destroy()
        offerDrawButton.destroy()
        redoButton.destroy()
        cancelMoveButton.destroy()
        confirmMoveButton.destroy()
        confirmResignationDialog.destroy()
        offerDrawDialog.destroy()
    }

}