package com.mjaruijs.fischersplayground.dialogs

import android.app.Dialog
import android.view.View
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.playeradapter.PlayerAdapter
import com.mjaruijs.fischersplayground.adapters.playeradapter.PlayerCardItem
import com.mjaruijs.fischersplayground.networking.NetworkManager
import com.mjaruijs.fischersplayground.networking.message.Message
import com.mjaruijs.fischersplayground.networking.message.Topic
import com.mjaruijs.fischersplayground.userinterface.OnSearchViewChangedListener

class InvitePlayerDialog(private val onInvite: (String, String, String) -> Unit) {

    private lateinit var dialog: Dialog
    private lateinit var playerCardList: PlayerAdapter

    private var initialized = false

    fun create(id: String, view: View) {
        dialog = Dialog(view.context)
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
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        initialized = true
        dialog.show()
    }

    private fun onPlayerClicked(inviteId: String, opponentName: String, opponentId: String) {
        dialog.hide()
        onInvite(inviteId, opponentName, opponentId)
    }

    fun show() {
        if (!initialized) {
            println("DIALOG IS NOT INITIALIZED YET")
        } else {
            dialog.show()
        }
    }

    fun addPlayers(name: String, id: String) {
        playerCardList += PlayerCardItem(name, id)
    }

}