package com.yinian.clipboard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 标签数据访问对象
 */
@Dao
interface TagDao {

    /**
     * 插入标签
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    /**
     * 批量插入标签
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>): List<Long>

    /**
     * 更新标签
     */
    @Update
    suspend fun update(tag: TagEntity)

    /**
     * 删除标签
     */
    @Delete
    suspend fun delete(tag: TagEntity)

    /**
     * 根据ID删除标签
     */
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 根据名称删除标签
     */
    @Query("DELETE FROM tags WHERE name = :name")
    suspend fun deleteByName(name: String)

    /**
     * 清空所有标签
     */
    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    /**
     * 根据ID获取标签
     */
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    /**
     * 根据名称获取标签
     */
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    /**
     * 获取所有标签（按创建时间倒序）
     */
    @Query("SELECT * FROM tags ORDER BY created_at DESC")
    fun getAll(): Flow<List<TagEntity>>

    /**
     * 搜索标签（名称模糊匹配）
     */
    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchByName(query: String): Flow<List<TagEntity>>

    /**
     * 获取标签总数
     */
    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getCount(): Int

    /**
     * 检查标签名是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM tags WHERE name = :name")
    suspend fun exists(name: String): Boolean

    /**
     * 为剪贴板添加标签
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToClipboard(clipboardTag: ClipboardTagEntity)

    /**
     * 批量为剪贴板添加标签
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagsToClipboard(clipboardTags: List<ClipboardTagEntity>)

    /**
     * 从剪贴板移除标签
     */
    @Query("DELETE FROM clipboard_tags WHERE clipboard_id = :clipboardId AND tag_id = :tagId")
    suspend fun removeTagFromClipboard(clipboardId: Long, tagId: Long)

    /**
     * 移除剪贴板的所有标签
     */
    @Query("DELETE FROM clipboard_tags WHERE clipboard_id = :clipboardId")
    suspend fun removeAllTagsFromClipboard(clipboardId: Long)

    /**
     * 获取剪贴板的所有标签
     */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN clipboard_tags ct ON t.id = ct.tag_id
        WHERE ct.clipboard_id = :clipboardId
        ORDER BY t.created_at DESC
    """)
    fun getTagsForClipboard(clipboardId: Long): Flow<List<TagEntity>>

    /**
     * 获取使用该标签的剪贴板数量
     */
    @Query("SELECT COUNT(*) FROM clipboard_tags WHERE tag_id = :tagId")
    suspend fun getUsageCount(tagId: Long): Int
}
