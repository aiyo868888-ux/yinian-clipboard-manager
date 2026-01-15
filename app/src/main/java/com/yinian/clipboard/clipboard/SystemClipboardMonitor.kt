package com.yinian.clipboard.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统剪贴板监听器，监听Android系统剪贴板变化
 * 注意：避免与android.content.ClipboardManager命名冲突
 */
@Singleton
class SystemClipboardMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val systemClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /**
     * 监听剪贴板变化，返回Flow
     */
    fun watchClipboard(): Flow<ClipboardData> = callbackFlow {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            val clipData = systemClipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val data = parseClipData(item)
                if (data != null) {
                    trySend(data)
                }
            }
        }

        systemClipboardManager.addPrimaryClipChangedListener(listener)

        awaitClose {
            systemClipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    /**
     * 获取当前剪贴板内容
     */
    fun getCurrentClip(): ClipboardData? {
        val clipData = systemClipboardManager.primaryClip ?: return null
        if (clipData.itemCount == 0) return null

        val item = clipData.getItemAt(0)
        return parseClipData(item)
    }

    /**
     * 复制内容到剪贴板
     */
    fun copyToClipboard(data: ClipboardData) {
        val clip = when (data.type) {
            ClipboardDataType.TEXT -> ClipData.newPlainText("clipboard", data.textContent)
            ClipboardDataType.HTML -> ClipData.newHtmlText("clipboard", data.textContent ?: "", data.htmlContent)
            ClipboardDataType.IMAGE -> {
                // 图片处理需要Uri，这里简化处理
                ClipData.newPlainText("clipboard", data.textContent)
            }
        }
        systemClipboardManager.setPrimaryClip(clip)
    }

    /**
     * 解析ClipData.Item
     */
    private fun parseClipData(item: ClipData.Item): ClipboardData? {
        // 尝试获取文本
        val text = item.text?.toString()
        val htmlText = item.htmlText

        // 尝试获取Uri（图片）
        val uri = item.uri

        return when {
            text != null && htmlText != null -> ClipboardData(
                type = ClipboardDataType.HTML,
                textContent = text,
                htmlContent = htmlText
            )
            text != null -> ClipboardData(
                type = ClipboardDataType.TEXT,
                textContent = text
            )
            uri != null -> ClipboardData(
                type = ClipboardDataType.IMAGE,
                imageUri = uri
            )
            else -> null
        }
    }
}

/**
 * 剪贴板数据
 */
data class ClipboardData(
    val type: ClipboardDataType,
    val textContent: String? = null,
    val htmlContent: String? = null,
    val imageUri: Uri? = null
)

enum class ClipboardDataType {
    TEXT,
    HTML,
    IMAGE
}
