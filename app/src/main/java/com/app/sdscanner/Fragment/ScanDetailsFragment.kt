package com.app.sdscanner.Fragment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

import com.app.sdscanner.Adapter.ScanDetailsAdapter
import com.app.sdscanner.R
import com.app.sdscanner.Utils.Util

import org.json.JSONArray
import org.json.JSONObject

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.util.ArrayList
import java.util.Calendar

class ScanDetailsFragment : Fragment() {

    internal lateinit var scrollView: NestedScrollView
    internal lateinit var recyclerView: RecyclerView
    internal lateinit var total_files: TextView
    internal lateinit var total_files_size: TextView
    internal lateinit var file_exe: TextView
    internal lateinit var average: TextView
    internal lateinit var name: TextView
    internal lateinit var file_size: TextView
    internal lateinit var layout: LinearLayout
    internal lateinit var lets_share: Button
    internal lateinit var linearLayoutManager: LinearLayoutManager
    internal lateinit var scanDetailsAdapter: ScanDetailsAdapter
    internal var context: Context? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = activity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
    }

    private fun init(view: View) {
        scrollView = view.findViewById(R.id.scrollView)
        recyclerView = view.findViewById(R.id.recyclerView)
        total_files = view.findViewById(R.id.total_files)
        total_files_size = view.findViewById(R.id.total_files_size)
        file_exe = view.findViewById(R.id.file_exe)
        average = view.findViewById(R.id.average)
        name = view.findViewById(R.id.name)
        file_size = view.findViewById(R.id.file_size)
        layout = view.findViewById(R.id.layout)
        lets_share = view.findViewById(R.id.lets_share)
        lets_share.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            val returnedBitmap = Bitmap.createBitmap(layout.width, layout.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(returnedBitmap)
            val bgDrawable = layout.background
            if (bgDrawable != null)
                bgDrawable.draw(canvas)
            else
                canvas.drawColor(Color.WHITE)
            layout.draw(canvas)

            var bmpUri: Uri? = null
            try {
                val file = File(context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "my_scan_details_" + Calendar.getInstance().timeInMillis + ".png")
                val out = FileOutputStream(file)
                returnedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                out.close()
                bmpUri = FileProvider.getUriForFile(context!!,
                        context!!.packageName + ".core.my.provider", file)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            shareIntent.type = "image/*"
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri)
            shareIntent.putExtra(Intent.EXTRA_TEXT, "SDScanner Report")
            startActivity(Intent.createChooser(shareIntent, "Report"))
        }
        name.text = "File Name"
        file_size.text = "File Size"
        linearLayoutManager = object : LinearLayoutManager(context) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.isNestedScrollingEnabled = false
        val files = ArrayList<File>()
        try {
            val jsonArray = JSONArray(Util.getPref(context!!).getString(resources.getString(R.string.pref_names), "[]"))
            for (i in 0 until jsonArray.length()) {
                val file = File(jsonArray.getString(i))
                files.add(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scanDetailsAdapter = ScanDetailsAdapter(files)
        recyclerView.adapter = scanDetailsAdapter

        val totalFiles = Util.getPref(context!!).getLong(resources.getString(R.string.pref_total_files), 0)
        val totalFilesSize = Util.getPref(context!!).getFloat(resources.getString(R.string.pref_total_files_size), 0f).toDouble()
        try {
            val fileExe = JSONObject(Util.getPref(context!!).getString(resources.getString(R.string.pref_file_exe), "{}"))
            val iterator = fileExe.keys()
            val msg = StringBuilder()
            while (iterator.hasNext()) {
                val exe = iterator.next()
                msg.append(exe + " : " + fileExe.getInt(exe) + "\n")
            }
            file_exe.text = msg.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val form = DecimalFormat("0.00")
        average.text = form.format(totalFilesSize / totalFiles) + " MB"
        total_files.text = totalFiles.toString() + ""
        total_files_size.text = totalFilesSize.toString() + ""
    }

}
