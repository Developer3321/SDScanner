package com.app.sdscanner.Service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import android.support.v4.content.LocalBroadcastManager

import com.app.sdscanner.Activity.HomeActivity
import com.app.sdscanner.Model.FileExtension
import com.app.sdscanner.R
import com.app.sdscanner.Utils.Constants

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

import android.os.Process.THREAD_PRIORITY_BACKGROUND

class ScanMyService : Service() {

    internal var total_size: Double = 0.toDouble()
    internal var total_files: Long = 0
    internal lateinit var thread: Thread
    private var alreadyRunning = true
    internal lateinit var fileArrayList: ArrayList<File>
    internal lateinit var fileExtensions: HashMap<String, Int>


    private var mServiceHandler: ServiceHandler? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            thread = object : Thread() {
                override fun run() {
                    super.run()
                    letScan()
                }
            }
            thread.start()
        }
    }

    override fun onCreate() {

        val thread = HandlerThread("ReadMyFileArguments",
                THREAD_PRIORITY_BACKGROUND)
        thread.start()
        var mServiceLooper: Looper? = null
        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val msg = mServiceHandler!!.obtainMessage()
        msg.arg1 = startId
        mServiceHandler!!.sendMessage(msg)
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        alreadyRunning = false
        stopSelf()
    }

    protected fun letScan() {

        val fileName = Environment.getExternalStorageDirectory().toString()
        total_size = 0.0
        total_files = 0
        fileArrayList = ArrayList()
        fileExtensions = HashMap()
        getFiles(fileName)
        Collections.sort(fileArrayList) { o1, o2 -> if (o1.length() > o2.length()) -1 else if (o1.length() < o2.length()) 1 else 0 }
        val stringArrayList = ArrayList<String>()
        var size = if (fileArrayList.size > 10) 10 else fileArrayList.size
        for (i in 0 until size) {
            stringArrayList.add(fileArrayList[i].absolutePath)
        }
        val iterator = fileExtensions.keys.iterator()
        val fileExtensionArrayList = ArrayList<FileExtension>()
        while (iterator.hasNext()) {
            val fileExtension = FileExtension()
            fileExtension.exe = iterator.next()
            fileExtension.fileOccurrence = fileExtensions.get(fileExtension.exe)!!
            fileExtensionArrayList.add(fileExtension)
        }
        Collections.sort(fileExtensionArrayList) { o1, o2 -> if (o1.fileOccurrence > o2.fileOccurrence) -1 else if (o1.fileOccurrence < o2.fileOccurrence) 1 else 0 }
        val jsonObject = JSONObject()
        size = if (fileExtensionArrayList.size > 5) 5 else fileExtensionArrayList.size
        for (i in 0 until size) {
            val fileExtension = fileExtensionArrayList[i]
            try {
                jsonObject.put(fileExtension.exe, fileExtension.fileOccurrence)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        sendMesage(null, total_files, total_size, stringArrayList, jsonObject.toString())
        stopSelf()
    }

    fun getFiles(DirectoryPath: String) {
        if (alreadyRunning) {
            val f = File(DirectoryPath)
            val files = f.listFiles()
            f.mkdirs()
            if (files.size > 0 && alreadyRunning) {
                for (file in files) {
                    if (file.isDirectory) {
                        getFiles(file.absolutePath)
                    } else {
                        total_files++
                        total_size += (file.length() / 1048576.0f).toDouble()
                        fileArrayList.add(file)
                        val fileName_ = file.name
                        var exe = ""
                        try {
                            exe = fileName_.substring(fileName_.lastIndexOf(".") + 1).toLowerCase()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        if (fileExtensions.containsKey(exe)) {
                            fileExtensions[exe] = fileExtensions[exe]!!.plus(1)
                        } else {
                            fileExtensions[exe] = 1
                        }

                        if (alreadyRunning)
                            sendMesage(file.absolutePath, total_files, total_size, null, null)
                    }

                }
            }
        }
    }

    fun sendMesage(fileName: String?, total_files: Long, total_size: Double, strings: ArrayList<String>?, fileExe: String?) {
        val intent = Intent(Constants.BROADCAST_FILTER)
        if (fileName != null) {
            intent.putExtra(Constants.FILENAME, fileName)
            sendNotification(total_files, true)
        } else {
            intent.putExtra(Constants.FINISHED, strings)
            intent.putExtra(Constants.FILEEXE, fileExe)
            sendNotification(total_files, false)
        }
        intent.putExtra(Constants.TOTAL_FILES, total_files)
        intent.putExtra(Constants.TOTAL_FILE_SIZE, total_size)
        intent.putExtra(Constants.STOPPED, alreadyRunning)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    fun sendNotification(total_files: Long, b: Boolean) {

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (b) {
            val mBuilder = NotificationCompat.Builder(applicationContext, "file_notification").setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Scanning in progress").setContentText(total_files.toString() + " Files Scanned").setTicker(resources.getString(R.string.app_name)).setVisibility(1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC)
            }
            val resultIntent = Intent(applicationContext, HomeActivity::class.java)
            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addNextIntent(resultIntent)
            val resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            mBuilder.setContentIntent(resultPendingIntent)
            mNotificationManager.notify(100, mBuilder.build())
        } else {
            mNotificationManager.cancel(100)
        }
    }


}
