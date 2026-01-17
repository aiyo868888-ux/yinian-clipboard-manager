package com.yinian.clipboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yinian.clipboard.data.ClipboardEntity
import com.yinian.clipboard.data.ClipboardType
import com.yinian.clipboard.ui.components.ClipboardList
import com.yinian.clipboard.ui.components.EmptyState
import com.yinian.clipboard.ui.viewmodel.MainViewModel

/**
 * 剪贴板主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardMainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedClipboard by remember { mutableStateOf<ClipboardEntity?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showCreateTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("一念剪贴板")
                        Text(
                            "共 ${uiState.filteredClipboards.size} 条",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 触发重新加载 - 通过切换一个状态
                        viewModel.onSearchQueryChange("")
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // 筛选器
            FilterChips(
                selectedType = uiState.selectedType,
                selectedTagId = uiState.selectedTagId,
                showFavoritesOnly = uiState.showFavoritesOnly,
                tags = uiState.tags,
                onTypeClick = { viewModel.filterByType(it) },
                onTagClick = { viewModel.filterByTag(it) },
                onFavoritesClick = { viewModel.toggleFavoritesOnly() },
                onClearFilters = { viewModel.clearFilters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 剪贴板列表
            ClipboardList(
                clipboards = uiState.filteredClipboards,
                clipboardTags = uiState.clipboardTags,
                onFavoriteClick = viewModel::toggleFavorite,
                onDeleteClick = viewModel::deleteClipboard,
                onItemClick = { clipboard ->
                    selectedClipboard = clipboard
                    showTagDialog = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 标签选择对话框
    if (showTagDialog && selectedClipboard != null) {
        val currentTags = uiState.clipboardTags[selectedClipboard!!.id] ?: emptyList()
        val selectedTagIds = currentTags.map { it.id }.toSet()

        TagSelectDialog(
            tags = uiState.tags,
            selectedTags = selectedTagIds,
            onTagToggle = { tagId ->
                if (tagId in selectedTagIds) {
                    viewModel.removeTagFromClipboard(selectedClipboard!!.id, tagId)
                } else {
                    viewModel.addTagToClipboard(selectedClipboard!!.id, tagId)
                }
            },
            onDismiss = { showTagDialog = false },
            onCreateTag = { showCreateTagDialog = true }
        )
    }

    // 创建标签对话框
    if (showCreateTagDialog) {
        CreateTagDialog(
            onDismiss = { showCreateTagDialog = false },
            onConfirm = { name, color ->
                viewModel.createTag(name, color)
                showCreateTagDialog = false
            }
        )
    }
}

/**
 * 搜索栏
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索剪贴板内容...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除"
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

/**
 * 筛选器Chips
 */
@Composable
private fun FilterChips(
    selectedType: ClipboardType?,
    selectedTagId: Long?,
    showFavoritesOnly: Boolean,
    tags: List<com.yinian.clipboard.data.TagEntity>,
    onTypeClick: (ClipboardType?) -> Unit,
    onTagClick: (Long?) -> Unit,
    onFavoritesClick: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 类型筛选
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedType == null,
                onClick = { onTypeClick(null) },
                label = { Text("全部") }
            )
            FilterChip(
                selected = selectedType == ClipboardType.TEXT,
                onClick = { onTypeClick(ClipboardType.TEXT) },
                label = { Text("文本") }
            )
            FilterChip(
                selected = selectedType == ClipboardType.IMAGE,
                onClick = { onTypeClick(ClipboardType.IMAGE) },
                label = { Text("图片") }
            )
        }

        // 标签筛选（如果有标签）
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTagId == null,
                    onClick = { onTagClick(null) },
                    label = { Text("全部标签") }
                )
                tags.take(3).forEach { tag ->
                    val tagColor = try {
                        Color(android.graphics.Color.parseColor(tag.color))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    FilterChip(
                        selected = selectedTagId == tag.id,
                        onClick = { onTagClick(tag.id) },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .drawBehind { drawRect(color = tagColor) }
                                )
                                Text(
                                    text = tag.name,
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 收藏筛选 + 清除按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onFavoritesClick,
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("仅收藏")
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (selectedType != null || selectedTagId != null || showFavoritesOnly) {
                TextButton(onClick = onClearFilters) {
                    Text("清除筛选")
                }
            }
        }
    }
}
