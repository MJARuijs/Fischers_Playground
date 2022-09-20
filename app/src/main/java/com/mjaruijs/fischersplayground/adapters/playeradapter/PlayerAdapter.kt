package com.mjaruijs.fischersplayground.adapters.playeradapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.UIButton
import com.mjaruijs.fischersplayground.util.Time

class PlayerAdapter(var id: String, private val onInvite: (String, Long, String, String) -> Unit, private val networkManager: NetworkManager) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

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
        return PlayerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.invite_player_card, parent, false))
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val playerCard = players[position]
        holder.nameButton.buttonText = playerCard.name
        holder.nameButton.setButtonTextSize(150f)
        holder.nameButton.setColor(Color.TRANSPARENT)
        holder.nameButton.setOnClick {

            val inviteId = "${id}_${players[position].id}_${System.nanoTime()}"
            val timeStamp = Time.getFullTimeStamp()

            onInvite(inviteId, timeStamp, players[position].name, players[position].id)

            networkManager.sendMessage(NetworkMessage(Topic.INVITE, "${players[position].id}|$inviteId|$timeStamp"))
        }
    }

    override fun getItemCount() = players.size

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var nameButton: UIButton = view.findViewById(R.id.player_name)
    }
}