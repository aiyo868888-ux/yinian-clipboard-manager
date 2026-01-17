package com.yinian.clipboard.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.TagEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * 剪贴板列表项
 */
@Composable
fun ClipboardListItem(
    clipboard: ClipboardEntity,
    tags: List<TagEntity> = emptyList(),
    onFavoriteClick: (ClipboardEntity) -> Unit,
    onDeleteClick: (ClipboardEntity) -> Unit,
    onItemClick: (ClipboardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick(clipboard) },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 标题行：类型标签 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型标签
                TypeChip(type = clipboard.type.name)

                // 时间
                Text(
                    text = formatTimestamp(clipboard.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 内容预览
            clipboard.textContent?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            clipboard.imageUri?.let { uri ->
                Text(
                    text = "📷 图片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 标签显示
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(tag = tag)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 操作按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // 收藏按钮
                IconButton(onClick = { onFavoriteClick(clipboard) }) {
                    Icon(
                        imageVector = if (clipboard.isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = "收藏",
                        tint = if (clipboard.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // 删除按钮
                IconButton(onClick = { onDeleteClick(clipboard) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 类型标签Chip
 */
@Composable
private fun TypeChip(type: String) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = type,
                style = MaterialTheme.typography.labelSmall
            )
        }
    )
}

/**
 * 标签Chip
 */
@Composable
private fun TagChip(tag: TagEntity) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(tag.color))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tagColor.copy(alpha = 0.15f),
        border = null
    ) {
        Text(
            text = tag.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tagColor
        )
    }
}

/**
 * 格式化时间戳
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        diff < 604800_000 -> "${diff / 86400_000}天前"
        else -> {
            val dateFormat = DateFormat.getMediumDateFormat(null)
            val date = Date(timestamp)
            dateFormat.format(date)
        }
    }
}

/**
 * 剪贴板列表
 */
@Composable
fun ClipboardList(
    clipboards: List<com.yinian.clipboard.data.ClipboardEntity>,
    clipboardTags: Map<Long, List<com.yinian.clipboard.data.TagEntity>> = emptyMap(),
    onFavoriteClick: (com.yinian.clipboard.data.ClipboardEntity) -> Unit,
    onDeleteClick: (com.yinian.clipboard.data.ClipboardEntity) -> Unit,
    onItemClick: (com.yinian.clipboard.data.ClipboardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (clipboards.isEmpty()) {
        EmptyState(
            message = "暂无剪贴板记录",
            modifier = modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(clipboards) { clipboard ->
                val tags = clipboardTags[clipboard.id] ?: emptyList()
                ClipboardListItem(
                    clipboard = clipboard,
                    tags = tags,
                    onFavoriteClick = onFavoriteClick,
                    onDeleteClick = onDeleteClick,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

/**
 * 空状态提示
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
