package com.mjaruijs.fischersplayground.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.openingadapter.OpeningLine
import com.mjaruijs.fischersplayground.chess.game.Move
import com.mjaruijs.fischersplayground.chess.game.MoveArrow
import com.mjaruijs.fischersplayground.util.Logger

class OpeningMovePagerFragment : Fragment() {

    private lateinit var onLineSelected: (OpeningLine, Int) -> Unit
    private lateinit var onLineCleared: () -> Unit
    private var onMoveClick: (Move) -> Unit = { _ -> }
    private val openingLines = ArrayList<OpeningLine>()

    private lateinit var pager: ViewPager2
    private lateinit var tabIndicator: TabLayout

    private lateinit var pagerAdapter: ScreenSlidePagerAdapter

    companion object {

        private const val TAG = "OpeningMovePagerFragment"

        fun getInstance(onLineSelected: (OpeningLine, Int) -> Unit, onLineCleared: () -> Unit, onMoveClick: (Move) -> Unit, openingLines: ArrayList<OpeningLine> = ArrayList()): OpeningMovePagerFragment {
            val pagerFragment = OpeningMovePagerFragment()
            pagerFragment.onLineSelected = onLineSelected
            pagerFragment.onLineCleared = onLineCleared
            pagerFragment.onMoveClick = onMoveClick

            for (line in openingLines) {
                pagerFragment.openingLines += line
            }

            return pagerFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Logger.debug(TAG, "onCreateView()")
        return inflater.inflate(R.layout.opening_move_pager_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.debug(TAG, "onViewCreated() start")

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
        tabMediator.attach()

        val tabStrip = tabIndicator.getChildAt(0) as LinearLayout
        for (i in 0 until tabStrip.childCount) {
            tabStrip.getChildAt(i).setOnTouchListener { _, _ -> true }
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                onPageChanged(position)
            }
        })

        Logger.debug(TAG, "onViewCreated() end")
    }

    private fun onPageChanged(position: Int) {
        val selectedFragment = pagerAdapter.get(position)

        val line = selectedFragment.getOpeningLine()
        val selectedMove = selectedFragment.currentMoveIndex
        onLineSelected(line, selectedMove)
    }

    fun getCurrentOpeningFragment() = pagerAdapter.get(pager.currentItem)

    fun addLine(setupMoves: ArrayList<Move> = arrayListOf(), lineMoves: ArrayList<Move> = arrayListOf(), arrows: HashMap<Int, ArrayList<MoveArrow>>) {
        pagerAdapter.add(pager.currentItem + 1, setupMoves, lineMoves, arrows)

        if (pagerAdapter.itemCount >= 2) {
            tabIndicator.visibility = View.VISIBLE
        }

        val tabStrip = tabIndicator.getChildAt(0) as LinearLayout
        tabStrip.getChildAt(pagerAdapter.itemCount - 1).setOnTouchListener { _, _ -> true }
        pager.setCurrentItem(pager.currentItem + 1, true)
    }

    fun getFragments() = pagerAdapter.getFragments()

    fun deleteCurrentLine() {
        if (pagerAdapter.itemCount == 1) {
            pagerAdapter.get(0).clear()
            onLineCleared()
        } else {
            pagerAdapter.delete(pager.currentItem)
            onPageChanged(pager.currentItem)
        }

        if (pagerAdapter.itemCount < 2) {
            tabIndicator.visibility = View.INVISIBLE
        }
    }

    inner class ScreenSlidePagerAdapter(private val onMoveClick: (Move) -> Unit, val fragment: OpeningMovePagerFragment, private val fragments: ArrayList<OpeningMovesFragment> = ArrayList()) : FragmentStateAdapter(fragment) {

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

        override fun getItemId(position: Int): Long {
            return fragments[position].hashCode().toLong()
        }

        fun isEmpty() = fragments.isEmpty()

        fun get(i: Int) = fragments[i]

        fun add(openingLine: OpeningLine? = null) {
            fragments += if (openingLine != null) {
                OpeningMovesFragment.getInstance(onMoveClick, openingLine.setupMoves, openingLine.lineMoves, openingLine.arrows)
            } else {
                OpeningMovesFragment.getInstance(onMoveClick)
            }
            notifyDataSetChanged()
        }

        fun add(index: Int, setupMoves: ArrayList<Move>, lineMoves: ArrayList<Move>, arrows: HashMap<Int, ArrayList<MoveArrow>>) {
            fragments.add(index, OpeningMovesFragment.getInstance(onMoveClick, setupMoves, lineMoves, arrows))
            notifyDataSetChanged()
        }

        fun delete(i: Int) {
            fragments.removeAt(i)
            notifyItemRemoved(i)
        }

        fun getFragments() = fragments
    }

}