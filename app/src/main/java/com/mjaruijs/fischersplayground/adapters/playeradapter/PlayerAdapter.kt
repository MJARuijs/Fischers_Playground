package com.mjaruijs.fischersplayground.adapters.playeradapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.Preferences
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.UIButton

class PlayerAdapter(private val id: String, private val onInvite: (String, String, String) -> Unit) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    private val players = ArrayList<PlayerCardItem>()

    operator fun plusAssign(player: PlayerCardItem) {
        players += player
        notifyDataSetChanged()
    }

    fun clear() {
        players.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        return PlayerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.player_card_view, parent, false))
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val playerCard = players[position]
        holder.nameButton.buttonText = playerCard.name
        holder.nameButton.setOnClickListener {
            val inviteId = "${id}_${players[position].id}_${System.nanoTime()}"
            onInvite(inviteId, players[position].name, players[position].id)

            NetworkManager.sendMessage(Message(Topic.INFO, "invite", "${players[position].id}|$inviteId"))
        }
    }

    override fun getItemCount() = players.size

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var nameButton: UIButton = view.findViewById(R.id.player_name)
    }
}