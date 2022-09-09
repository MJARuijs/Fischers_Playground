package com.mjaruijs.fischersplayground.fragments

import android.content.Context
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.MarginItemDecoration
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatAdapter
import com.mjaruijs.fischersplayground.adapters.chatadapter.ChatMessage
import com.mjaruijs.fischersplayground.adapters.chatadapter.MessageType
import com.mjaruijs.fischersplayground.util.Time
import kotlin.math.abs

class ChatFragment(private val onMessageSent: (ChatMessage) -> Unit, private val close: () -> Unit) : Fragment(R.layout.chat_fragment) {

    private lateinit var constraintLayout: ConstraintLayout

    private lateinit var chatRecycler: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var inputCard: CardView
    private lateinit var inputBox: EditText

    private lateinit var sendButton: CardView
    private var keyboardHeight = -1

    private var previousX = 0f
    private var previousY = 0f

    private var totalDX = 0f
    private var holdStartTime = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        constraintLayout = view.findViewById(R.id.chat_layout)

        chatAdapter = ChatAdapter()

        chatRecycler = view.findViewById(R.id.chat_recycler_view)
        chatRecycler.layoutManager = LinearLayoutManager(context)
        chatRecycler.adapter = chatAdapter
        chatRecycler.addItemDecoration(MarginItemDecoration(resources.getDimensionPixelSize(R.dimen.chat_padding)))

        inputCard = view.findViewById(R.id.chat_input_card)
        inputBox = view.findViewById(R.id.chat_input_box)
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

        chatRecycler.setOnClickListener {
            close()
        }

        chatRecycler.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                return true
            }

            override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    totalDX = 0f
                    holdStartTime = System.currentTimeMillis()

                    previousX = event.x
                    previousY = event.y
                }
                if (event.action == MotionEvent.ACTION_MOVE) {
                    val dx = event.x - previousX

                    totalDX += dx

                    if (abs(dx) > 5.0f) {
//                        translate(-dx)
                    }

                    previousX = event.x
                    previousY = event.y
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    val currentTime = System.currentTimeMillis()
                    val totalHoldTime = currentTime - holdStartTime
                    holdStartTime = 0L

                    if (totalDX < -20f) {
                        closeKeyboard()
                        close()
                    } else if (totalHoldTime < 250L) {
                        closeKeyboard()
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun closeKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    private fun toggle() {
        val offset = if (keyboardHeight == 0) 32 else keyboardHeight + 32
        val constraints = ConstraintSet()
        constraints.clone(constraintLayout)
        constraints.connect(R.id.chat_input_card, ConstraintSet.BOTTOM, R.id.chat_layout, ConstraintSet.BOTTOM, offset)
        constraints.applyTo(constraintLayout)

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
        chatAdapter.notifyDataSetChanged()
    }

}