package com.yinian.clipboard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.yinian.clipboard.clipboard.ClipboardListenerService
import com.yinian.clipboard.export.DataExportServer
import com.yinian.clipboard.floatingwindow.FloatingWindowManager
import com.yinian.clipboard.floatingwindow.requestFloatingWindowPermission
import com.yinian.clipboard.ui.screens.ClipboardMainScreen
import com.yinian.clipboard.ui.screens.ExportScreen
import com.yinian.clipboard.ui.screens.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 主Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var exportServer: DataExportServer

    // 通知权限请求 launcher
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startClipboardListenerService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 【测试】输出日志验证新代码
        Log.e("MainActivity", "🔥🔥🔥 MainActivity onCreate - 新版本 🔥🔥🔥")
        Timber.e("🔥🔥🔥 MainActivity onCreate - 新版本 🔥🔥🔥")

        // 先检查并请求通知权限
        checkAndRequestNotificationPermission()

        setContent {
            MaterialTheme {
                MainNavigation(exportServer)
            }
        }
    }

    /**
     * 检查并请求通知权限（Android 13+）
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 请求权限
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // 权限已授予，直接启动服务
                startClipboardListenerService()
            }
        } else {
            // Android 13 以下不需要请求权限
            startClipboardListenerService()
        }
    }

    /**
     * 启动剪贴板监听服务
     */
    private fun startClipboardListenerService() {
        // 启动剪贴板监听服务
        val intent = Intent(this, ClipboardListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // 【关键修复】启动悬浮窗服务
        startFloatingWindowService()
    }

    /**
     * 启动悬浮窗服务
     */
    private fun startFloatingWindowService() {
        val intent = Intent(this, com.yinian.clipboard.floatingwindow.FloatingWindowService::class.java)
        intent.action = com.yinian.clipboard.floatingwindow.FloatingWindowService.ACTION_SHOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Timber.i("✅ 已启动悬浮窗服务")
    }
}

/**
 * 简单的主导航（底部Tab）
 */
@Composable
fun MainNavigation(exportServer: DataExportServer) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("剪贴板", "导出", "设置")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Home
                                    1 -> Icons.Default.Share
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> ClipboardMainScreen(
                modifier = Modifier.padding(padding)
            )
            1 -> ExportScreen(
                exportServer = exportServer,
                modifier = Modifier.padding(padding)
            )
            2 -> SettingsScreen(
                modifier = Modifier.padding(padding)
            )
        }
    }
}

