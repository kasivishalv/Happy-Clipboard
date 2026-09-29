package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClipboardItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY lastCopiedAt DESC")
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE isFavorite = 1 ORDER BY lastCopiedAt DESC")
    fun getFavorites(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE collectionId = :collectionId ORDER BY lastCopiedAt DESC")
    fun getItemsByCollection(collectionId: String): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getItemsByDateRange(startTime: Long, endTime: Long): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ClipboardItem?

    @Query("SELECT * FROM clipboard_items WHERE contentHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): ClipboardItem?

    @Query("""
        SELECT * FROM clipboard_items 
        WHERE content LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%'
           OR collectionName LIKE '%' || :query || '%'
        ORDER BY lastCopiedAt DESC
    """)
    fun searchItems(query: String): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ClipboardItem>): List<Long>

    @Update
    suspend fun update(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clipboard_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM clipboard_items WHERE isFavorite = 0")
    suspend fun clearNonFavorites()

    @Query("DELETE FROM clipboard_items")
    suspend fun clearAll()

    @Query("DELETE FROM clipboard_items WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredItems(now: Long)

    @Query("DELETE FROM clipboard_items WHERE isFavorite = 0 AND createdAt < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun getItemCount(): Int

    @Query("""
        DELETE FROM clipboard_items 
        WHERE isFavorite = 0 
          AND id NOT IN (
              SELECT id FROM clipboard_items 
              ORDER BY lastCopiedAt DESC 
              LIMIT :keepCount
          )
    """)
    suspend fun trimToCapacity(keepCount: Int): Int
}
