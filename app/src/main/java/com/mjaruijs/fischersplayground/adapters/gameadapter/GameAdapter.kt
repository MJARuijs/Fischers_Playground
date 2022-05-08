package com.mjaruijs.fischersplayground.adapters.gameadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import kotlin.IllegalArgumentException
import kotlin.collections.ArrayList

class GameAdapter(private val onGameClicked: (GameCardItem) -> Unit) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    private val games = ArrayList<GameCardItem>()

    fun updateGameCard(gameId: String, opponentName: String, newStatus: GameStatus, isPlayerWhite: Boolean? = null, createGameIfAbsent: Boolean = false) {
        val game = games.find { game -> game.id == gameId }

        if (game == null && !createGameIfAbsent) {
            throw IllegalArgumentException("Could not find game card with id: $gameId")
        } else if (game == null) {
            val newGame = GameCardItem(gameId, opponentName, newStatus, isPlayerWhite)
            plusAssign(newGame)
        } else {
            game.gameStatus = newStatus
            game.isPlayingWhite = isPlayerWhite

//            sort()
//            notifyDataSetChanged()
            val gameIndex = games.indexOf(game)
            notifyItemChanged(gameIndex)
        }
    }

    fun updateCardStatus(id: String, newStatus: GameStatus) {
        val game = games.find { game -> game.id == id } ?: throw IllegalArgumentException("Could not update game card with id: $id, since it does not exist")
        game.gameStatus = newStatus

//        sort()
//        notifyDataSetChanged()
        val gameIndex = games.indexOf(game)
        notifyItemChanged(gameIndex)
    }

    operator fun plusAssign(gameCardItem: GameCardItem) {
        games += gameCardItem

//        sort()
//        notifyDataSetChanged()
        notifyItemChanged(games.size - 1)
    }

    private fun sort() {
        games.sortWith { gameCard1: GameCardItem, gameCard2: GameCardItem ->
            if (gameCard1.gameStatus.sortingValue > gameCard2.gameStatus.sortingValue) {
                1
            } else if (gameCard1.gameStatus.sortingValue == gameCard2.gameStatus.sortingValue) {
                0
            } else {
                1
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        return GameViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.match_card, parent, false))
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val gameCard = games[position]
        holder.gameStatusView.setBackgroundColor(gameCard.gameStatus.color)
        holder.opponentNameView.text = gameCard.opponentName

        holder.gameLayout.setOnClickListener {
            onGameClicked(gameCard)
        }
    }

    override fun getItemCount() = games.size

    inner class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val opponentNameView: TextView = view.findViewById(R.id.opponent_name)
        val gameStatusView: View = view.findViewById(R.id.game_status_view)
        val gameLayout: ConstraintLayout = view.findViewById(R.id.game_layout)
    }

}