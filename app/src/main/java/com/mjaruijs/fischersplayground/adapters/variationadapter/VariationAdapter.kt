package com.mjaruijs.fischersplayground.adapters.variationadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.userinterface.UIButton2
import com.mjaruijs.fischersplayground.util.Logger

class VariationAdapter(private val onVariationClicked: (String) -> Unit, private val onVariationSelected: (String, Boolean) -> Unit, private val variations: ArrayList<Variation> = arrayListOf()) : RecyclerView.Adapter<VariationAdapter.VariationViewHolder>() {

    fun contains(variation: Variation): Boolean {
        return variations.contains(variation)
    }

    operator fun plusAssign(variation: Variation) {
        variations += variation
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariationViewHolder {
        return VariationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.variation_layout, parent, false))
    }

    override fun onBindViewHolder(holder: VariationViewHolder, position: Int) {
        val variation = variations[position]
        holder.variationButton.setText(variation.name)
        holder.variationButton.setTextSize(20f)
        holder.variationButton.setOnClickListener {
            onVariationClicked(variation.name)
        }

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onVariationSelected(variation.name, isChecked)
        }
    }

    override fun getItemCount() = variations.size

    inner class VariationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val variationButton: UIButton2 = view.findViewById(R.id.variation_button)
        val checkBox: CheckBox = view.findViewById(R.id.variation_checkbox)
    }

    companion object {
        private const val TAG = "VariationAdapter"
    }

}