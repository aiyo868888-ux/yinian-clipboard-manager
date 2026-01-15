package com.yinian.clipboard.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 剪贴板内容实体
 */
@Entity(
    tableName = "clipboard_items",
    indices = [
        Index(value = ["created_at"], name = "idx_created_at"),
        Index(value = ["type"], name = "idx_type"),
        Index(value = ["is_favorite"], name = "idx_favorite")
    ]
)
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: ClipboardType,

    @ColumnInfo(name = "text_content", typeAffinity = ColumnInfo.TEXT)
    val textContent: String?,

    @ColumnInfo(name = "image_uri", typeAffinity = ColumnInfo.TEXT)
    val imageUri: String?,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ClipboardType {
    TEXT,
    IMAGE,
    HTML,
    UNKNOWN
}
