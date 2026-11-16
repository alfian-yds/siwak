package com.mosque.prayerclock.server

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings

/**
 * Mencatat apakah ada layar SIWAK yang sedang tampil (F-TV-18).
 * Hanya SIWAK sendiri yang dipantau; aplikasi lain di TV tidak pernah dicatat.
 */
object AppForegroundTracker : Application.ActivityLifecycleCallbacks {
    @Volatile private var started = 0

    /** Waktu (epoch ms) keadaan tampil/latar belakang terakhir berubah. */
    @Volatile var since: Long = System.currentTimeMillis()
        private set

    val isForeground: Boolean get() = started > 0

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(this)
    }

    /** Android 10+ melarang membuka layar dari latar belakang tanpa izin "Tampil di atas aplikasi lain". */
    fun canOpenFromBackground(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)

    override fun onActivityStarted(activity: Activity) {
        if (started++ == 0) since = System.currentTimeMillis()
    }

    override fun onActivityStopped(activity: Activity) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0) since = System.currentTimeMillis()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
