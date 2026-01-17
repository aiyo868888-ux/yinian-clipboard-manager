package com.yinian.clipboard.accessibility

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.yinian.clipboard.clipboard.ClipboardData
import com.yinian.clipboard.clipboard.ClipboardDataType
import com.yinian.clipboard.clipboard.ClipboardListenerService
import timber.log.Timber

/**
 * 剪贴板辅助功能服务
 *
 * 功能说明：
 * - 在后台持续监听剪贴板变化
 * - 绕过 Android 10+ 的前台剪贴板访问限制
 * - 不需要用户切换输入法
 * - 完全不影响用户当前操作
 *
 * 隐私承诺：
 * - 只在剪贴板内容变化时读取
 * - 不读取屏幕其他内容
 * - 数据仅存储在本地
 */
class ClipboardAccessibilityService : AccessibilityService() {

    private val clipboardManager: ClipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    private var lastClipboardContent: String? = null

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    private val triggerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.yinian.clipboard.TRIGGER_READ") {
                Timber.i("📡 收到触发广播，主动读取剪贴板")
                // 强制读取最新内容，不管是否相同
                readCurrentClipboard(forceUpdate = true)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.i("✅ 辅助功能服务已启动")

        try {
            // 注册剪贴板监听器
            clipboardManager.addPrimaryClipChangedListener(clipboardListener)
            Timber.i("✅ 剪贴板监听器已注册")

            // 注册广播接收器（接收悬浮窗的触发信号）
            val filter = IntentFilter("com.yinian.clipboard.TRIGGER_READ")
            registerReceiver(triggerReceiver, filter)
            Timber.i("✅ 广播接收器已注册")

            // 读取当前剪贴板内容作为初始值
            readCurrentClipboard()

            // 【关键】启动前台服务确保持续运行
            startClipboardListenerService()
        } catch (e: Exception) {
            Timber.e(e, "❌ 辅助功能服务启动失败")
        }
    }

    /**
     * 启动剪贴板监听服务（前台服务，防止被杀）
     */
    private fun startClipboardListenerService() {
        try {
            val intent = android.content.Intent(this, com.yinian.clipboard.clipboard.ClipboardListenerService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Timber.i("✅ 已启动 ClipboardListenerService 前台服务")
        } catch (e: Exception) {
            Timber.e(e, "❌ 启动前台服务失败")
        }
    }

    private fun readCurrentClipboard(forceUpdate: Boolean = false) {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrEmpty()) {
                    // 【关键修复】强制更新总是更新缓存，不管是否相同
                    lastClipboardContent = text

                    val clipboardData = ClipboardData(
                        type = ClipboardDataType.TEXT,
                        textContent = text
                    )
                    ClipboardListenerService.setLatestClipboardData(clipboardData)

                    val tag = if (forceUpdate) "强制读取" else "初始读取"
                    Timber.i("📋 ${tag}剪贴板内容: ${text.take(30)}...")
                    Timber.i("✅ 缓存已更新[$tag]")
                }
            } else {
                Timber.w("⚠️ 剪贴板为空")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 读取剪贴板失败")
        }
    }

    private fun handleClipboardChange() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData == null || clipData.itemCount == 0) {
                Timber.w("⚠️ 剪贴板为空")
                return
            }

            val text = clipData.getItemAt(0).text?.toString()
            if (text.isNullOrEmpty()) {
                Timber.w("⚠️ 剪贴板内容为空")
                return
            }

            // 移除去重逻辑 - 每次剪贴板变化都更新缓存
            // 用户可能重复复制相同内容，每次都应该能保存
            lastClipboardContent = text

            // 更新到 ClipboardListenerService 的缓存
            val clipboardData = ClipboardData(
                type = ClipboardDataType.TEXT,
                textContent = text
            )
            ClipboardListenerService.setLatestClipboardData(clipboardData)

            Timber.i("📋 剪贴板已更新: ${text.take(30)}...")
        } catch (e: Exception) {
            Timber.e(e, "❌ 处理剪贴板变化失败")
        }
    }

    override fun onDestroy() {
        try {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
            unregisterReceiver(triggerReceiver)
            Timber.i("🛑 辅助功能服务已停止")
        } catch (e: Exception) {
            Timber.e(e, "❌ 移除监听器失败")
        }
        super.onDestroy()
    }

    override fun onInterrupt() {
        Timber.w("⚠️ 辅助功能服务被中断")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理辅助功能事件
    }
}
