package com.example.ermes.utils

import android.content.Context
import android.content.SharedPreferences

class Preferences(private val context: Context) {

    private var sharedPref: SharedPreferences = context.getSharedPreferences("SHARED_PREFS", Context.MODE_PRIVATE)

    var isFirstRun: Boolean
        get() = sharedPref.getBoolean("first_run", true)
        set(value) {
            sharedPref.edit().putBoolean("first_run", value).apply()
        }



    var configFilePath:String?
        get() = sharedPref.getString("configFilePath", "")
        set(value) {
            sharedPref.edit().putString("configFilePath", value).apply()
        }


}