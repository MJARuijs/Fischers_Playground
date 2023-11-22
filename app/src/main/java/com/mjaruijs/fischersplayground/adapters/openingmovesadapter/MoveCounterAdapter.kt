package com.mjaruijs.fischersplayground.adapters.openingmovesadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R

class MoveCounterAdapter(private var numberOfMoves: Int = 0) : RecyclerView.Adapter<MoveCounterAdapter.MoveCounterViewHolder>() {

    fun increment() {
        numberOfMoves++
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveCounterViewHolder {
        return MoveCounterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.opening_move_counter_item, parent, false))
    }

    override fun onBindViewHolder(holder: MoveCounterViewHolder, position: Int) {
        holder.moveCounterText.text = "${position}."
    }

    override fun getItemCount() = numberOfMoves

    inner class MoveCounterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val moveCounterText: TextView = view.findViewById(R.id.move_counter_text)
    }

}