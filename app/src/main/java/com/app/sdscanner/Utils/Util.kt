package com.app.sdscanner.Utils

import android.content.Context
import android.content.SharedPreferences

import com.app.sdscanner.R


object Util {

    fun getPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(
                context.getString(R.string.pref_name), Context.MODE_PRIVATE)
    }
}
