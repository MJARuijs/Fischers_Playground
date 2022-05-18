package com.mjaruijs.fischersplayground.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.MarginItemDecoration
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatAdapter
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.Time

class ChatFragment : Fragment(R.layout.chat_fragment) {

    private lateinit var chatAdapter: ChatAdapter

    private var gameId = ""
    private var userId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameId = requireArguments().getString("game_id") ?: throw IllegalArgumentException("Missing essential information in ChatFragment: game_id")
        userId = requireArguments().getString("user_id") ?: throw IllegalArgumentException("Missing essential information in ChatFragment: user_id")
        chatAdapter = ChatAdapter()

        val chatRecycler = view.findViewById<RecyclerView>(R.id.chat_recycler_view)
        chatRecycler.layoutManager = LinearLayoutManager(context)
        chatRecycler.adapter = chatAdapter
        chatRecycler.addItemDecoration(MarginItemDecoration(resources.getDimensionPixelSize(R.dimen.chat_padding)))

        val inputBox = view.findViewById<EditText>(R.id.chat_input_box)

        inputBox.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                sendMessage((v as EditText).text.toString())
                return@setOnKeyListener true
            }

            return@setOnKeyListener false
        }

        view.findViewById<ImageView>(R.id.send_button).setOnClickListener {
            sendMessage(inputBox.text.toString())
        }
    }

    private fun sendMessage(content: String) {
        val timeStamp = Time.getSimpleTimeStamp()
        chatAdapter += ChatMessage(timeStamp, content, MessageType.SENT)
        NetworkManager.sendMessage(Message(Topic.CHAT_MESSAGE, "", "$gameId|$userId|$timeStamp|$content"))
    }

    fun addReceivedMessage(message: ChatMessage) {
        chatAdapter += message
    }

    fun addReceivedMessage(timeStamp: String, message: String) {
        chatAdapter += ChatMessage(timeStamp, message, MessageType.RECEIVED)
    }

//    fun closeChat() {
//        val chatBoxAnimator = ObjectAnimator.ofFloat(chatContainerView, "x", -chatTranslation.toFloat())
//        val chatButtonAnimator = ObjectAnimator.ofFloat(openChatButton, "x", 0.0f)
//
//        chatBoxAnimator.duration = 500L
//        chatButtonAnimator.duration = 500L
//
//        chatBoxAnimator.start()
//        chatButtonAnimator.start()
//
//        chatOpened = false
//    }

    private fun getScreenWidth(): Int? {
        val windowMetrics = activity?.windowManager?.currentWindowMetrics ?: return null
        val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        return windowMetrics.bounds.width() - insets.left - insets.right
    }
}