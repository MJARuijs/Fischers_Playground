package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.util.Time
import kotlin.IllegalArgumentException
import kotlin.collections.ArrayList

class GameAdapter(private val onGameClicked: (GameCardItem) -> Unit, private val onGameDeleted: (String) -> Unit) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    private val games = ArrayList<GameCardItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateGameCard(gameId: String, newStatus: GameStatus, isPlayerWhite: Boolean? = null, hasUpdate: Boolean): Boolean {
        val game = games.find { game -> game.id == gameId } ?: return false

        game.gameStatus = newStatus
        game.isPlayingWhite = isPlayerWhite

        if (hasUpdate) {
            hasUpdate(gameId)
        } else {
            clearUpdate(gameId)
        }

        sort()
        notifyDataSetChanged()
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateGameCard(gameId: String, newStatus: GameStatus, isPlayerWhite: Boolean? = null): Boolean {
        val game = games.find { game -> game.id == gameId } ?: return false

        game.gameStatus = newStatus
        game.isPlayingWhite = isPlayerWhite

        sort()
        notifyDataSetChanged()
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCardStatus(id: String, newStatus: GameStatus, timeStamp: Long) {
        val game = games.find { game -> game.id == id } ?: throw IllegalArgumentException("Could not update game card with id: $id, since it does not exist..")
        game.gameStatus = newStatus
        game.hasUpdate = true
        game.lastUpdated = timeStamp

        sort()
        notifyDataSetChanged()
    }

    fun updateCardStatus(id: String, newStatus: GameStatus) = updateCardStatus(id, newStatus, Time.getFullTimeStamp())

    @SuppressLint("NotifyDataSetChanged")
    operator fun plusAssign(gameCardItem: GameCardItem) {
        games += gameCardItem

        sort()
        notifyDataSetChanged()
    }

    fun hasUpdate(gameId: String) {
        val game = games.find { game -> game.id == gameId } ?: throw IllegalArgumentException("Could not set update-light for game with id: $gameId, since it does not exist..")
        val gameIndex = games.indexOf(game)

        games[gameIndex].hasUpdate = true
        notifyItemChanged(gameIndex)
    }

    fun clearUpdate(gameCardItem: GameCardItem, userId: String) {
        gameCardItem.hasUpdate = false
        val gameIndex = games.indexOf(gameCardItem)
        notifyItemChanged(gameIndex)

        NetworkManager.sendMessage(Message(Topic.INFO, "clear_update", "$userId|${gameCardItem.id}"))
    }

    private fun clearUpdate(gameId: String) {
        val game = games.find { game -> game.id == gameId } ?: throw IllegalArgumentException("Could not set update-light for game with id: $gameId, since it does not exist..")
        val gameIndex = games.indexOf(game)

        games[gameIndex].hasUpdate = false
        notifyItemChanged(gameIndex)
    }

    private fun sort() {
        games.sortWith { gameCard1: GameCardItem, gameCard2: GameCardItem ->
            if (gameCard1.gameStatus.sortingValue > gameCard2.gameStatus.sortingValue) {
                -1
            } else if (gameCard1.gameStatus.sortingValue == gameCard2.gameStatus.sortingValue) {
                if (gameCard1.lastUpdated > gameCard2.lastUpdated) -1 else 1
            } else {
                1
            }
        }
    }

    private fun delete(gameId: String) {
        games.removeIf { gameCard -> gameCard.id == gameId }
        notifyDataSetChanged()
        onGameDeleted(gameId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        return GameViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.match_card, parent, false))
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val gameCard = games[position]
        holder.gameStatusView.setBackgroundColor(gameCard.gameStatus.color)
        holder.opponentNameView.text = gameCard.opponentName
        holder.updateIndicator.visibility = if (gameCard.hasUpdate) VISIBLE else INVISIBLE
        holder.gameLayout.setOnClickListener {
            onGameClicked(gameCard)
        }
        holder.deleteButton.setOnClickListener {
            delete(gameCard.id)
        }
    }

    override fun getItemCount() = games.size

    inner class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val opponentNameView: TextView = view.findViewById(R.id.opponent_name)
        val gameStatusView: View = view.findViewById(R.id.game_status_view)
        val gameLayout: ConstraintLayout = view.findViewById(R.id.game_layout)
        val updateIndicator: ImageView = view.findViewById(R.id.update_indicator)
        val deleteButton: Button = view.findViewById(R.id.delete_match_button)
    }

}