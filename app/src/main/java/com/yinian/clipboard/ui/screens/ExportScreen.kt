package com.yinian.clipboard.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yinian.clipboard.export.DataExportServer
import com.yinian.clipboard.export.QrCodeGenerator
import timber.log.Timber

/**
 * 数据导出界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    exportServer: DataExportServer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isServerRunning by remember { mutableStateOf(exportServer.isRunning()) }
    var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var deviceName by remember { mutableStateOf(android.os.Build.MODEL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "数据导出",
            style = MaterialTheme.typography.headlineMedium
        )

        // 服务器控制卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServerRunning) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "导出服务器",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isServerRunning) "运行中" else "已停止",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isServerRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Switch(
                        checked = isServerRunning,
                        onCheckedChange = { running ->
                            if (running) {
                                val success = exportServer.startServer()
                                isServerRunning = success
                                if (success) {
                                    // 生成二维码
                                    qrCodeBitmap = QrCodeGenerator().generatePairingQrCode(deviceName)
                                }
                            } else {
                                exportServer.stopServer()
                                isServerRunning = false
                                qrCodeBitmap = null
                            }
                        }
                    )
                }

                if (isServerRunning) {
                    Divider()

                    Text(
                        text = "服务器地址：http://localhost:8080",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "在同一Wi-Fi网络下，电脑浏览器访问：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 二维码显示卡片
        if (isServerRunning && qrCodeBitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "扫描二维码配对",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 显示二维码
                    qrCodeBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "二维码",
                            modifier = Modifier.size(250.dp)
                        )
                    }

                    Text(
                        text = "使用手机扫描此二维码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 导出格式选项
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // 打开浏览器
                                openBrowser(context, "http://localhost:8080/api/clipboard/export/csv")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导出CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                // 打开浏览器
                                openBrowser(context, "http://localhost:8080/api/clipboard")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("导出JSON")
                        }
                    }
                }
            }
        }

        // 使用说明
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "使用说明",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "1. 确保手机和电脑在同一Wi-Fi网络\n2. 开启导出服务器\n3. 在电脑浏览器输入服务器地址\n4. 选择导出格式（JSON或CSV）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 打开浏览器
 */
private fun openBrowser(context: android.content.Context, url: String) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(url)
    )
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Timber.e(e, "打开浏览器失败")
    }
}
