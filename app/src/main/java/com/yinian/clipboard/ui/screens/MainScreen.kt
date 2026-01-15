package com.yinian.clipboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("一念剪贴板") },
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
                showFavoritesOnly = uiState.showFavoritesOnly,
                onTypeClick = { viewModel.filterByType(it) },
                onFavoritesClick = { viewModel.toggleFavoritesOnly() },
                onClearFilters = { viewModel.clearFilters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 剪贴板列表
            ClipboardList(
                clipboards = uiState.filteredClipboards,
                onFavoriteClick = viewModel::toggleFavorite,
                onDeleteClick = viewModel::deleteClipboard,
                onItemClick = { /* TODO: 显示详情 */ },
                modifier = Modifier.fillMaxSize()
            )
        }
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
    showFavoritesOnly: Boolean,
    onTypeClick: (ClipboardType?) -> Unit,
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

            if (selectedType != null || showFavoritesOnly) {
                TextButton(onClick = onClearFilters) {
                    Text("清除筛选")
                }
            }
        }
    }
}
