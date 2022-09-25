package com.mjaruijs.fischersplayground.adapters.crashreportadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.mjaruijs.fischersplayground.R
import com.mjaruijs.fischersplayground.util.FileManager

class CrashReportAdapter(private val onClick: (String) -> Unit, val fileNames: ArrayList<CrashReport>) : RecyclerView.Adapter<CrashReportAdapter.CrashReportViewHolder>() {

    fun remove(fileName: String) {
        val index = fileNames.indexOfFirst { crashReport -> crashReport.fileName == fileName }
        if (index == -1) {
            println("Can't remove $fileName because it doesn't exist..")
            return
        }

        fileNames.removeAt(index)
        notifyItemRemoved(index)
    }

    fun remove(crashReport: CrashReport) {
        val index = fileNames.indexOf(crashReport)
        if (index == -1) {
            println("Can't remove ${crashReport.fileName} because it doesn't exist..")
            return
        }

        fileNames.removeAt(index)
        notifyItemRemoved(index)
    }

    fun clear() {
        fileNames.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrashReportViewHolder {
        return CrashReportViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.crash_report_item, parent, false))
    }

    override fun onBindViewHolder(holder: CrashReportViewHolder, position: Int) {
        val crashReport = fileNames[position]
        holder.fileNameField.text = crashReport.fileName
        holder.crashReportCard.setOnClickListener {
            onClick(holder.fileNameField.text.toString())
        }
        holder.deleteButton.setOnClickListener {
            FileManager.delete(crashReport.fileName)
            fileNames.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount() = fileNames.size

    inner class CrashReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val crashReportCard: CardView = view.findViewById(R.id.crash_report_card)
        val fileNameField: TextView = view.findViewById(R.id.file_name_field)
        val deleteButton: Button = view.findViewById(R.id.delete_crash_button)
    }

}