package com.yinian.clipboard.clipboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.ClipboardType
import com.yinian.clipboard.floatingwindow.FloatingWindowService
import com.yinian.clipboard.repository.ClipboardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import javax.inject.Inject

/**
 * 剪贴板数据缓存服务
 *
 * 功能说明：
 * - 提供跨服务的线程安全数据缓存
 * - 接收来自 AccessibilityService 的剪贴板更新
 * - 接收来自 FloatingWindowService 的保存请求
 *
 * 注意：此服务不再主动监听剪贴板，监听功能由 ClipboardAccessibilityService 负责
 */
@AndroidEntryPoint
class ClipboardListenerService : Service() {

    @Inject
    lateinit var repository: ClipboardRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val CHANNEL_ID = "clipboard_listener_channel"
    private val NOTIFICATION_ID = 1001

    companion object {
        // 缓存互斥锁（线程安全保护）
        private val cacheMutex = Mutex()

        // 缓存最新的剪贴板数据（不自动保存）
        private var latestClipboardData: ClipboardData? = null

        /**
         * 获取缓存数据（线程安全）
         */
        @JvmStatic
        fun getLatestClipboardData(): ClipboardData? = latestClipboardData

        /**
         * 更新缓存数据（线程安全）
         * 注意：此方法可能被 AccessibilityService 从后台线程调用
         */
        @JvmStatic
        fun setLatestClipboardData(data: ClipboardData?) {
            // 使用简单的同步，不需要协程（避免阻塞 AccessibilityService）
            synchronized(cacheMutex) {
                latestClipboardData = data
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // 注册保存广播接收器
        registerSaveReceiver()

        Timber.i("✅ 缓存服务已启动（由 AccessibilityService 更新缓存）")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * 注册保存广播接收器 - 接收广播携带的剪贴板内容
     */
    private fun registerSaveReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == FloatingWindowService.ACTION_SAVE_CLIPBOARD) {
                    val text = intent.getStringExtra("text")

                    Timber.i("========================================")
                    Timber.i("📬 收到保存广播")

                    if (text != null) {
                        Timber.i("📋 广播中的剪贴板内容: ${text.take(50)}")

                        // 直接使用广播中的数据创建 ClipboardData
                        val data = ClipboardData(
                            type = ClipboardDataType.TEXT,
                            textContent = text
                        )

                        Timber.i("💾 开始保存...")
                        saveClipboardManually(data)
                    } else {
                        Timber.w("❌ 广播中无剪贴板数据")
                    }

                    Timber.i("========================================")
                }
            }
        }

        val filter = IntentFilter(FloatingWindowService.ACTION_SAVE_CLIPBOARD)
        registerReceiver(receiver, filter)
    }

    /**
     * 手动保存剪贴板 - 极速版（直接保存，不检查重复）
     */
    private fun saveClipboardManually(data: ClipboardData) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                Timber.i("💾 开始保存...")

                // 直接保存，不做重复检查（避免超时）
                val entity = ClipboardEntity(
                    type = when (data.type) {
                        ClipboardDataType.TEXT -> ClipboardType.TEXT
                        ClipboardDataType.HTML -> ClipboardType.HTML
                        ClipboardDataType.IMAGE -> ClipboardType.IMAGE
                    },
                    textContent = data.textContent,
                    imageUri = data.imageUri?.toString()
                )

                repository.insertClipboard(entity)

                Timber.i("✅ 保存成功！")
                Timber.i("========================================")

            } catch (e: Exception) {
                Timber.e(e, "❌ 保存失败")
                Timber.i("========================================")
            }
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "剪贴板缓存",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提供剪贴板数据缓存服务"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     *创建前台服务通知
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
            .setContentText("剪贴板缓存服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
