package com.mjaruijs.fischersplayground.dialogs

import android.app.Dialog
import android.content.Context
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.playeradapter.PlayerAdapter
import com.mjaruijs.fischersplayground.adapters.playeradapter.PlayerCardItem
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.listeners.OnSearchViewChangedListener
import java.util.*

class CreateGameDialog(private val onInvite: (String, Long, String, String) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var playerCardList: PlayerAdapter

    private var initialized = false

    fun create(id: String, context: Context) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.create_game_dialog)

        val searchBar = dialog.findViewById<SearchView>(R.id.search_bar) ?: return

        searchBar.isIconifiedByDefault = false
        searchBar.isFocusedByDefault = true
        searchBar.isFocusableInTouchMode = true
        searchBar.queryHint = "Search players.."

        searchBar.setOnQueryTextListener(OnSearchViewChangedListener {
            if (searchBar.query.isNotBlank()) {
                NetworkManager.sendMessage(Message(Topic.INFO, "search_players", "${searchBar.query}"))
            }
        })

        playerCardList = PlayerAdapter(id, ::onPlayerClicked)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.available_players_list) ?: return
        recyclerView.adapter = playerCardList
        recyclerView.layoutManager = LinearLayoutManager(context)

        initialized = true
    }

    fun updateId(id: String) {
        playerCardList.id = id
    }

    fun setRecentOpponents(opponents: Stack<Pair<String, String>>) {
        playerCardList.clear()
        for (opponent in opponents.reversed()) {
            playerCardList += PlayerCardItem(opponent.first, opponent.second)
        }
    }

    private fun onPlayerClicked(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        dialog.hide()
        onInvite(inviteId, timeStamp, opponentName, opponentId)
    }

    fun show() {
        if (initialized) {
            val searchBar = dialog.findViewById<SearchView>(R.id.search_bar) ?: return
            searchBar.setQuery("", false)
            dialog.show()
        }
    }

    fun clearPlayers() {
        playerCardList.clear()
    }

    fun addPlayers(name: String, id: String) {
        println("ADDING PLAYERS: $name $id")
        playerCardList += PlayerCardItem(name, id)
    }

}