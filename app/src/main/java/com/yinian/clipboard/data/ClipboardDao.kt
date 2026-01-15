package com.yinian.clipboard.data

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 剪贴板数据访问对象
 */
@Dao
interface ClipboardDao {

    /**
     * 插入剪贴板记录
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ClipboardEntity): Long

    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ClipboardEntity>): List<Long>

    /**
     * 更新记录
     */
    @Update
    suspend fun update(entity: ClipboardEntity)

    /**
     * 删除记录
     */
    @Delete
    suspend fun delete(entity: ClipboardEntity)

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 清空所有记录
     */
    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    /**
     * 根据ID获取记录
     */
    @Query("SELECT * FROM clipboard_items WHERE id = :id")
    suspend fun getById(id: Long): ClipboardEntity?

    /**
     * 获取最新的一条记录
     */
    @Query("SELECT * FROM clipboard_items ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatest(): ClipboardEntity?

    /**
     * 获取所有记录（按创建时间倒序）
     */
    @Query("SELECT * FROM clipboard_items ORDER BY created_at DESC")
    fun getAllByCreatedAt(): Flow<List<ClipboardEntity>>

    /**
     * 分页查询所有记录
     */
    @Query("SELECT * FROM clipboard_items ORDER BY created_at DESC")
    fun pagingSource(): PagingSource<Int, ClipboardEntity>

    /**
     * 获取收藏的记录
     */
    @Query("SELECT * FROM clipboard_items WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavorites(): Flow<List<ClipboardEntity>>

    /**
     * 按类型筛选
     */
    @Query("SELECT * FROM clipboard_items WHERE type = :type ORDER BY created_at DESC")
    fun getByType(type: ClipboardType): Flow<List<ClipboardEntity>>

    /**
     * 搜索文本内容
     */
    @Query("""
        SELECT * FROM clipboard_items
        WHERE text_content LIKE '%' || :query || '%'
        ORDER BY created_at DESC
    """)
    fun searchByText(query: String): Flow<List<ClipboardEntity>>

    /**
     * 更新收藏状态
     */
    @Query("UPDATE clipboard_items SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getCount(): Int

    /**
     * 删除旧记录（保留最近的N条）
     */
    @Query("""
        DELETE FROM clipboard_items
        WHERE id NOT IN (
            SELECT id FROM clipboard_items
            ORDER BY created_at DESC
            LIMIT :limit
        )
    """)
    suspend fun deleteOldRecords(limit: Int): Int
}
