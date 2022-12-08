package com.mjaruijs.fischersplayground.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.mjaruijs.fischersplayground.R

class CrashReportDialog(private val context: Context) {

    private lateinit var dialog: Dialog
    private lateinit var textView: TextView
    private lateinit var button: Button

    private var copiedText = ""

    fun create(context: Activity) {
        dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_crash_report)
    }

    fun setLayout() {
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    fun setContent(content: String) {
        textView = dialog.findViewById(R.id.crash_content)
        textView.text = content

        button = dialog.findViewById(R.id.copy_button)
        button.setOnClickListener {
            val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("crash_report", content)
            clipBoard.setPrimaryClip(clip)
        }
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}