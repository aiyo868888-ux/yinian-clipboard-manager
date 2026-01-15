package com.yinian.clipboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.ClipboardType
import com.yinian.clipboard.data.TagEntity
import com.yinian.clipboard.repository.ClipboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI状态
 */
data class MainUiState(
    val clipboards: List<ClipboardEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val filteredClipboards: List<ClipboardEntity> = emptyList(),
    val selectedType: ClipboardType? = null,
    val selectedTagId: Long? = null,
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 主界面ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ClipboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadClipboards()
        loadTags()
    }

    /**
     * 加载剪贴板记录
     */
    private fun loadClipboards() {
        viewModelScope.launch {
            repository.getAllClipboards()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { clipboards ->
                    _uiState.update { currentState ->
                        currentState.copy(clipboards = clipboards)
                    }
                    applyFilters()
                }
        }
    }

    /**
     * 加载标签
     */
    private fun loadTags() {
        viewModelScope.launch {
            repository.getAllTags()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { tags ->
                    _uiState.update { it.copy(tags = tags) }
                }
        }
    }

    /**
     * 应用所有筛选条件
     */
    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.clipboards

        // 收藏筛选
        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        // 类型筛选
        state.selectedType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        // 标签筛选
        state.selectedTagId?.let { tagId ->
            // 需要异步获取标签对应的剪贴板，这里简化处理
            viewModelScope.launch {
                repository.getTagsForClipboard(0).first() // 占位，实际需要遍历检查
            }
        }

        // 搜索筛选
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.textContent?.contains(state.searchQuery, ignoreCase = true) == true
            }
        }

        _uiState.update { it.copy(filteredClipboards = filtered) }
    }

    /**
     * 搜索剪贴板
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    /**
     * 按类型筛选
     */
    fun filterByType(type: ClipboardType?) {
        _uiState.update { it.copy(selectedType = type) }
        applyFilters()
    }

    /**
     * 按标签筛选
     */
    fun filterByTag(tagId: Long?) {
        _uiState.update { it.copy(selectedTagId = tagId) }
        applyFilters()
    }

    /**
     * 切换收藏筛选
     */
    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
        applyFilters()
    }

    /**
     * 清除所有筛选
     */
    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedType = null,
                selectedTagId = null,
                searchQuery = "",
                showFavoritesOnly = false
            )
        }
        applyFilters()
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(clipboard: ClipboardEntity) {
        viewModelScope.launch {
            repository.updateFavorite(clipboard.id, !clipboard.isFavorite)
        }
    }

    /**
     * 删除剪贴板
     */
    fun deleteClipboard(clipboard: ClipboardEntity) {
        viewModelScope.launch {
            repository.deleteClipboard(clipboard)
        }
    }

    /**
     * 创建标签
     */
    fun createTag(name: String, color: String) {
        viewModelScope.launch {
            repository.createTag(name, color)
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }

    /**
     * 删除标签
     */
    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
        }
    }

    /**
     * 为剪贴板添加标签
     */
    fun addTagToClipboard(clipboardId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.addTagToClipboard(clipboardId, tagId)
        }
    }

    /**
     * 从剪贴板移除标签
     */
    fun removeTagFromClipboard(clipboardId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromClipboard(clipboardId, tagId)
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
