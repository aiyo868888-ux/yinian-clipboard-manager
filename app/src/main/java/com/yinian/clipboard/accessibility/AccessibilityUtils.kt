package com.yinian.clipboard.accessibility

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings

/**
 * 检查辅助功能服务是否已启用
 */
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = ComponentName(
        context,
        ClipboardAccessibilityService::class.java
    )

    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val enabledServicesList = enabledServices.split(":")
    for (enabledService in enabledServicesList) {
        val enabledComponentName = ComponentName.unflattenFromString(enabledService)
        if (enabledComponentName != null && enabledComponentName == expectedComponentName) {
            return true
        }
    }

    return false
}

/**
 * 打开辅助功能设置页面
 */
fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
