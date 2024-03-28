package com.example.ermes

import android.app.Application
import com.sunmi.authorizelibrary.SunmiAuthorizeSDK

class ApplicationClass : Application() {

    override fun onCreate() {
        super.onCreate()

        //Print log
        SunmiAuthorizeSDK.setDebuggable(true)
        //Initialization
        SunmiAuthorizeSDK.init(this)

    }
}