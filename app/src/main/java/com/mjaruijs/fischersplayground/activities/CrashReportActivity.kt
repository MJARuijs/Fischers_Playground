package com.mjaruijs.fischersplayground.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.adapters.crashreportadapter.CrashReport
import com.mjaruijs.fischersplayground.adapters.crashreportadapter.CrashReportAdapter
import com.mjaruijs.fischersplayground.dialogs.CrashReportDialog
import com.mjaruijs.fischersplayground.util.FileManager
import java.io.File

class CrashReportActivity : AppCompatActivity() {

    private lateinit var crashReportRecycler: RecyclerView
    private lateinit var crashReportAdapter: CrashReportAdapter

    private lateinit var crashDialog: CrashReportDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_report)

        crashDialog = CrashReportDialog(applicationContext)
        crashDialog.create(this)
        crashDialog.setLayout()

        val fileList = FileManager.listFilesInDirectory()

        val crashReports = ArrayList<CrashReport>()
        for (path in fileList) {
            crashReports += CrashReport(path)
        }

        crashReportAdapter = CrashReportAdapter(::onClick, crashReports)
        crashReportRecycler = findViewById(R.id.crash_report_recycler)
        crashReportRecycler.layoutManager = LinearLayoutManager(applicationContext)
        crashReportRecycler.adapter = crashReportAdapter

        for (filePath in fileList) {
            if (!FileManager.doesFileExist(filePath)) {
                crashReportAdapter.remove(filePath)
            } else {
                if (FileManager.isFileEmpty(filePath)) {
                    crashReportAdapter.remove(filePath)
                }
            }
        }

        crashReportAdapter.notifyDataSetChanged()

        val deleteReportsButton = findViewById<Button>(R.id.delete_reports_button)
        deleteReportsButton.setOnClickListener {
            for (filePath in crashReportAdapter.fileNames) {
                FileManager.delete(filePath.fileName)
            }
            crashReportAdapter.clear()
        }
    }

    private fun onClick(fileName: String) {
        val file = File("${applicationContext.filesDir.absoluteFile}/$fileName")
        if (file.exists()) {
            val text = file.readText()
            crashDialog.setContent(text)
            crashDialog.show()
        } else {
            Toast.makeText(applicationContext, "${applicationContext.filesDir.absoluteFile}/$fileName doesn't exist..", Toast.LENGTH_SHORT).show()
        }
    }

    fun destroy() {
        crashDialog.dismiss()
    }
}