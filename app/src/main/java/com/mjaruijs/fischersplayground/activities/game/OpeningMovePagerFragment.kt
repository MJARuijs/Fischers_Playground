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

class OpeningMovePagerFragment(private val openingLines: ArrayList<OpeningLine> = ArrayList()) : Fragment() {

    private lateinit var pager: ViewPager2
    private lateinit var tabIndicator: TabLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.opening_move_pager_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pager = view.findViewById(R.id.move_pager)
        tabIndicator = view.findViewById(R.id.tab_indicator)

        val pagerAdapter = ScreenSlidePagerAdapter(this)
        pager.adapter = pagerAdapter

        for ((i, line) in openingLines.withIndex()) {
            pagerAdapter.addFragment(OpeningMovesFragment2(line.moves))
        }

        if (openingLines.size < 2) {
            tabIndicator.visibility = View.INVISIBLE
        }

        TabLayoutMediator(tabIndicator, pager) {
            tab, position ->
        }.attach()
    }

    fun addLine(line: OpeningLine) {
        openingLines += line

        if (openingLines.size >= 2) {
            tabIndicator.visibility = View.VISIBLE
        }
    }

    inner class ScreenSlidePagerAdapter(val fragment: OpeningMovePagerFragment, private val openingLines: ArrayList<Fragment> = ArrayList()) : FragmentStateAdapter(fragment) {

        override fun getItemCount() = openingLines.size

        override fun createFragment(position: Int) = openingLines[position]

        fun addFragment(fragment: Fragment) {
            openingLines += fragment
        }

    }

}