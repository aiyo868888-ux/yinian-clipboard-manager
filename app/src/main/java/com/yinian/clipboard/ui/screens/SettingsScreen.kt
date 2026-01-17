package com.yinian.clipboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yinian.clipboard.R
import com.yinian.clipboard.accessibility.isAccessibilityServiceEnabled
import com.yinian.clipboard.accessibility.openAccessibilitySettings
import com.yinian.clipboard.floatingwindow.FloatingWindowManager

/**
 * 设置界面 - 简化版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val floatingWindowManager = remember { FloatingWindowManager(context) }

    // 悬浮窗权限状态
    var hasFloatingPermission by remember { mutableStateOf(floatingWindowManager.hasPermission()) }
    var isFloatingShowing by remember { mutableStateOf(floatingWindowManager.isShowing()) }

    // 辅助功能权限状态
    var accessibilityGranted by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var showAccessibilityHelp by remember { mutableStateOf(false) }

    // 刷新所有状态
    val refreshAllState = {
        hasFloatingPermission = floatingWindowManager.hasPermission()
        isFloatingShowing = floatingWindowManager.isShowing()
        accessibilityGranted = isAccessibilityServiceEnabled(context)
    }

    // 监听生命周期，每次页面显示时刷新状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAllState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            // 辅助功能权限（核心）
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (accessibilityGranted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (accessibilityGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (accessibilityGranted) Color.Green else Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.accessibility_permission_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        text = if (accessibilityGranted)
                            stringResource(R.string.accessibility_permission_granted)
                        else
                            stringResource(R.string.accessibility_permission_not_granted),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!accessibilityGranted) {
                        Button(
                            onClick = { openAccessibilitySettings(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.accessibility_permission_button))
                        }

                        TextButton(
                            onClick = { showAccessibilityHelp = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.accessibility_permission_help))
                        }
                    } else {
                        TextButton(
                            onClick = { refreshAllState() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("刷新状态")
                        }
                    }
                }
            }

            // 悬浮窗权限（核心）
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("悬浮窗设置", style = MaterialTheme.typography.titleMedium)
                    Divider()

                    // 权限状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("悬浮窗权限")
                        Text(
                            text = if (hasFloatingPermission) "已授予" else "未授予",
                            color = if (hasFloatingPermission) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    if (!hasFloatingPermission) {
                        Button(
                            onClick = {
                                context.startActivity(floatingWindowManager.requestPermission())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("请求权限")
                        }
                    }

                    Divider()

                    // 悬浮窗开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("显示悬浮窗")
                        Switch(
                            checked = isFloatingShowing,
                            onCheckedChange = {
                                if (hasFloatingPermission) {
                                    floatingWindowManager.toggleFloatingWindow()
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        isFloatingShowing = floatingWindowManager.isShowing()
                                    }, 500)
                                }
                            },
                            enabled = hasFloatingPermission
                        )
                    }

                    // 提示信息
                    if (!hasFloatingPermission) {
                        Text(
                            text = "⚠️ 请先授予悬浮窗权限",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (hasFloatingPermission && !accessibilityGranted) {
                        Text(
                            text = "⚠️ 请先授权辅助功能",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // 刷新按钮
                    if (hasFloatingPermission) {
                        TextButton(
                            onClick = { refreshAllState() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("刷新状态")
                        }
                    }
                }
            }

            // 使用说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用方法", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 复制要保存的文本\n2. 点击悬浮窗即可保存\n3. 不影响当前输入法",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // 辅助功能帮助对话框
    if (showAccessibilityHelp) {
        AlertDialog(
            onDismissRequest = { showAccessibilityHelp = false },
            title = { Text(stringResource(R.string.accessibility_permission_help_title)) },
            text = { Text(stringResource(R.string.accessibility_permission_help_text)) },
            confirmButton = {
                TextButton(onClick = { showAccessibilityHelp = false }) {
                    Text("我明白了")
                }
            }
        )
    }
}
