package com.mjaruijs.fischersplayground.adapters.chatadapter

import android.content.res.Resources
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R

class ChatAdapter(private val resources: Resources, private val messages: ArrayList<ChatMessage> = arrayListOf()) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private var recyclerWidth = 0

    fun clear() {
        println("CLEARING MESSAGES")
        messages.clear()
        notifyDataSetChanged()
    }

    operator fun plusAssign(message: ChatMessage) {
        messages += message
        println("ADDING MESSAGES")
        notifyItemChanged(messages.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if (recyclerWidth == 0) {
            recyclerWidth = parent.width
        }

        return MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.chat_message_layout, parent, false))
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val messageCard = messages[position]
        holder.messageContentView.text = messageCard.message
        holder.messageTimeStampView.text = messageCard.timeStamp
        holder.messageContentView.tag = messageCard

        if (messageCard.type == MessageType.SENT) {
            holder.chatItemLayout.gravity = Gravity.END
            holder.messageCard.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.accent_color, null))
        } else {
            holder.chatItemLayout.gravity = Gravity.START
            holder.messageCard.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.background_color, null))
        }

        holder.messageContentView.maxWidth = (recyclerWidth * 0.7f).toInt()
    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val chatItemLayout: LinearLayout = view.findViewById(R.id.chat_item_layout)
        val messageCard: CardView = view.findViewById(R.id.chat_message_card)
        val messageContentView: TextView = view.findViewById(R.id.message_content)
        val messageTimeStampView: TextView = view.findViewById(R.id.message_time_stamp)
    }

}