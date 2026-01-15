package com.yinian.clipboard.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.time.Instant

/**
 * 标签实体
 */
@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], name = "idx_tag_name", unique = true),
        Index(value = ["color"], name = "idx_color"),
        Index(value = ["created_at"], name = "idx_tag_created_at")
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color")
    val color: String, // 颜色代码，如 "#FF5722"

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 剪贴板-标签关联表
 */
@Entity(
    tableName = "clipboard_tags",
    primaryKeys = ["clipboard_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ClipboardEntity::class,
            parentColumns = ["id"],
            childColumns = ["clipboard_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clipboard_id"], name = "idx_clipboard_id"),
        Index(value = ["tag_id"], name = "idx_tag_id")
    ]
)
data class ClipboardTagEntity(
    @ColumnInfo(name = "clipboard_id")
    val clipboardId: Long,

    @ColumnInfo(name = "tag_id")
    val tagId: Long
)
