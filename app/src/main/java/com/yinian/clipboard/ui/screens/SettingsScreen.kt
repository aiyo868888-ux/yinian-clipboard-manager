package com.yinian.clipboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import com.yinian.clipboard.floatingwindow.FloatingWindowManager
import com.yinian.clipboard.floatingwindow.requestFloatingWindowPermission

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val floatingWindowManager = remember { FloatingWindowManager(context) }
    var hasPermission by remember { mutableStateOf(floatingWindowManager.hasPermission()) }
    var isShowing by remember { mutableStateOf(floatingWindowManager.isShowing()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 悬浮窗权限设置
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "悬浮窗设置",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Divider()

                    // 权限状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("悬浮窗权限")
                        Text(
                            text = if (hasPermission) "已授予" else "未授予",
                            color = if (hasPermission) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    // 请求权限按钮
                    if (!hasPermission) {
                        Button(
                            onClick = {
                                (context as? AppCompatActivity)?.requestFloatingWindowPermission { granted ->
                                    hasPermission = granted
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("请求权限")
                        }
                    }

                    Divider()

                    // 显示/隐藏悬浮窗
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("悬浮窗显示")
                        Switch(
                            checked = isShowing,
                            onCheckedChange = {
                                if (hasPermission) {
                                    floatingWindowManager.toggleFloatingWindow()
                                    isShowing = floatingWindowManager.isShowing()
                                }
                            },
                            enabled = hasPermission
                        )
                    }
                }
            }

            // 其他设置（占位）
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "其他设置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "更多设置即将推出...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
