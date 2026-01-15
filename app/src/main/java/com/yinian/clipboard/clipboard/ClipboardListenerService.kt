package com.yinian.clipboard.clipboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.ClipboardType
import com.yinian.clipboard.repository.ClipboardRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * 剪贴板监听前台服务
 */
@AndroidEntryPoint
class ClipboardListenerService : Service() {

    @Inject
    lateinit var clipboardMonitor: SystemClipboardMonitor

    @Inject
    lateinit var repository: ClipboardRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val CHANNEL_ID = "clipboard_listener_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // 开始监听剪贴板
        clipboardMonitor.watchClipboard()
            .onEach { data ->
                handleNewClipboardData(data)
            }
            .launchIn(serviceScope)
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
     * 处理新的剪贴板数据
     */
    private fun handleNewClipboardData(data: ClipboardData) {
        serviceScope.launch {
            try {
                // 1. 检查是否与最新记录重复
                val latest = withContext(Dispatchers.IO) {
                    repository.getAllClipboards().first().firstOrNull()
                }

                // 判断是否重复（内容相同且时间间隔小于2秒）
                val isDuplicate = latest?.let { entity ->
                    val isSameContent = when (data.type) {
                        ClipboardDataType.TEXT -> entity.textContent == data.textContent
                        ClipboardDataType.HTML -> entity.textContent == data.textContent
                        ClipboardDataType.IMAGE -> entity.imageUri == data.imageUri?.toString()
                    }
                    val timeDiff = System.currentTimeMillis() - entity.createdAt
                    isSameContent && timeDiff < 2000
                } ?: false

                if (!isDuplicate) {
                    // 2. 转换为ClipboardEntity
                    val entity = ClipboardEntity(
                        type = when (data.type) {
                            ClipboardDataType.TEXT -> ClipboardType.TEXT
                            ClipboardDataType.HTML -> ClipboardType.HTML
                            ClipboardDataType.IMAGE -> ClipboardType.IMAGE
                        },
                        textContent = data.textContent,
                        imageUri = data.imageUri?.toString()
                    )

                    // 3. 插入数据库
                    withContext(Dispatchers.IO) {
                        repository.insertClipboard(entity)
                    }

                    Timber.d("保存剪贴板记录: ${data.textContent?.take(50)}...")
                } else {
                    Timber.d("跳过重复剪贴板记录")
                }
            } catch (e: Exception) {
                Timber.e(e, "保存剪贴板数据失败")
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
                "剪贴板监听",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "监听剪贴板变化"
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
            .setContentTitle("剪贴板监听中")
            .setContentText("正在监听剪贴板变化")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
