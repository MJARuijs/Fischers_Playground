package com.mjaruijs.fischersplayground

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.mjaruijs.fischersplayground.databinding.TestFragmentBinding
import kotlinx.coroutines.delay

private const val BANNER_ANIMATION_DELAY = 1000L
private const val BANNER_ANIMATION_DURATION = 800L

class TestFragment : Fragment(R.layout.test_fragment) {

    companion object {
        fun newInstance() = TestFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val binding = TestFragmentBinding.bind(view)
//
        val animateBannerUpConstraintSet = ConstraintSet().apply {
            clone(view.findViewById<ConstraintLayout>(R.id.constraintLayout))
            connect(
                R.id.animatedBanner,
                ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP
            )
            clear(R.id.animatedBanner, ConstraintSet.TOP)
        }

        view.findViewById<Button>(R.id.button2).setOnClickListener {
//            delay(BANNER_ANIMATION_DELAY)
            val transition = AutoTransition().apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = BANNER_ANIMATION_DURATION
            }

            TransitionManager.beginDelayedTransition(view.findViewById<ConstraintLayout>(R.id.constraintLayout), transition)
            animateBannerUpConstraintSet.applyTo(view.findViewById<ConstraintLayout>(R.id.constraintLayout))
        }

    }
}