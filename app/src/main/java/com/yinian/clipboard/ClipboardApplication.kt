package com.yinian.clipboard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application类，初始化Hilt
 */
@HiltAndroidApp
class ClipboardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化操作
    }
}
