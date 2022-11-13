package com.mjaruijs.fischersplayground.activities.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.pieces.Move

class OpeningMovePagerFragment(private val onLineSelected: (OpeningLine, Int) -> Unit, private val onMoveClick: (Move, Boolean) -> Unit, private val openingLines: ArrayList<OpeningLine> = ArrayList()) : Fragment() {

    private lateinit var pager: ViewPager2
    private lateinit var tabIndicator: TabLayout

    private lateinit var pagerAdapter: ScreenSlidePagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_move_pager_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pager = view.findViewById(R.id.move_pager)
        tabIndicator = view.findViewById(R.id.tab_indicator)

        pagerAdapter = ScreenSlidePagerAdapter(onMoveClick, this)
        pager.adapter = pagerAdapter

        for (line in openingLines) {
            pagerAdapter.add(line)
        }

        if (pagerAdapter.isEmpty()) {
            pagerAdapter.add()
        }

        if (pagerAdapter.itemCount < 2) {
            tabIndicator.visibility = View.INVISIBLE

        }

        val tabMediator = TabLayoutMediator(tabIndicator, pager) {
                _, _->
        }
        tabMediator.detach()

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                
                val selectedFragment = pagerAdapter.get(position) as OpeningMovesFragment2
                val line = selectedFragment.getOpeningLine()
                val selectedMove = selectedFragment.currentMoveIndex
                onLineSelected(line, selectedMove)
            }
        })
    }

    fun getCurrentOpeningFragment() = pagerAdapter.get(pager.currentItem) as OpeningMovesFragment2

    fun addLine(setupMoves: ArrayList<Move> = arrayListOf()) {
        pagerAdapter.add(setupMoves)

        if (pagerAdapter.itemCount >= 2) {
            tabIndicator.visibility = View.VISIBLE
        }

        pager.setCurrentItem(pagerAdapter.itemCount - 1, true)
    }

    fun getFragments() = pagerAdapter.getFragments()

    fun deleteCurrentLine() {
        pagerAdapter.delete(pager.currentItem)

        if (pagerAdapter.itemCount < 2) {
            tabIndicator.visibility = View.INVISIBLE
        }
    }

    fun removeLastMove() {
        getCurrentOpeningFragment().removeLastMove()
    }

    inner class ScreenSlidePagerAdapter(val onMoveClick: (Move, Boolean) -> Unit, val fragment: OpeningMovePagerFragment, private val fragments: ArrayList<Fragment> = ArrayList()) : FragmentStateAdapter(fragment) {

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

        fun isEmpty() = fragments.isEmpty()

        fun get(i: Int) = fragments[i]

        fun add(openingLine: OpeningLine? = null) {
            fragments += if (openingLine != null) {
                OpeningMovesFragment2(onMoveClick, openingLine.setupMoves, openingLine.lineMoves)
            } else {
                OpeningMovesFragment2(onMoveClick)
            }
            notifyDataSetChanged()
        }

        fun add(setupMoves: ArrayList<Move>) {
            fragments += OpeningMovesFragment2(onMoveClick, setupMoves)
            notifyDataSetChanged()
        }

        fun delete(i: Int) {
            fragments.removeAt(i)
            notifyItemRemoved(i)
        }

        fun getFragments() = fragments
    }

}