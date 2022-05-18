package com.mjaruijs.fischersplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.*
import android.widget.Button
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class TestActivity : AppCompatActivity() {

    var set = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val baseConstraint = ConstraintSet().apply {
            clone(findViewById<ConstraintLayout>(R.id.main_layout))
        }

        val newConstraint = ConstraintSet().apply {
            clone(applicationContext, R.layout.activity_test_alt)
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            val transition = AutoTransition().apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = 1000L
            }

            TransitionManager.beginDelayedTransition(findViewById<ConstraintLayout>(R.id.main_layout), transition)
            if (set) {
                baseConstraint.applyTo(findViewById(R.id.main_layout))
            } else {
                newConstraint.applyTo(findViewById<ConstraintLayout>(R.id.main_layout))
            }
            set = !set
        }
    }

}