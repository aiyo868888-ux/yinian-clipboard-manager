package com.yinian.clipboard.repository

import com.yinian.clipboard.data.ClipboardDao
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.ClipboardType
import com.yinian.clipboard.data.TagDao
import com.yinian.clipboard.data.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 剪贴板仓库，统一管理数据访问
 */
@Singleton
class ClipboardRepository @Inject constructor(
    private val clipboardDao: ClipboardDao,
    private val tagDao: TagDao
) {

    // ===== Clipboard 相关操作 =====

    /**
     * 插入剪贴板记录
     */
    suspend fun insertClipboard(entity: ClipboardEntity): Long {
        return clipboardDao.insert(entity)
    }

    /**
     * 更新剪贴板记录
     */
    suspend fun updateClipboard(entity: ClipboardEntity) {
        clipboardDao.update(entity)
    }

    /**
     * 删除剪贴板记录
     */
    suspend fun deleteClipboard(entity: ClipboardEntity) {
        clipboardDao.delete(entity)
    }

    /**
     * 根据ID删除剪贴板
     */
    suspend fun deleteClipboardById(id: Long) {
        clipboardDao.deleteById(id)
    }

    /**
     * 获取所有剪贴板记录（按创建时间倒序）
     */
    fun getAllClipboards(): Flow<List<ClipboardEntity>> {
        return clipboardDao.getAllByCreatedAt()
    }

    /**
     * 获取收藏的剪贴板
     */
    fun getFavorites(): Flow<List<ClipboardEntity>> {
        return clipboardDao.getFavorites()
    }

    /**
     * 按类型筛选剪贴板
     */
    fun getClipboardsByType(type: ClipboardType): Flow<List<ClipboardEntity>> {
        return clipboardDao.getByType(type)
    }

    /**
     * 搜索剪贴板文本内容
     */
    fun searchClipboards(query: String): Flow<List<ClipboardEntity>> {
        return clipboardDao.searchByText(query)
    }

    /**
     * 更新收藏状态
     */
    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        clipboardDao.updateFavorite(id, isFavorite)
    }

    /**
     * 获取剪贴板总数
     */
    suspend fun getClipboardCount(): Int {
        return clipboardDao.getCount()
    }

    /**
     * 删除旧记录（保留最近的N条）
     */
    suspend fun deleteOldClipboards(limit: Int): Int {
        return clipboardDao.deleteOldRecords(limit)
    }

    // ===== Tag 相关操作 =====

    /**
     * 创建标签
     */
    suspend fun createTag(name: String, color: String): Result<Long> {
        return try {
            // 检查标签名是否已存在
            if (tagDao.exists(name)) {
                Result.failure(Exception("标签名已存在"))
            } else {
                val tag = TagEntity(name = name, color = color)
                val id = tagDao.insert(tag)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新标签
     */
    suspend fun updateTag(tag: TagEntity): Result<Unit> {
        return try {
            tagDao.update(tag)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除标签
     */
    suspend fun deleteTag(tag: TagEntity) {
        tagDao.delete(tag)
    }

    /**
     * 根据ID删除标签
     */
    suspend fun deleteTagById(id: Long) {
        tagDao.deleteById(id)
    }

    /**
     * 获取所有标签
     */
    fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao.getAll()
    }

    /**
     * 搜索标签
     */
    fun searchTags(query: String): Flow<List<TagEntity>> {
        return tagDao.searchByName(query)
    }

    /**
     * 获取标签总数
     */
    suspend fun getTagCount(): Int {
        return tagDao.getCount()
    }

    /**
     * 为剪贴板添加标签
     */
    suspend fun addTagToClipboard(clipboardId: Long, tagId: Long) {
        val relation = com.yinian.clipboard.data.ClipboardTagEntity(clipboardId, tagId)
        tagDao.addTagToClipboard(relation)
    }

    /**
     * 从剪贴板移除标签
     */
    suspend fun removeTagFromClipboard(clipboardId: Long, tagId: Long) {
        tagDao.removeTagFromClipboard(clipboardId, tagId)
    }

    /**
     * 移除剪贴板的所有标签
     */
    suspend fun removeAllTagsFromClipboard(clipboardId: Long) {
        tagDao.removeAllTagsFromClipboard(clipboardId)
    }

    /**
     * 获取剪贴板的所有标签
     */
    fun getTagsForClipboard(clipboardId: Long): Flow<List<TagEntity>> {
        return tagDao.getTagsForClipboard(clipboardId)
    }

    /**
     * 获取使用该标签的剪贴板数量
     */
    suspend fun getTagUsageCount(tagId: Long): Int {
        return tagDao.getUsageCount(tagId)
    }

    /**
     * 根据标签筛选剪贴板（通过子查询实现）
     */
    fun getClipboardsByTag(tagId: Long): Flow<List<ClipboardEntity>> {
        // 注意：这需要Room的@Transaction支持，这里简化处理
        // 实际实现可以通过创建新的DAO方法或使用RawQuery
        return getAllClipboards() // 临时返回全部，后续在ViewModel中过滤
    }
}
