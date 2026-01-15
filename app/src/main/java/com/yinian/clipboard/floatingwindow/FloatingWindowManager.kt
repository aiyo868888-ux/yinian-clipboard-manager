package com.yinian.clipboard.floatingwindow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 悬浮窗管理器，处理权限和显示控制
 */
class FloatingWindowManager(private val context: Context) {

    companion object {
        const val FLOATING_WINDOW_REQUEST_CODE = 1001
    }

    /**
     * 检查是否有悬浮窗权限
     */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestPermission(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            // Android 6.0 以下不需要权限
            Intent()
        }
    }

    /**
     * 显示悬浮窗
     */
    fun showFloatingWindow() {
        if (!hasPermission()) {
            throw SecurityException("缺少悬浮窗权限")
        }

        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_SHOW
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * 隐藏悬浮窗
     */
    fun hideFloatingWindow() {
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_HIDE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * 停止悬浮窗服务
     */
    fun stopFloatingWindowService() {
        val intent = Intent(context, FloatingWindowService::class.java)
        context.stopService(intent)
    }

    /**
     * 切换悬浮窗显示状态
     */
    fun toggleFloatingWindow() {
        if (FloatingWindowService.isRunning) {
            hideFloatingWindow()
        } else {
            showFloatingWindow()
        }
    }

    /**
     * 获取悬浮窗状态
     */
    fun isShowing(): Boolean {
        return FloatingWindowService.isRunning
    }
}
