package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.dialogs.ResignDialog
import com.mjaruijs.fischersplayground.userinterface.UIButton

class MultiplayerActionButtonsFragment : Fragment(R.layout.multiplayer_actionbar) {

    private var maxTextSize = Float.MAX_VALUE

    private val resignDialog = ResignDialog()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resignDialog.create(requireContext())

        val textOffset = 65
        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)
        val resignButton = view.findViewById<UIButton>(R.id.resign_button)
//        resignButton
//            .setTextYOffset(textOffset)
//            .setText("Resign")
//            .setColoredDrawable(R.drawable.resign)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if (isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                resignDialog.show(gameId, id) {
//                    NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "resign", "$gameId|$id"))
////                    SavedGames.get(gameId)?.status = GameStatus.GAME_LOST
//
//                    finishActivity(GameStatus.GAME_LOST)
//                }
//            }
//
//        val offerDrawButton = view.findViewById<UIButton>(R.id.offer_draw_button)
//        offerDrawButton
//            .setText("Offer Draw")
//            .setColor(buttonBackgroundColor)
//            .setColoredDrawable(R.drawable.handshake_2)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if (isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                offerDrawDialog.show(gameId, id)
//            }
//
//        val redoButton = view.findViewById<UIButton>(R.id.request_redo_button)
//        redoButton
//            .setText("Undo")
//            .setColoredDrawable(R.drawable.rewind)
//            .setButtonTextSize(50f)
//            .setColor(buttonBackgroundColor)
//            .setButtonTextColor(textColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if (isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                NetworkManager.sendMessage(Message(Topic.GAME_UPDATE, "request_undo", "$gameId|$id"))
//            }
//
//        val backButton = view.findViewById<UIButton>(R.id.back_button)
//        backButton
//            .setText("Back")
//            .setColoredDrawable(R.drawable.arrow_back)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .disable()
//            .setOnClickListener {
//                if ((it as UIButton).disabled || isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                val buttonStates = game.showPreviousMove()
//                if (buttonStates.first) {
//                    it.disable()
//                }
//                if (buttonStates.second) {
//                    game.clearBoardData()
//                    view.findViewById<UIButton>(R.id.forward_button)?.enable()
//                }
//                glView.requestRender()
//            }
//
//        val forwardButton = view.findViewById<UIButton>(R.id.forward_button)
//        forwardButton
//            .setText("Forward")
//            .setColoredDrawable(R.drawable.arrow_forward)
//            .setButtonTextSize(50f)
//            .setButtonTextColor(textColor)
//            .setColor(buttonBackgroundColor)
//            .setChangeIconColorOnHover(false)
//            .setTextYOffset(textOffset)
//            .disable()
//            .setCenterVertically(false)
//            .setOnButtonInitialized(::onButtonInitialized)
//            .setOnClickListener {
//                if ((it as UIButton).disabled || isChatOpened()) {
//                    return@setOnClickListener
//                }
//
//                val buttonStates = game.showNextMove()
//                if (buttonStates.first) {
//                    it.disable()
//                }
//                if (buttonStates.second) {
//                    view.findViewById<UIButton>(R.id.back_button)?.enable()
//                }
//                glView.requestRender()
//            }
    }

    private fun onButtonInitialized(textSize: Float) {
        if (textSize < maxTextSize) {
            maxTextSize = textSize
//            findViewById<UIButton>(R.id.resign_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.offer_draw_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.request_redo_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.back_button).setButtonTextSize(maxTextSize)
//            findViewById<UIButton>(R.id.forward_button).setButtonTextSize(maxTextSize)
        }
    }
}