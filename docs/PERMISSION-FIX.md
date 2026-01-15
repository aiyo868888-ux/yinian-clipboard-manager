# 🔐 权限检查修复指南

## 问题清单

1. ❌ Android 13+ 缺少 POST_NOTIFICATIONS 权限检查
2. ❌ 悬浮窗启动前未检查 SYSTEM_ALERT_WINDOW 权限

---

## 修复 1: 通知权限（Android 13+）

### 当前代码
`app/src/main/java/com/yinian/clipboard/ui/MainActivity.kt:51-58`
```kotlin
private fun startClipboardListenerService() {
    val intent = Intent(this, ClipboardListenerService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)  // ❌ 未检查权限
    } else {
        startService(intent)
    }
}
```

### 修复代码

```kotlin
package com.yinian.clipboard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // 通知权限请求 Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动服务
            startClipboardListenerService()
        } else {
            // 权限被拒绝，显示提示
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        val intent = Intent(this, ClipboardListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * 显示权限被拒绝对话框
     */
    private fun showPermissionDeniedDialog() {
        // 使用 AlertDialog 或 Compose Dialog 提示用户
        // 可以引导用户到设置页面手动开启权限
    }
}
```

### Compose UI 版本（推荐）

如果使用 Compose，可以这样处理：

```kotlin
@Composable
fun PermissionRequestScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            showRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )) {
                PackageManager.PERMISSION_GRANTED -> onPermissionGranted()
                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            onPermissionGranted()
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("需要通知权限") },
            text = { Text("剪贴板监听服务需要通知权限才能在后台运行。请在设置中开启。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 打开应用设置页面
                        val intent = Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text("取消")
                }
            }
        )
    }
}
```

---

## 修复 2: 悬浮窗权限检查

### 当前代码
`app/src/main/java/com/yinian/clipboard/floatingwindow/FloatingWindowService.kt:49-54`
```kotlin
ACTION_SHOW -> {
    if (!isRunning) {
        startForeground(NOTIFICATION_ID, createNotification())
        showFloatingWindow()  // ❌ 未检查权限
        isRunning = true
    }
}
```

### 修复代码

```kotlin
ACTION_SHOW -> {
    if (!isRunning) {
        // 检查悬浮窗权限
        if (FloatingWindowManager(applicationContext).hasPermission()) {
            startForeground(NOTIFICATION_ID, createNotification())
            showFloatingWindow()
            isRunning = true
        } else {
            // 权限未授予，发送广播通知 Activity
            val intent = Intent(ACTION_PERMISSION_REQUIRED)
            sendBroadcast(intent)
            stopSelf()  // 停止服务
        }
    }
}
```

### MainActivity 中处理权限请求

```kotlin
class MainActivity : ComponentActivity() {

    private val floatingWindowPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 检查权限是否授予
        if (FloatingWindowManager(this).hasPermission()) {
            // 权限已授予，可以启动悬浮窗服务
            Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册悬浮窗权限广播接收器
        registerFloatingWindowPermissionReceiver()

        // ...
    }

    private fun registerFloatingWindowPermissionReceiver() {
        val filter = IntentFilter(FloatingWindowService.ACTION_PERMISSION_REQUIRED)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    FloatingWindowService.ACTION_PERMISSION_REQUIRED -> {
                        // 启动悬浮窗权限请求
                        requestFloatingWindowPermission()
                    }
                }
            }
        }, filter)
    }

    private fun requestFloatingWindowPermission() {
        val manager = FloatingWindowManager(this)
        val intent = manager.requestPermission()
        floatingWindowPermissionLauncher.launch(intent)
    }
}
```

---

## 📋 完整的权限检查流程

```kotlin
// 1. 在 AndroidManifest.xml 中声明（已完成）
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

// 2. 在 Application 启动时检查
class ClipboardApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 检查关键权限
        checkCriticalPermissions()
    }

    private fun checkCriticalPermissions() {
        val missingPermissions = mutableListOf<String>()

        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missingPermissions.add("POST_NOTIFICATIONS")
            }
        }

        // 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            missingPermissions.add("SYSTEM_ALERT_WINDOW")
        }

        if (missingPermissions.isNotEmpty()) {
            Timber.w("缺少权限: ${missingPermissions.joinToString()}")
        }
    }
}
```

---

## 🧪 测试清单

### Android 13+ 设备测试
- [ ] 首次启动是否请求通知权限
- [ ] 拒绝权限后是否显示提示对话框
- [ ] 授予权限后服务是否正常启动
- [ ] 在设置中关闭权限后应用是否提示

### 悬浮窗权限测试
- [ ] 首次开启悬浮窗是否请求权限
- [ ] 拒绝权限后是否不显示悬浮窗
- [ ] 授予权限后悬浮窗是否正常显示
- [ ] 在设置中关闭权限后悬浮窗是否消失

### 权限状态持久化测试
- [ ] 应用重启后权限状态是否保持
- [ ] 应用升级后权限是否保留

---

## 🎯 修复时间估计

- **通知权限修复**: 30分钟
- **悬浮窗权限修复**: 30分钟
- **测试验证**: 30分钟
- **总计**: 约1.5小时

---

## 📝 注意事项

1. **Android 13+** 必须在运行时请求 `POST_NOTIFICATIONS` 权限
2. **SYSTEM_ALERT_WINDOW** 是特殊权限，需要引导用户到系统设置页面
3. 使用 `registerForActivityResult` 而不是 `startActivityForResult`
4. 权限被拒绝后，应该友好提示用户并说明权限用途

---

## 🔄 相关文件

需要修改的文件：
- [ ] `MainActivity.kt` - 添加权限请求逻辑
- [ ] `FloatingWindowService.kt` - 添加权限检查
- [ ] `ClipboardApplication.kt` - 添加权限状态检查
- [ ] `AndroidManifest.xml` - 确保权限声明正确（已完成）

---

> ✅ 修复后，应用在 Android 13+ 设备上将不会因缺少权限而崩溃！
