package com.app.sdscanner.Activity

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.widget.FrameLayout

import com.app.sdscanner.Fragment.ScanDetailsFragment
import com.app.sdscanner.Fragment.ScanFragment
import com.app.sdscanner.R
import com.app.sdscanner.Service.ScanMyService
import com.app.sdscanner.Utils.Constants

class HomeActivity : AppCompatActivity() {

    internal lateinit var container: FrameLayout
    internal lateinit var scanFragment: ScanFragment
    lateinit var scanDetailsFragment: ScanDetailsFragment

    internal var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scanFragment.onBroadcastReceive(intent, context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        container = findViewById(R.id.container)
        addFragment(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            supportFragmentManager.putFragment(outState, "ScanFragment", scanFragment)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            supportFragmentManager.putFragment(outState, "ScanDetailsFragment", scanDetailsFragment)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun addFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            scanFragment = ScanFragment()
            supportFragmentManager.beginTransaction().add(R.id.container, scanFragment, "ScanFragment").addToBackStack("ScanFragment").commitAllowingStateLoss()
        } else {
            scanFragment = supportFragmentManager.getFragment(savedInstanceState, "ScanFragment") as ScanFragment
            if(supportFragmentManager.getFragment(savedInstanceState, "ScanDetailsFragment")!=null)
            scanDetailsFragment = supportFragmentManager.getFragment(savedInstanceState, "ScanDetailsFragment") as ScanDetailsFragment
        }
    }

    public override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter(Constants.BROADCAST_FILTER))
    }

    public override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onBackPressed() {
        if(supportFragmentManager.backStackEntryCount>1)
        {
            supportFragmentManager.popBackStack();
        }else {
            val intentService = Intent(this, ScanMyService::class.java)
            stopService(intentService)
            finish()
        }
    }
}
