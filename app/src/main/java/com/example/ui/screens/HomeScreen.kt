package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClipboardItem
import com.example.data.model.ContentType
import com.example.ui.ClipboardUiState
import com.example.ui.MainViewModel
import com.example.ui.components.ClipboardItemCard
import com.example.ui.theme.HappyOrange
import com.example.ui.theme.HappySecurityRestrained
import com.example.ui.theme.HappyTeal300
import com.example.ui.theme.HappyTeal600
import com.example.ui.theme.HappyTeal900
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: ClipboardUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("HappyClipboard", "HOME_SCREEN_ENTERED")
    val context = LocalContext.current
    var selectedFilterCategory by remember { mutableStateOf("All") }

    // Filter items according to: Search query + Filter chips (All, Text, URLs, Code, Images, Favorites, Sensitive)
    val filteredItems = remember(uiState.items, selectedFilterCategory, uiState.searchQuery, uiState.collectionFilter, uiState.selectedDate) {
        uiState.items.filter { item ->
            val matchesSearch = if (uiState.searchQuery.isBlank()) true else {
                item.content.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.notes?.contains(uiState.searchQuery, ignoreCase = true) == true
            }

            val matchesCategory = when (selectedFilterCategory) {
                "Text" -> item.contentType == ContentType.TEXT
                "URLs" -> item.contentType == ContentType.URL
                "Code" -> item.contentType == ContentType.CODE
                "Images" -> item.contentType == ContentType.IMAGE
                "Favorites" -> item.isFavorite
                "Sensitive" -> item.isSensitive
                else -> true
            }

            val matchesCollection = if (uiState.collectionFilter == null) true else item.collectionId == uiState.collectionFilter
            val matchesDate = if (uiState.selectedDate == null) true else {
                val cal = Calendar.getInstance().apply { timeInMillis = item.lastCopiedAt }
                val start = uiState.selectedDate!!
                val end = start + (24 * 60 * 60 * 1000L) - 1
                item.lastCopiedAt in start..end
            }

            matchesSearch && matchesCategory && matchesCollection && matchesDate
        }
    }

    // Group items into date sections: TODAY, YESTERDAY, OLDER
    val groupedItems = remember(filteredItems) {
        val calNow = Calendar.getInstance()
        val nowYear = calNow.get(Calendar.YEAR)
        val nowDay = calNow.get(Calendar.DAY_OF_YEAR)

        val calItem = Calendar.getInstance()

        filteredItems.groupBy { item ->
            calItem.timeInMillis = item.lastCopiedAt
            val itemYear = calItem.get(Calendar.YEAR)
            val itemDay = calItem.get(Calendar.DAY_OF_YEAR)

            when {
                nowYear == itemYear && nowDay == itemDay -> "TODAY"
                nowYear == itemYear && (nowDay - itemDay == 1) -> "YESTERDAY"
                else -> "OLDER"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ==========================================
        // Search Bar (16-20dp rounded corners)
        // ==========================================
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search clips, URLs, code, swatches...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        // ==========================================
        // Filter Chips (All, Text, URLs, Code, Images, Favorites, Sensitive)
        // ==========================================
        val filterOptions = listOf("All", "Text", "URLs", "Code", "Images", "Favorites", "Sensitive")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (filter in filterOptions) {
                val isSelected = selectedFilterCategory == filter
                val selectedColor = when (filter) {
                    "Favorites" -> HappyOrange
                    "Sensitive" -> HappySecurityRestrained
                    "URLs" -> HappyTeal600
                    else -> MaterialTheme.colorScheme.primary
                }

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedFilterCategory = filter
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedColor.copy(alpha = 0.15f),
                        selectedLabelColor = selectedColor,
                        selectedLeadingIconColor = selectedColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        selectedBorderColor = selectedColor
                    ),
                    label = {
                        Text(
                            filter,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            // Bulk select toggle chip
            FilterChip(
                selected = uiState.isSelectionMode,
                onClick = {
                    if (uiState.isSelectionMode) {
                        viewModel.clearSelection()
                    } else {
                        viewModel.enterSelectionMode()
                    }
                },
                modifier = Modifier.testTag("bulk_select_mode_chip"),
                shape = RoundedCornerShape(12.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = uiState.isSelectionMode,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select Mode",
                        tint = if (uiState.isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        if (uiState.isSelectionMode) "Done" else "Select",
                        fontWeight = if (uiState.isSelectionMode) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }

        // Active collection or date filter pill
        if (uiState.collectionFilter != null || uiState.selectedDate != null) {
            val pillShape = RoundedCornerShape(12.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(pillShape),
                shape = pillShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (uiState.collectionFilter != null) {
                            val colName = uiState.collections.find { it.id == uiState.collectionFilter }?.name ?: "Collection"
                            "Filter: Collection '$colName'"
                        } else "Filter: Selected Calendar Date",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = {
                            viewModel.setCollectionFilter(null)
                            viewModel.setSelectedDate(null)
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("clear_filter_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear filter", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Bulk Selection Actions Toolbar
        if (uiState.isSelectionMode) {
            val bulkShape = RoundedCornerShape(12.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(bulkShape),
                shape = bulkShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${uiState.selectedItemIds.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allIds = filteredItems.map { it.id }
                        TextButton(
                            onClick = {
                                if (uiState.selectedItemIds.size == allIds.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll(allIds)
                                }
                            },
                            modifier = Modifier.testTag("bulk_select_all_btn")
                        ) {
                            Text(
                                text = if (uiState.selectedItemIds.size == filteredItems.size && filteredItems.isNotEmpty()) "Deselect" else "Select All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { viewModel.favoriteSelectedItems(true) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("bulk_favorite_btn")
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Favorite selected",
                                tint = HappyOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteSelectedItems() },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("bulk_delete_btn")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = HappySecurityRestrained,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Empty State or Grouped Items List
        // ==========================================
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val emptyIconShape = RoundedCornerShape(20.dp)
                    Surface(
                        shape = emptyIconShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(76.dp)
                            .clip(emptyIconShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Clipboard",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = if (uiState.searchQuery.isNotBlank() || selectedFilterCategory != "All") {
                            "No matching clips found"
                        } else {
                            "Nothing copied yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (uiState.searchQuery.isNotBlank() || selectedFilterCategory != "All") {
                            "Try clearing filters or search query to see your clipboard history."
                        } else {
                            "Copy something from any app and it will appear here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Sample Clip", "Happy Clipboard brand colors: #16697A and #FFA62B"))
                            viewModel.pasteFromClipboard(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Test Clipboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Grouped by TODAY, YESTERDAY, OLDER
            val sectionOrder = listOf("TODAY", "YESTERDAY", "OLDER")

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (header in sectionOrder) {
                    val itemsInGroup = groupedItems[header] ?: emptyList()
                    if (itemsInGroup.isNotEmpty()) {
                        item(key = "header_$header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${itemsInGroup.size} item${if (itemsInGroup.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(itemsInGroup, key = { it.id }) { item ->
                            val isSelected = uiState.selectedItemIds.contains(item.id)
                            ClipboardItemCard(
                                item = item,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onToggleSelect = { viewModel.toggleSelection(item.id) },
                                onCopy = { viewModel.copyItemToClipboard(context, item) },
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                                onOpenDetails = { viewModel.showDetails(item) },
                                onEdit = { viewModel.showEdit(item) },
                                onAddToCollection = { viewModel.showCollectionPicker(item) },
                                onDelete = { viewModel.deleteItem(item.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }
        }
    }
}
