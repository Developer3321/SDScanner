package com.app.sdscanner.Adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.app.sdscanner.R

import java.io.File
import java.text.DecimalFormat
import java.util.ArrayList

class ScanDetailsAdapter(internal var files: ArrayList<File>) : RecyclerView.Adapter<ScanDetailsAdapter.MyViewHolder>() {
    internal var form: DecimalFormat

    init {
        form = DecimalFormat("0.00")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.scan_details_row, parent, false)

        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bindView(files[position])
    }

    override fun getItemCount(): Int {
        return files.size
    }

    public inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var file_name: TextView
        var file_size: TextView

        init {
            file_name = itemView.findViewById(R.id.name)
            file_size = itemView.findViewById(R.id.file_size)
        }

        fun bindView(file: File) {
            file_name.text = file.name
            file_size.text = form.format(file.length() / 1048576.0) + " MB"
        }
    }
}
