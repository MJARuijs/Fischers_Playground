package com.mjaruijs.fischersplayground.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.mjaruijs.fischersplayground.R

class TestClientActivity : ClientActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_test_activity)

        findViewById<Button>(R.id.test_button).setOnClickListener {
            Toast.makeText(applicationContext, "Click!", Toast.LENGTH_SHORT).show()
        }
    }


}