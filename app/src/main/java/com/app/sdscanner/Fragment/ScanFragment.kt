package com.app.sdscanner.Fragment

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import com.app.sdscanner.Activity.HomeActivity
import com.app.sdscanner.R
import com.app.sdscanner.Service.ScanMyService
import com.app.sdscanner.Utils.Constants
import com.app.sdscanner.Utils.Util
import com.github.lzyzsd.circleprogress.ArcProgress

import org.json.JSONArray

import java.util.ArrayList

class ScanFragment : Fragment(), View.OnClickListener {

    internal lateinit var arc_progress: ArcProgress
    internal lateinit var filenames: TextView
    internal lateinit var startstop: Button
    internal lateinit var report: Button

    internal var fileNamesStringArrayList: ArrayList<String>? = null

    internal var fileName: String? = null

    internal var scanFinished = false
    internal var totalFileSize: Double = 0.toDouble()
    internal var totalFiles: Long = 0
    internal var sizeStorageUsed: Long = 0

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(Constants.FILENAME, fileName)
        outState.putLong(Constants.TOTAL_FILES, totalFiles)
        outState.putBoolean("scanfinish", scanFinished)
        outState.putDouble(Constants.TOTAL_FILE_SIZE, totalFileSize)
        outState.putStringArrayList(Constants.FINISHED, fileNamesStringArrayList)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.FILENAME))
                fileName = savedInstanceState.getString(Constants.FILENAME)

            if (savedInstanceState.containsKey(Constants.TOTAL_FILES))
                totalFiles = savedInstanceState.getLong(Constants.TOTAL_FILES, 0)

            if (savedInstanceState.containsKey("scanfinish")) {
                scanFinished = savedInstanceState.getBoolean("scanfinish")
                if (scanFinished)
                    arc_progress.progress = 100
                else {
                    arc_progress.progress = (totalFileSize / sizeStorageUsed * 100).toInt()
                }
            }
            if (savedInstanceState.containsKey(Constants.TOTAL_FILE_SIZE))
                totalFileSize = savedInstanceState.getDouble(Constants.TOTAL_FILE_SIZE, 0.0)
            if (savedInstanceState.containsKey(Constants.FINISHED))
                fileNamesStringArrayList = savedInstanceState.getStringArrayList(Constants.FINISHED)

            showButtons()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
    }

    private fun init(view: View) {
        arc_progress = view.findViewById(R.id.arc_progress)
        arc_progress.unfinishedStrokeColor = resources.getColor(R.color.colorAccent)
        arc_progress.finishedStrokeColor = resources.getColor(R.color.colorPrimaryDark)
        arc_progress.textColor = resources.getColor(R.color.colorPrimaryDark)
        arc_progress.bottomText = "Scan Files"
        filenames = view.findViewById(R.id.filenames)
        startstop = view.findViewById(R.id.startstop)
        report = view.findViewById(R.id.report)
        startstop.setOnClickListener(this)
        report.setOnClickListener(this)

        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val total_size: Long
        val available_size: Long
        val usedSize: Long
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            total_size = stat.blockCountLong * stat.blockSizeLong
            available_size = stat.availableBlocksLong * stat.blockSizeLong
            usedSize = total_size - available_size
        } else {
            total_size = stat.blockCount.toLong() * stat.blockSize.toLong()
            available_size = stat.availableBlocks.toLong() * stat.blockSize.toLong()
            usedSize = total_size - available_size
        }


        sizeStorageUsed = usedSize / 1048576
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.startstop -> {
                scanFinished = false
                if (startstop.text.toString().equals(resources.getString(R.string.scan), ignoreCase = true)) {

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                    100)
                        } else {
                            sdScan()
                        }
                    } else {
                        sdScan()
                    }

                } else {
                    val intentService = Intent(activity, ScanMyService::class.java)
                    activity!!.stopService(intentService)
                    val mNotificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mNotificationManager.cancel(100)
                    startstop.text = resources.getString(R.string.scan)
                }
                showButtons()
            }
            R.id.report -> {
                (activity as HomeActivity).scanDetailsFragment = ScanDetailsFragment()
                activity!!.supportFragmentManager.beginTransaction().add(R.id.container, (activity as HomeActivity).scanDetailsFragment, "ScanDetailsFragment").addToBackStack("ScanDetailsFragment").commitAllowingStateLoss()
            }
        }
    }

    private fun sdScan() {
        val intentService = Intent(activity, ScanMyService::class.java)
        activity!!.startService(intentService)
        startstop.text = resources.getString(R.string.stop)
        showButtons()
    }

    fun onBroadcastReceive(intent: Intent, context: Context) {
        totalFiles = intent.getLongExtra(Constants.TOTAL_FILES, 0)
        totalFileSize = intent.getDoubleExtra(Constants.TOTAL_FILE_SIZE, 0.0)

        if (intent.hasExtra(Constants.FILENAME)) {
            fileName = intent.getStringExtra(Constants.FILENAME)
            filenames.text = "Scanning File : " + fileName!!
            val per = (totalFileSize / sizeStorageUsed * 100).toInt()
            arc_progress.progress = per
        } else if (intent.hasExtra(Constants.FINISHED)) {
            scanFinished = true
            arc_progress.progress = 100
            fileNamesStringArrayList = intent.getStringArrayListExtra(Constants.FINISHED)
            val jsonArray = JSONArray(fileNamesStringArrayList)

            val editor = Util.getPref(context).edit()
            editor.putString(resources.getString(R.string.pref_names), jsonArray.toString())
            editor.putLong(resources.getString(R.string.pref_total_files), totalFiles)
            editor.putFloat(resources.getString(R.string.pref_total_files_size), totalFileSize.toFloat())
            editor.putString(resources.getString(R.string.pref_file_exe), intent.getStringExtra(Constants.FILEEXE))
            editor.apply()
            startstop.text = getContext()!!.resources.getString(R.string.scan)

            if (intent.getBooleanExtra(Constants.STOPPED, true))
                Toast.makeText(getContext(), resources.getString(R.string.complete_scan), Toast.LENGTH_SHORT).show()
        }
        showButtons()
    }

    private fun showButtons() {
        if (scanFinished) {
            report.visibility = View.VISIBLE
        } else {
            report.visibility = View.INVISIBLE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            var granted = true
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                granted = false
            }
            if (granted) {
                sdScan()
            } else {
                Toast.makeText(context, resources.getString(R.string.no_permission), Toast.LENGTH_SHORT).show()
            }
        }
    }

}
