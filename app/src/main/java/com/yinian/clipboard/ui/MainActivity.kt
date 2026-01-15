package com.yinian.clipboard.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import com.yinian.clipboard.clipboard.ClipboardListenerService
import com.yinian.clipboard.export.DataExportServer
import com.yinian.clipboard.floatingwindow.FloatingWindowManager
import com.yinian.clipboard.floatingwindow.requestFloatingWindowPermission
import com.yinian.clipboard.ui.screens.ClipboardMainScreen
import com.yinian.clipboard.ui.screens.ExportScreen
import com.yinian.clipboard.ui.screens.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主Activity
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var exportServer: DataExportServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动剪贴板监听服务
        startClipboardListenerService()

        setContent {
            MaterialTheme {
                MainNavigation(exportServer)
            }
        }
    }

    /**
     * 启动剪贴板监听服务
     */
    private fun startClipboardListenerService() {
        val intent = Intent(this, ClipboardListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
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
                                    0 -> androidx.compose.material.icons.Icons.Default.Home
                                    1 -> androidx.compose.material.icons.Icons.Default.Share
                                    else -> androidx.compose.material.icons.Icons.Default.Settings
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

