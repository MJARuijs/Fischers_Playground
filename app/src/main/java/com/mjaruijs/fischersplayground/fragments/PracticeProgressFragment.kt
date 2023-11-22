package com.mjaruijs.fischersplayground.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.util.Logger

class PracticeProgressFragment : Fragment() {

    private lateinit var progressBar: ProgressBar

    var maxValue = 0
        private set

    var currentValue = 0
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.practice_progress_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progress_bar)

        progressBar.max = maxValue
        progressBar.setProgress(currentValue, true)
    }

    fun incrementMax(amount: Int) {
        maxValue += amount
        progressBar.max += amount
    }

    fun incrementCurrent() {
        currentValue++
        progressBar.setProgress(currentValue, true)
    }

    fun complete() {
        progressBar.setProgress(maxValue, true)
    }

    companion object {

        private const val TAG = "PracticeProgressFragment"

        fun getInstance(currentValue: Int, maxValue: Int): PracticeProgressFragment {
            val fragment = PracticeProgressFragment()
            fragment.currentValue = currentValue
            fragment.maxValue = maxValue
            return fragment
        }

    }

}