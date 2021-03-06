package com.mjaruijs.fischersplayground.adapters.chatadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R

class ChatAdapter(private val messages: ArrayList<ChatMessage> = arrayListOf()) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private var recyclerWidth = 0

    operator fun plusAssign(message: ChatMessage) {
        messages += message
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

        val params = holder.messageCard.layoutParams as RelativeLayout.LayoutParams
        if (messageCard.type == MessageType.SENT) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
        }

        holder.messageCard.layoutParams = params
        holder.messageContentView.maxWidth = (recyclerWidth * 0.7f).toInt()

    }

    override fun getItemCount() = messages.size

    inner class MessageViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val messageCard: CardView = view.findViewById(R.id.chat_message_card)
        val messageContentView: TextView = view.findViewById(R.id.message_content)
        val messageTimeStampView: TextView = view.findViewById(R.id.message_time_stamp)
    }

}