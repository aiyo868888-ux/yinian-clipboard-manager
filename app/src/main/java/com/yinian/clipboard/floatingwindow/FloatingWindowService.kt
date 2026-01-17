package com.yinian.clipboard.floatingwindow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.yinian.clipboard.clipboard.ClipboardListenerService
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.ClipboardType
import com.yinian.clipboard.repository.ClipboardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 悬浮窗服务 - 辅助功能版本
 * 功能：显示悬浮窗，点击时保存剪贴板（通过 AccessibilityService 监听）
 */
@AndroidEntryPoint
class FloatingWindowService : Service() {

    @Inject
    lateinit var clipboardRepository: ClipboardRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    companion object {
        private const val DEFAULT_X = 100
        private const val DEFAULT_Y = 200
        private const val CLICK_THRESHOLD = 10f
        const val ACTION_SHOW = "com.yinian.clipboard.ACTION_SHOW"
        const val ACTION_HIDE = "com.yinian.clipboard.ACTION_HIDE"
        const val ACTION_PERMISSION_REQUIRED = "com.yinian.clipboard.ACTION_PERMISSION_REQUIRED"
        const val ACTION_SAVE_CLIPBOARD = "com.yinian.clipboard.ACTION_SAVE_CLIPBOARD"
        private const val CHANNEL_ID = "floating_window_channel"
        private const val NOTIFICATION_ID = 1002
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        Timber.i("✅ 悬浮窗服务已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 【关键修复】立即调用startForeground()避免崩溃
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        when (intent?.action) {
            ACTION_SHOW -> {
                if (!isRunning) {
                    if (FloatingWindowManager(applicationContext).hasPermission()) {
                        // 确保剪贴板监听服务正在运行
                        ensureClipboardListenerRunning()
                        showFloatingWindow()
                        isRunning = true
                    } else {
                        sendBroadcast(Intent(ACTION_PERMISSION_REQUIRED))
                        stopSelf()
                    }
                }
            }
            ACTION_HIDE -> {
                if (isRunning) {
                    removeFloatingWindow()
                    isRunning = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeFloatingWindow()
        isRunning = false
        super.onDestroy()
    }

    /**
     * 显示悬浮窗
     */
    private fun showFloatingWindow() {
        if (floatingView != null) return

        floatingView = LayoutInflater.from(this)
            .inflate(com.yinian.clipboard.R.layout.floating_window_layout, null)

        // 配置悬浮窗参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = DEFAULT_X
            y = DEFAULT_Y
        }

        // 设置触摸事件（拖动）
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // 点击事件：使用绝对值判断移动距离
                        val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                        val deltaY = kotlin.math.abs(event.rawY - initialTouchY)
                        if (deltaX < CLICK_THRESHOLD && deltaY < CLICK_THRESHOLD) {
                            Timber.i("👆 检测到点击事件 (deltaX=$deltaX, deltaY=$deltaY)")
                            try {
                                onFloatingViewClick()
                            } catch (e: Exception) {
                                Timber.e(e, "❌ 点击处理异常")
                                try {
                                    Toast.makeText(this@FloatingWindowService, "❌ 处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                } catch (toastException: Exception) {
                                    Timber.e(toastException, "❌ 无法显示Toast")
                                }
                            }
                        } else {
                            Timber.d("👆 拖动结束，不触发点击 (deltaX=$deltaX, deltaY=$deltaY)")
                        }
                        return true
                    }
                    else -> return false
                }
            }
        })

        windowManager.addView(floatingView, params)
        Timber.i("✅ 悬浮窗已显示")
    }

    /**
     * 移除悬浮窗
     */
    private fun removeFloatingWindow() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
        Timber.i("✅ 悬浮窗已移除")
    }

    /**
     * 悬浮窗点击 - 读取剪贴板并保存
     *
     * 实现：使用反射直接读取剪贴板（绕过后台限制）
     */
    private fun onFloatingViewClick() {
        Timber.i("========================================")
        Timber.i("📱 悬浮窗点击：开始保存剪贴板")

        try {
            var savedText: String? = null

            // 【方案1】优先直接读取剪贴板（前台可靠）
            try {
                Timber.i("📍 [步骤1] 尝试直接读取剪贴板...")
                val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboardManager.primaryClip

                Timber.i("📍 [步骤1.1] clipData是否为空: ${clipData == null}")
                Timber.i("📍 [步骤1.2] clipData数量: ${clipData?.itemCount ?: 0}")

                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString()?.trim()
                    Timber.i("📍 [步骤1.3] 读取到的文本: ${if (text.isNullOrEmpty()) "[空]" else "[有内容]"}")

                    if (!text.isNullOrEmpty()) {
                        savedText = text
                        Timber.i("📋 直接读取剪贴板: ${text.take(30)}...")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 直接读取失败")
            }

            // 【方案2】如果直接读取为空（后台限制），尝试使用输入法服务（优先）
            if (savedText.isNullOrEmpty()) {
                Timber.w("⚠️ 直接读取为空，尝试输入法服务")

                // 输入法服务拥有更高权限，可以后台读取剪贴板
                val imeText = com.yinian.clipboard.ime.ClipboardInputMethodService.readClipboardNow()
                Timber.i("📍 [步骤2.1] 输入法读取结果: ${if (imeText.isNullOrEmpty()) "[空]" else "[有内容]"}")

                if (!imeText.isNullOrEmpty()) {
                    savedText = imeText.trim()
                    Timber.i("📦 使用输入法读取: ${savedText?.take(30)}...")
                } else {
                    // 如果输入法服务也未激活，尝试辅助功能服务
                    Timber.w("⚠️ 输入法服务未激活，尝试辅助功能服务")

                    // 触发辅助功能服务读取（发送3次确保触发）
                    repeat(3) {
                        triggerAccessibilityRead()
                        Thread.sleep(100)
                    }
                    Timber.i("📍 [步骤2.2] 已发送3次触发广播给辅助功能服务")

                    // 等待读取完成
                    Thread.sleep(500)
                    Timber.i("📍 [步骤2.3] 等待500ms后读取缓存")

                    val cachedData = ClipboardListenerService.getLatestClipboardData()
                    Timber.i("📍 [步骤2.4] 缓存数据是否存在: ${cachedData != null}")
                    Timber.i("📍 [步骤2.5] 缓存内容: ${cachedData?.textContent?.take(30) ?: "[空]"}")

                    if (!cachedData?.textContent.isNullOrEmpty()) {
                        savedText = cachedData?.textContent?.trim()
                        Timber.i("📦 使用辅助功能缓存: ${savedText?.take(30)}...")
                    }
                }
            }

            if (savedText.isNullOrEmpty()) {
                Timber.w("⚠️ 剪贴板为空或读取失败")
                showToast("剪贴板为空，请先复制文本")
                Timber.i("========================================")
                return
            }

            Timber.i("✅ 准备保存: ${savedText.take(30)}...")

            // 同步更新缓存（备用）
            val newData = com.yinian.clipboard.clipboard.ClipboardData(
                type = com.yinian.clipboard.clipboard.ClipboardDataType.TEXT,
                textContent = savedText
            )
            ClipboardListenerService.setLatestClipboardData(newData)

            // 保存到数据库
            serviceScope.launch {
                try {
                    val entity = ClipboardEntity(
                        type = ClipboardType.TEXT,
                        textContent = savedText,
                        imageUri = null,
                        isFavorite = false,
                        createdAt = System.currentTimeMillis()
                    )

                    clipboardRepository.insertClipboard(entity)
                    Timber.i("✅ 已保存[监听缓存]: ${savedText.take(30)}...")
                    showToast("已保存")

                } catch (e: Exception) {
                    Timber.e(e, "❌ 保存失败")
                    showToast("保存失败")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ 读取剪贴板失败")
            showToast("读取剪贴板失败")
        }

        Timber.i("========================================")
    }

    /**
     * 显示提示信息
     */
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this@FloatingWindowService, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持悬浮窗显示"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 确保剪贴板监听服务正在运行
     */
    private fun ensureClipboardListenerRunning() {
        try {
            val intent = Intent(this, com.yinian.clipboard.clipboard.ClipboardListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                Timber.i("✅ 已启动 ClipboardListenerService (foreground)")
            } else {
                startService(intent)
                Timber.i("✅ 已启动 ClipboardListenerService (regular)")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ 启动 ClipboardListenerService 失败")
        }
    }

    /**
     * 触发辅助功能服务主动读取剪贴板
     * 使用广播通知辅助功能服务
     */
    private fun triggerAccessibilityRead() {
        try {
            // 发送广播触发辅助功能服务读取剪贴板
            val intent = Intent("com.yinian.clipboard.TRIGGER_READ")
            sendBroadcast(intent)
            Timber.i("📡 已发送触发广播")
        } catch (e: Exception) {
            Timber.e(e, "❌ 发送触发广播失败")
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, com.yinian.clipboard.ui.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("一念剪贴板")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
