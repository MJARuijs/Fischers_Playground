package com.mjaruijs.fischersplayground.fragments.actionbars

import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.dialogs.OfferDrawDialog
import com.mjaruijs.fischersplayground.dialogs.ResignDialog
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.UIButton

class MultiplayerActionButtonsFragment(private val gameId: String, private val playerId: String, private val isChatOpened: () -> Boolean, requestRender: () -> Unit, networkManager: NetworkManager) : ActionButtonsFragment(R.layout.multiplayer_actionbar, requestRender, networkManager) {

    private lateinit var resignButton: UIButton
    private lateinit var offerDrawButton: UIButton
    private lateinit var redoButton: UIButton
    private val resignDialog = ResignDialog()
    private val offerDrawDialog = OfferDrawDialog()

    override var numberOfButtons = 5

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resignDialog.create(requireContext())
        offerDrawDialog.create(requireContext())

        val textOffset = 65
        val textColor = Color.WHITE
        val buttonBackgroundColor = Color.argb(0.4f, 0.25f, 0.25f, 0.25f)
        resignButton = view.findViewById(R.id.resign_button)
        resignButton
            .setTextYOffset(textOffset)
            .setText("Resign")
            .setColoredDrawable(R.drawable.resign)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setColor(buttonBackgroundColor)
            .setChangeIconColorOnHover(false)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                if (isChatOpened()) {
                    return@setOnClickListener
                }

                resignDialog.show {
                    networkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "resign", "$gameId|$playerId"))
//                    SavedGames.get(gameId)?.status = GameStatus.GAME_LOST

//                    finishActivity(GameStatus.GAME_LOST)
                }
            }
//
        offerDrawButton = view.findViewById(R.id.offer_draw_button)
        offerDrawButton
            .setText("Offer Draw")
            .setColor(buttonBackgroundColor)
            .setColoredDrawable(R.drawable.handshake_2)
            .setButtonTextSize(50f)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setTextYOffset(textOffset)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                if (isChatOpened()) {
                    return@setOnClickListener
                }

                offerDrawDialog.show(gameId, playerId, networkManager)
            }

        redoButton = view.findViewById(R.id.request_redo_button)
        redoButton
            .setText("Undo")
            .setColoredDrawable(R.drawable.rewind)
            .setButtonTextSize(50f)
            .setColor(buttonBackgroundColor)
            .setButtonTextColor(textColor)
            .setChangeIconColorOnHover(false)
            .setTextYOffset(textOffset)
            .setCenterVertically(false)
            .setOnButtonInitialized(::onButtonInitialized)
            .setOnClickListener {
                if (isChatOpened()) {
                    return@setOnClickListener
                }

                networkManager.sendMessage(NetworkMessage(Topic.GAME_UPDATE, "request_undo", "$gameId|$playerId"))
            }

        buttons += resignButton
        buttons += offerDrawButton
        buttons += redoButton
    }
}