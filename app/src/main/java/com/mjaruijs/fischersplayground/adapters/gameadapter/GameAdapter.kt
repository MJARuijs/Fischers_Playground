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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.util.Time

class GameAdapter(private val onGameClicked: (GameCardItem) -> Unit, private val onGameDeleted: (String) -> Unit) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    private val games = ArrayList<GameCardItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateGameCard(gameId: String, newStatus: GameStatus, lastUpdated: Long, isPlayerWhite: Boolean? = null, hasUpdate: Boolean): Boolean {
        val game = games.find { game -> game.id == gameId } ?: return false

        game.lastUpdated = lastUpdated
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

    fun containsCard(id: String): Boolean {
        return games.any { gameCard -> gameCard.id == id }
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

    fun clearUpdate(gameCardItem: GameCardItem) {
        gameCardItem.hasUpdate = false
        val gameIndex = games.indexOf(gameCardItem)
        notifyItemChanged(gameIndex)
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

    fun removeFinishedGames() {
        games.removeIf { gameCard ->
            gameCard.gameStatus == GameStatus.GAME_WON || gameCard.gameStatus == GameStatus.GAME_DRAW || gameCard.gameStatus == GameStatus.GAME_LOST
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        return GameViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.game_card, parent, false))
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val gameCard = games[position]

        holder.gameStatusView.setBackgroundColor(gameCard.gameStatus.color)
        holder.opponentNameView.text = gameCard.opponentName
        holder.updateIndicator.visibility = if (gameCard.hasUpdate) VISIBLE else INVISIBLE
        holder.layout.setOnClickListener {
            onGameClicked(gameCard)
        }
        holder.deleteButton.setOnClickListener {
            games.removeIf { card -> gameCard.id == card.id }
            onGameDeleted(gameCard.id)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = games.size

    inner class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val gameCard: CardView = view.findViewById(R.id.game_card)
        val opponentNameView: TextView = view.findViewById(R.id.opponent_name)
        val gameStatusView: View = view.findViewById(R.id.game_status_view)
        val updateIndicator: ImageView = view.findViewById(R.id.update_indicator)
        val layout: ConstraintLayout = view.findViewById(R.id.game_layout)
        val deleteButton: Button = view.findViewById(R.id.delete_button)
    }

}