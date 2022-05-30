package com.mjaruijs.fischersplayground.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.mjaruijs.fischersplayground.R

class GraphicSettingsDialog : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {



        return inflater.inflate(R.layout.activity_settings, null)
    }

}