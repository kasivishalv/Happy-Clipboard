package com.example.data.repository

import com.example.core.classifier.ContentClassifier
import com.example.core.security.CryptoHelper
import com.example.core.security.SecurityPreferences
import com.example.data.db.ClipboardDao
import com.example.data.db.CollectionDao
import com.example.data.model.ClipboardItem
import com.example.data.model.CollectionEntity
import com.example.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ClipboardRepository(
    private val clipboardDao: ClipboardDao,
    private val collectionDao: CollectionDao,
    private val preferences: SecurityPreferences
) {
    val allItems: Flow<List<ClipboardItem>> = clipboardDao.getAllItems()
    val favoriteItems: Flow<List<ClipboardItem>> = clipboardDao.getFavorites()
    val allCollections: Flow<List<CollectionEntity>> = collectionDao.getAllCollections()

    fun getItemsByCollection(collectionId: String): Flow<List<ClipboardItem>> {
        return clipboardDao.getItemsByCollection(collectionId)
    }

    fun search(query: String): Flow<List<ClipboardItem>> {
        return clipboardDao.searchItems(query)
    }

    fun getItemsByDateRange(startTime: Long, endTime: Long): Flow<List<ClipboardItem>> {
        return clipboardDao.getItemsByDateRange(startTime, endTime)
    }

    suspend fun captureClip(content: String, sourceApp: String? = null): ClipboardItem? = withContext(Dispatchers.IO) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return@withContext null

        // Check if source app is in excluded list
        if (!sourceApp.isNullOrBlank() && preferences.excludedPackages.any { pkg -> sourceApp.contains(pkg, ignoreCase = true) }) {
            return@withContext null
        }

        val hash = ContentClassifier.sha256(trimmed)
        val existing = clipboardDao.findByHash(hash)

        if (existing != null && preferences.duplicateHandling == "MERGE") {
            val updated = existing.copy(
                copyCount = existing.copyCount + 1,
                lastCopiedAt = System.currentTimeMillis()
            )
            clipboardDao.update(updated)
            return@withContext updated
        }

        // Classification
        val classification = ContentClassifier.classify(
            trimmed,
            otpAutoDeleteMinutes = preferences.otpAutoDeleteMinutes
        )

        // Check sensitive preference
        if (classification.isSensitive && preferences.sensitiveHandling == "DONT_SAVE") {
            return@withContext null
        }

        val newItem = ClipboardItem(
            content = trimmed,
            contentType = classification.contentType,
            contentHash = hash,
            createdAt = System.currentTimeMillis(),
            lastCopiedAt = System.currentTimeMillis(),
            copyCount = 1,
            isFavorite = false,
            sourceApp = sourceApp,
            isSensitive = classification.isSensitive,
            expiresAt = classification.expiresAt
        )

        val id = try {
            clipboardDao.insert(newItem)
        } catch (e: Exception) {
            android.util.Log.e("HappyClipboard", "Error inserting clip", e)
            return@withContext null
        }
        enforceMaxCapacity()
        return@withContext newItem.copy(id = id)
    }

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val item = clipboardDao.getItemById(id) ?: return@withContext
        clipboardDao.update(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun setCollection(id: Long, collectionId: String?, collectionName: String?) = withContext(Dispatchers.IO) {
        val item = clipboardDao.getItemById(id) ?: return@withContext
        clipboardDao.update(item.copy(collectionId = collectionId, collectionName = collectionName))
    }

    suspend fun updateNotes(id: Long, notes: String?) = withContext(Dispatchers.IO) {
        val item = clipboardDao.getItemById(id) ?: return@withContext
        clipboardDao.update(item.copy(notes = notes))
    }

    suspend fun updateContent(id: Long, newContent: String) = withContext(Dispatchers.IO) {
        val item = clipboardDao.getItemById(id) ?: return@withContext
        val classification = ContentClassifier.classify(newContent, preferences.otpAutoDeleteMinutes)
        val updated = item.copy(
            content = newContent,
            contentType = classification.contentType,
            contentHash = ContentClassifier.sha256(newContent),
            isSensitive = classification.isSensitive
        )
        clipboardDao.update(updated)
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        clipboardDao.deleteById(id)
    }

    suspend fun deleteItems(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isNotEmpty()) {
            clipboardDao.deleteByIds(ids)
        }
    }

    suspend fun clearNonFavorites() = withContext(Dispatchers.IO) {
        clipboardDao.clearNonFavorites()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        clipboardDao.clearAll()
    }

    suspend fun createCollection(name: String, icon: String = "folder", colorHex: Long = 0xFF3F51B5) = withContext(Dispatchers.IO) {
        val id = name.lowercase().replace("\\s+".toRegex(), "_") + "_" + System.currentTimeMillis() % 1000
        val entity = CollectionEntity(id = id, name = name, iconName = icon, colorHex = colorHex)
        collectionDao.insert(entity)
    }

    suspend fun deleteCollection(id: String) = withContext(Dispatchers.IO) {
        collectionDao.deleteById(id)
    }

    suspend fun cleanupExpired() = withContext(Dispatchers.IO) {
        try {
            clipboardDao.deleteExpiredItems(System.currentTimeMillis())
            val retention = preferences.retentionDays
            if (retention > 0) {
                val cutoff = System.currentTimeMillis() - (retention * 24L * 60L * 60L * 1000L)
                clipboardDao.deleteOlderThan(cutoff)
            }
            enforceMaxCapacity()
        } catch (e: Exception) {
            android.util.Log.e("HappyClipboard", "Error during expired/retention cleanup", e)
        }
    }

    suspend fun enforceMaxCapacity() = withContext(Dispatchers.IO) {
        try {
            val limit = preferences.maxHistoryLimitOrNull ?: return@withContext
            if (limit > 0) {
                val count = clipboardDao.getItemCount()
                if (count > limit) {
                    clipboardDao.trimToCapacity(limit)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HappyClipboard", "Error enforcing max history capacity", e)
        }
    }

    // Export & Backup
    fun exportToJson(items: List<ClipboardItem>): String {
        val root = JSONObject()
        root.put("format", "happy-clipboard")
        root.put("version", 1)
        root.put("createdAt", System.currentTimeMillis())

        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("content", item.content)
            obj.put("contentType", item.contentType.name)
            obj.put("contentHash", item.contentHash)
            obj.put("createdAt", item.createdAt)
            obj.put("lastCopiedAt", item.lastCopiedAt)
            obj.put("copyCount", item.copyCount)
            obj.put("isFavorite", item.isFavorite)
            obj.put("collectionId", item.collectionId ?: "")
            obj.put("collectionName", item.collectionName ?: "")
            obj.put("notes", item.notes ?: "")
            array.put(obj)
        }
        root.put("items", array)
        return root.toString(2)
    }

    fun exportToHcb(items: List<ClipboardItem>, password: String): String {
        val json = exportToJson(items)
        return CryptoHelper.encrypt(json, password)
    }

    suspend fun importFromJson(jsonString: String, mode: String = "MERGE"): Int = withContext(Dispatchers.IO) {
        try {
            // Phase 1: Validate payload schema & parse all records in memory
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("items") ?: return@withContext 0

            val itemsToInsert = mutableListOf<ClipboardItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val content = obj.optString("content", "")
                if (content.isEmpty()) continue

                val typeStr = obj.optString("contentType", ContentType.TEXT.name)
                val type = try { ContentType.valueOf(typeStr) } catch (_: Exception) { ContentType.TEXT }
                val hash = obj.optString("contentHash", ContentClassifier.sha256(content))
                val isFav = obj.optBoolean("isFavorite", false)
                val colId = obj.optString("collectionId").takeIf { it.isNotEmpty() }
                val colName = obj.optString("collectionName").takeIf { it.isNotEmpty() }
                val notes = obj.optString("notes").takeIf { it.isNotEmpty() }

                itemsToInsert.add(
                    ClipboardItem(
                        content = content,
                        contentType = type,
                        contentHash = hash,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        lastCopiedAt = obj.optLong("lastCopiedAt", System.currentTimeMillis()),
                        copyCount = obj.optInt("copyCount", 1),
                        isFavorite = isFav,
                        collectionId = colId,
                        collectionName = colName,
                        notes = notes
                    )
                )
            }

            if (itemsToInsert.isEmpty()) return@withContext 0

            // Phase 2: Transactional database execution
            if (mode == "REPLACE") {
                clipboardDao.clearAll()
            }
            clipboardDao.insertAll(itemsToInsert)
            itemsToInsert.size
        } catch (_: Exception) {
            0
        }
    }

    suspend fun importFromHcb(hcbPayload: String, password: String, mode: String = "MERGE"): Int {
        val decrypted = CryptoHelper.decrypt(hcbPayload, password)
        return importFromJson(decrypted, mode)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = clipboardDao.getItemCount()
        if (count > 0 && preferences.initialDataSeeded) return@withContext

        // Default Collections
        val collections = listOf(
            CollectionEntity("docker", "Docker & DevOps", "terminal", 0xFF0288D1),
            CollectionEntity("programming", "Programming & SQL", "code", 0xFF7B1FA2),
            CollectionEntity("frequent", "Frequently Used", "star", 0xFFF57C00),
            CollectionEntity("personal", "Personal & Notes", "person", 0xFF388E3C),
            CollectionEntity("links", "Web Resources", "link", 0xFFE64A19)
        )
        collectionDao.insertAll(collections)

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        
        // Today timestamps
        val t1 = now - (15 * 60 * 1000L) // 15 mins ago
        val t2 = now - (45 * 60 * 1000L) // 45 mins ago
        val t3 = now - (2 * 3600 * 1000L) // 2 hours ago
        val t4 = now - (4 * 3600 * 1000L) // 4 hours ago

        // Yesterday timestamps
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayBase = cal.timeInMillis
        val y1 = yesterdayBase - (1 * 3600 * 1000L)
        val y2 = yesterdayBase - (3 * 3600 * 1000L)
        val y3 = yesterdayBase - (6 * 3600 * 1000L)

        // Older day (2 days ago)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val older1 = cal.timeInMillis

        val sampleClips = listOf(
            // Today clips
            ClipboardItem(
                content = "docker compose up -d && docker compose ps",
                contentType = ContentType.CODE,
                contentHash = ContentClassifier.sha256("docker compose up -d && docker compose ps"),
                createdAt = t1,
                lastCopiedAt = t1,
                copyCount = 4,
                isFavorite = true,
                collectionId = "docker",
                collectionName = "Docker & DevOps",
                sourceApp = "Terminal"
            ),
            ClipboardItem(
                content = "https://github.com/kasivishalv/happy-clipboard",
                contentType = ContentType.URL,
                contentHash = ContentClassifier.sha256("https://github.com/kasivishalv/happy-clipboard"),
                createdAt = t2,
                lastCopiedAt = t2,
                copyCount = 2,
                isFavorite = true,
                collectionId = "links",
                collectionName = "Web Resources",
                sourceApp = "Chrome"
            ),
            ClipboardItem(
                content = "#3F51B5",
                contentType = ContentType.COLOR,
                contentHash = ContentClassifier.sha256("#3F51B5"),
                createdAt = t3,
                lastCopiedAt = t3,
                copyCount = 1,
                isFavorite = false,
                collectionId = "programming",
                collectionName = "Programming & SQL",
                notes = "Primary brand color"
            ),
            ClipboardItem(
                content = "Your verification code is 482913. Do not share this OTP with anyone.",
                contentType = ContentType.SENSITIVE,
                contentHash = ContentClassifier.sha256("Your verification code is 482913. Do not share this OTP with anyone."),
                createdAt = t4,
                lastCopiedAt = t4,
                copyCount = 1,
                isFavorite = false,
                isSensitive = true,
                expiresAt = now + (4 * 60 * 1000L + 45 * 1000L) // expires in ~4m 45s
            ),
            // Yesterday clips
            ClipboardItem(
                content = "SELECT u.id, u.email, COUNT(c.id) as clip_count FROM users u LEFT JOIN clipboard_items c ON u.id = c.user_id GROUP BY u.id;",
                contentType = ContentType.CODE,
                contentHash = ContentClassifier.sha256("SELECT u.id, u.email, COUNT(c.id) as clip_count FROM users u LEFT JOIN clipboard_items c ON u.id = c.user_id GROUP BY u.id;"),
                createdAt = y1,
                lastCopiedAt = y1,
                copyCount = 3,
                isFavorite = true,
                collectionId = "programming",
                collectionName = "Programming & SQL"
            ),
            ClipboardItem(
                content = "team-lead@happyclipboard.dev",
                contentType = ContentType.EMAIL,
                contentHash = ContentClassifier.sha256("team-lead@happyclipboard.dev"),
                createdAt = y2,
                lastCopiedAt = y2,
                copyCount = 1,
                isFavorite = false,
                collectionId = "frequent",
                collectionName = "Frequently Used"
            ),
            ClipboardItem(
                content = "+1 (555) 349-8291",
                contentType = ContentType.PHONE,
                contentHash = ContentClassifier.sha256("+1 (555) 349-8291"),
                createdAt = y3,
                lastCopiedAt = y3,
                copyCount = 1,
                isFavorite = false,
                collectionId = "personal",
                collectionName = "Personal & Notes"
            ),
            // 2 days ago
            ClipboardItem(
                content = "npm install @tanstack/react-query zustand lucide-react",
                contentType = ContentType.CODE,
                contentHash = ContentClassifier.sha256("npm install @tanstack/react-query zustand lucide-react"),
                createdAt = older1,
                lastCopiedAt = older1,
                copyCount = 1,
                isFavorite = false,
                collectionId = "programming",
                collectionName = "Programming & SQL"
            )
        )

        for (clip in sampleClips) {
            clipboardDao.insert(clip)
        }
        preferences.initialDataSeeded = true
    }
}
