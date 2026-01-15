package com.yinian.clipboard.floatingwindow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * 悬浮窗服务
 */
@AndroidEntryPoint
class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    companion object {
        const val ACTION_SHOW = "com.yinian.clipboard.ACTION_SHOW"
        const val ACTION_HIDE = "com.yinian.clipboard.ACTION_HIDE"
        const val ACTION_PERMISSION_REQUIRED = "com.yinian.clipboard.ACTION_PERMISSION_REQUIRED"
        private const val CHANNEL_ID = "floating_window_channel"
        private const val NOTIFICATION_ID = 1002
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                if (!isRunning) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    showFloatingWindow()
                    isRunning = true
                }
            }
            ACTION_HIDE -> {
                if (isRunning) {
                    removeFloatingWindow()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    isRunning = false
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

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
            x = 100
            y = 200
        }

        // 设置触摸监听（拖动和点击）
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
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val xDiff = (event.rawX - initialTouchX).toInt()
                        val yDiff = (event.rawY - initialTouchY).toInt()

                        // 如果移动距离小于10像素，视为点击
                        if (xDiff * xDiff + yDiff * yDiff < 100) {
                            onFloatingViewClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)
    }

    /**
     * 移除悬浮窗
     */
    private fun removeFloatingWindow() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
    }

    /**
     * 悬浮窗点击事件
     */
    private fun onFloatingViewClick() {
        Toast.makeText(this, "悬浮窗被点击", Toast.LENGTH_SHORT).show()
        // TODO: 打开主界面或执行其他操作
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
            .setContentTitle("悬浮窗服务")
            .setContentText("点击隐藏悬浮窗")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
