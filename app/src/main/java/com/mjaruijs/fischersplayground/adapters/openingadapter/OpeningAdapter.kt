package com.mjaruijs.fischersplayground.adapters.openingadapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.TEXT_ALIGNMENT_TEXT_START
import android.view.View.TEXT_ALIGNMENT_VIEW_START
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.chess.pieces.Team
import com.mjaruijs.fischersplayground.userinterface.UIButton2

class OpeningAdapter(private val onOpeningClicked: (String, Team) -> Unit, private val onDeleteOpening: (Opening) -> Unit, private val openings: ArrayList<Opening> = arrayListOf()) : RecyclerView.Adapter<OpeningAdapter.OpeningViewHolder>() {

    fun contains(opening: Opening): Boolean {
        return openings.contains(opening)
    }

    operator fun plusAssign(opening: Opening) {
        openings += opening
        notifyDataSetChanged()
    }

    fun deleteOpening(opening: Opening) {
        val index = openings.indexOf(opening)
        openings.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpeningViewHolder {
        return OpeningViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.opening_card, parent, false))
    }

    override fun onBindViewHolder(holder: OpeningViewHolder, position: Int) {
        val opening = openings[position]
        holder.openingTeamView.setBackgroundColor(opening.team.color)
        holder.cardButton.setText(opening.name)
        holder.cardButton.setTextSize(20.0f)
        holder.cardButton.setTextPadding(8, 0, 0 , 0)
        holder.cardButton.textAlignment = TEXT_ALIGNMENT_VIEW_START
        holder.cardButton.setColor(Color.argb(0.05f, 1.0f, 1.0f, 1.0f))
        holder.cardButton.setOnClickListener {
            onOpeningClicked(opening.name, opening.team)
        }
        holder.cardButton.setOnLongClickListener {
            if (holder.deleteOpeningButton.visibility == View.VISIBLE) {
                holder.deleteOpeningButton.visibility = View.GONE
            } else {
                holder.deleteOpeningButton.visibility = View.VISIBLE
            }
            true
        }

        holder.deleteOpeningButton
            .setIcon(R.drawable.delete_icon)
            .setColor(Color.RED)
            .setOnClickListener {
                onDeleteOpening(opening)
            }
    }

    override fun getItemCount() = openings.size

    inner class OpeningViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val openingCard: CardView = view.findViewById(R.id.opening_card)
        val cardButton: UIButton2 = view.findViewById(R.id.opening_card_button)
        val openingTeamView: View = view.findViewById(R.id.opening_team)
        val deleteOpeningButton: UIButton2 = view.findViewById(R.id.delete_opening_button)
    }

}