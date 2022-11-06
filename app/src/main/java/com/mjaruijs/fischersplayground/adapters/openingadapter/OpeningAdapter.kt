package com.mjaruijs.fischersplayground.adapters.openingadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Team

class OpeningAdapter(private val onOpeningClicked: (String, Team) -> Unit, private val openings: ArrayList<Opening> = arrayListOf()) : RecyclerView.Adapter<OpeningAdapter.OpeningViewHolder>() {

    fun contains(opening: Opening): Boolean {
        return openings.contains(opening)
    }

    operator fun plusAssign(opening: Opening) {
        openings += opening
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpeningViewHolder {
        return OpeningViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.opening_card, parent, false))
    }

    override fun onBindViewHolder(holder: OpeningViewHolder, position: Int) {
        val opening = openings[position]
        holder.openingNameView.text = opening.name
        holder.openingTeamView.setBackgroundColor(opening.team.color)
        holder.openingCard.setOnClickListener {
            onOpeningClicked(opening.name, opening.team)
        }
    }

    override fun getItemCount() = openings.size

    inner class OpeningViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val openingCard: CardView = view.findViewById(R.id.opening_card)
        val openingNameView: TextView = view.findViewById(R.id.opening_name)
        val openingTeamView: View = view.findViewById(R.id.opening_team)
    }

}