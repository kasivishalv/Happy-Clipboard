package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String = "folder",
    val colorHex: Long = 0xFF3F51B5,
    val createdAt: Long = System.currentTimeMillis()
)
