package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val contentType: ContentType = ContentType.TEXT,
    val contentHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastCopiedAt: Long = System.currentTimeMillis(),
    val copyCount: Int = 1,
    val isFavorite: Boolean = false,
    val collectionId: String? = null,
    val collectionName: String? = null,
    val sourceApp: String? = null,
    val isSensitive: Boolean = false,
    val expiresAt: Long? = null,
    val notes: String? = null
)
