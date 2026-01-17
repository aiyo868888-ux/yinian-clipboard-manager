package com.yinian.clipboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application类，初始化Hilt和Timber
 */
@HiltAndroidApp
class ClipboardApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 【关键修复】初始化Timber日志库
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
