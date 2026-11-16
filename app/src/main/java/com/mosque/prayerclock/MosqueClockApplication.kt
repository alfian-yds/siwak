package com.mosque.prayerclock

import android.app.Application
import com.mosque.prayerclock.server.AppForegroundTracker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MosqueClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // F-TV-18: catat apakah SIWAK sedang tampil, untuk dilaporkan ke server.
        AppForegroundTracker.install(this)
    }
}
