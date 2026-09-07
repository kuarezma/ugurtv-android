package com.ugur.iptv

import android.app.Application
import android.util.Log

class UgurTvApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Global crash guard to prevent unexpected hard crashes on Android TV
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("UgurTvApp", "Global uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
