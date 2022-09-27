package com.mjaruijs.fischersplayground.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.MarginItemDecoration
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatAdapter
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.listeners.OnSwipeTouchListener
import com.mjaruijs.fischersplayground.util.Time

class ChatFragment : Fragment(R.layout.chat_fragment) {

    lateinit var onMessageSent: (ChatMessage) -> Unit
    lateinit var close: () -> Unit

    private lateinit var chatLayout: ConstraintLayout

    private lateinit var chatRecycler: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var inputCard: CardView
    private lateinit var inputBox: EditText

    private lateinit var sendButton: CardView
    private var keyboardHeight = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatLayout = view.findViewById(R.id.chat_item_layout)

        chatAdapter = ChatAdapter(resources)

        chatRecycler = view.findViewById(R.id.chat_recycler_view)
        chatRecycler.layoutManager = LinearLayoutManager(context)
        chatRecycler.adapter = chatAdapter
        chatRecycler.addItemDecoration(MarginItemDecoration(resources.getDimensionPixelSize(R.dimen.chat_padding)))

        inputCard = view.findViewById(R.id.chat_input_card)
        val defaultCardRadius = inputCard.radius

        inputBox = view.findViewById(R.id.chat_input_box)
        inputBox.doOnTextChanged { _, _, _, _ ->
            inputCard.radius = defaultCardRadius / inputBox.lineCount
            chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
//            chatLayout.invalidate()
        }

        inputBox.setOnEditorActionListener { v, actionId, _ ->
            val content = (v as EditText).text.toString()
            if (content.isBlank()) {
                return@setOnEditorActionListener false
            }

            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage(content)
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }

        sendButton = view.findViewById(R.id.send_card)
        sendButton.setOnClickListener {
            sendMessage(inputBox.text.toString())
        }

        chatRecycler.setOnTouchListener(OnSwipeTouchListener(OnSwipeTouchListener.SwipeDirection.RIGHT, ::onSwipe, ::onClick))
        chatRecycler.setOnClickListener {
            close()
        }
    }

    private fun onClick() {
        closeKeyboard()
    }

    private fun onSwipe() {
        closeKeyboard()
        close()
    }

    private fun closeKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun toggle() {
        val offset = if (keyboardHeight == 0) 32 else keyboardHeight + 32
        val constraints = ConstraintSet()
        constraints.clone(chatLayout)
        constraints.connect(R.id.chat_input_card, ConstraintSet.BOTTOM, R.id.chat_item_layout, ConstraintSet.BOTTOM, offset)
        constraints.applyTo(chatLayout)
        chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
    }

    fun translate(distance: Int) {
        keyboardHeight = distance
        toggle()
    }

    override fun onResume() {
        super.onResume()
        inputBox.clearFocus()
    }

    private fun sendMessage(content: String) {
        if (content.isBlank()) {
            return
        }

        val timeStamp = Time.getSimpleTimeStamp()
        val message = ChatMessage(timeStamp, content, MessageType.SENT)

        chatAdapter += message
        chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
        inputBox.text.clear()

        onMessageSent(message)
    }

    fun addReceivedMessage(message: ChatMessage) {
        chatAdapter += message
        chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)
    }

    fun addMessages(messages: ArrayList<ChatMessage>) {
        for (message in messages) {
            chatAdapter += message
        }
        chatRecycler.scrollToPosition(chatAdapter.itemCount - 1)

        chatAdapter.notifyDataSetChanged()
    }

    fun clearMessages() {
        chatAdapter.clear()
    }

}