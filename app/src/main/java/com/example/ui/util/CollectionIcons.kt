package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class CollectionIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

object CollectionIcons {
    const val DEFAULT_KEY = "folder"

    val ALL: List<CollectionIconOption> = listOf(
        CollectionIconOption("folder", "Folder", Icons.Default.Folder),
        CollectionIconOption("code", "Code", Icons.Default.Code),
        CollectionIconOption("terminal", "Terminal", Icons.Default.Terminal),
        CollectionIconOption("link", "Link", Icons.Default.Link),
        CollectionIconOption("image", "Image", Icons.Default.Image),
        CollectionIconOption("star", "Star", Icons.Default.Star),
        CollectionIconOption("person", "Personal", Icons.Default.Person),
        CollectionIconOption("notes", "Notes", Icons.Default.Description),
        CollectionIconOption("work", "Work", Icons.Default.Work),
        CollectionIconOption("school", "School", Icons.Default.School),
        CollectionIconOption("finance", "Finance", Icons.Default.AccountBalance),
        CollectionIconOption("shopping", "Shopping", Icons.Default.ShoppingCart),
        CollectionIconOption("music", "Music", Icons.Default.MusicNote),
        CollectionIconOption("video", "Video", Icons.Default.Videocam),
        CollectionIconOption("bookmark", "Bookmark", Icons.Default.Bookmark),
        CollectionIconOption("favorite", "Favorite", Icons.Default.Favorite),
        CollectionIconOption("security", "Security", Icons.Default.Lock),
        CollectionIconOption("apps", "Apps", Icons.Default.Apps),
        CollectionIconOption("database", "Database", Icons.Default.Storage),
        CollectionIconOption("cloud", "Cloud", Icons.Default.Cloud),
        CollectionIconOption("calendar", "Calendar", Icons.Default.CalendarToday),
        CollectionIconOption("custom", "Custom", Icons.Default.Category)
    )

    fun resolveIcon(key: String?): ImageVector {
        if (key.isNullOrBlank()) return Icons.Default.Folder
        return ALL.firstOrNull { it.key.equals(key, ignoreCase = true) }?.icon ?: Icons.Default.Folder
    }

    fun getLabel(key: String?): String {
        if (key.isNullOrBlank()) return "Folder"
        return ALL.firstOrNull { it.key.equals(key, ignoreCase = true) }?.label ?: "Folder"
    }
}
