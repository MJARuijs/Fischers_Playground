package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.playeradapter.PlayerAdapter
import com.mjaruijs.fischersplayground.adapters.playeradapter.PlayerCardItem
import com.mjaruijs.fischersplayground.listeners.OnSearchViewChangedListener
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.NetworkMessage
import com.mjaruijs.fischersplayground.networking.message.Topic
import java.util.*

class CreateGameDialog(private val onInvite: (String, Long, String, String) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var playerCardList: PlayerAdapter

    private var recentOpponents = Stack<Pair<String, String>>()

    private var initialized = false

    fun create(id: String, context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.create_game_dialog)

        val searchBar = dialog.findViewById<SearchView>(R.id.search_bar) ?: return

        searchBar.isIconifiedByDefault = false
        searchBar.isFocusedByDefault = true
        searchBar.isFocusableInTouchMode = true
        searchBar.queryHint = "Search players.."

        searchBar.setOnQueryTextListener(OnSearchViewChangedListener {
            if (searchBar.query.isNotBlank()) {
                NetworkManager.sendMessage(NetworkMessage(Topic.INFO, "search_players", "${searchBar.query}"))
            } else {
                context.runOnUiThread {
                    loadRecentPlayers()
                }
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
        recentOpponents = opponents

        playerCardList.clear()
        for (opponent in opponents.reversed()) {
            playerCardList += PlayerCardItem(opponent.first, opponent.second)
        }
    }

    private fun onPlayerClicked(inviteId: String, timeStamp: Long, opponentName: String, opponentId: String) {
        dialog.hide()
        onInvite(inviteId, timeStamp, opponentName, opponentId)
    }

    private fun loadRecentPlayers() {
        playerCardList.clear()
        for (opponent in recentOpponents.reversed()) {
            playerCardList += PlayerCardItem(opponent.first, opponent.second)
        }
    }

    fun show() {
        if (initialized) {
            val searchBar = dialog.findViewById<SearchView>(R.id.search_bar) ?: return
            searchBar.setQuery("", false)
            loadRecentPlayers()
            dialog.show()
        }
    }

    fun dismiss() {
        if (this::dialog.isInitialized) {
            dialog.dismiss()
        }
    }

    fun clearPlayers() {
        playerCardList.clear()
    }

    fun addPlayers(name: String, id: String) {
        playerCardList += PlayerCardItem(name, id)
    }

}