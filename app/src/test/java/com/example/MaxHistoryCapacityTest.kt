package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.security.SecurityPreferences
import com.example.data.db.AppDatabase
import com.example.data.model.ClipboardItem
import com.example.data.model.ContentType
import com.example.data.repository.ClipboardRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MaxHistoryCapacityTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var prefs: SecurityPreferences
    private lateinit var repository: ClipboardRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear prefs before test
        context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        prefs = SecurityPreferences(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ClipboardRepository(db.clipboardDao(), db.collectionDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAllCapacityOptionsPersistAcrossAppRestarts() {
        val options = SecurityPreferences.CAPACITY_OPTIONS
        assertEquals(
            listOf("200", "500", "1000", "5000", "10000", "25000", "50000", "UNLIMITED"),
            options
        )

        for (opt in options) {
            prefs.maxHistoryCapacity = opt
            assertEquals(opt, prefs.maxHistoryCapacity)

            // Simulate app restart by instantiating a fresh SecurityPreferences
            val restartedPrefs = SecurityPreferences(context)
            assertEquals("Option $opt must persist after restart", opt, restartedPrefs.maxHistoryCapacity)

            if (opt == SecurityPreferences.CAPACITY_UNLIMITED) {
                assertTrue(restartedPrefs.isCapacityUnlimited)
                assertNull(restartedPrefs.maxHistoryLimitOrNull)
            } else {
                assertFalse(restartedPrefs.isCapacityUnlimited)
                assertEquals(opt.toInt(), restartedPrefs.maxHistoryLimitOrNull)
            }
        }
    }

    @Test
    fun testBackwardCompatibilityWithLegacyIntegerSetting() {
        val rawPrefs = context.getSharedPreferences("happy_clipboard_prefs", Context.MODE_PRIVATE)

        // Legacy 1000
        rawPrefs.edit().remove("max_history_capacity").putInt("max_history", 1000).commit()
        val legacyPrefs = SecurityPreferences(context)
        assertEquals("1000", legacyPrefs.maxHistoryCapacity)
        assertEquals(1000, legacyPrefs.maxHistoryLimitOrNull)
        assertFalse(legacyPrefs.isCapacityUnlimited)

        // Legacy 0 (unlimited in some systems)
        rawPrefs.edit().remove("max_history_capacity").putInt("max_history", 0).commit()
        val legacyZeroPrefs = SecurityPreferences(context)
        assertEquals(SecurityPreferences.CAPACITY_UNLIMITED, legacyZeroPrefs.maxHistoryCapacity)
        assertNull(legacyZeroPrefs.maxHistoryLimitOrNull)
        assertTrue(legacyZeroPrefs.isCapacityUnlimited)
    }

    @Test
    fun testFiniteCapacityEnforcement() = runBlocking {
        prefs.maxHistoryCapacity = "200"
        val dao = db.clipboardDao()

        // Insert 250 clips with sequential timestamps
        val now = System.currentTimeMillis()
        val items = (1..250).map { i ->
            ClipboardItem(
                content = "Clip number $i",
                contentType = ContentType.TEXT,
                contentHash = "hash_$i",
                createdAt = now + i * 1000L,
                lastCopiedAt = now + i * 1000L,
                isFavorite = false
            )
        }
        dao.insertAll(items)
        assertEquals(250, dao.getItemCount())

        // Enforce capacity of 200
        repository.enforceMaxCapacity()

        // Total should be trimmed to exactly 200
        assertEquals(200, dao.getItemCount())

        // The oldest 50 items (1..50) should be gone, newest 200 (51..250) should remain
        val remaining = dao.getAllItems().first()
        assertEquals(200, remaining.size)
        // Most recent is #250
        assertEquals("Clip number 250", remaining.first().content)
        // Oldest remaining is #51
        assertEquals("Clip number 51", remaining.last().content)
    }

    @Test
    fun testFavoriteProtectionDuringCapacityEnforcement() = runBlocking {
        prefs.maxHistoryCapacity = "200"
        val dao = db.clipboardDao()
        val now = System.currentTimeMillis()

        // Create 50 older clips, ALL marked as FAVORITE
        val favoriteItems = (1..50).map { i ->
            ClipboardItem(
                content = "Favorite Starred Clip $i",
                contentType = ContentType.TEXT,
                contentHash = "fav_hash_$i",
                createdAt = now + i * 1000L,
                lastCopiedAt = now + i * 1000L,
                isFavorite = true
            )
        }
        dao.insertAll(favoriteItems)

        // Create 200 newer clips that are NOT favorites
        val normalItems = (51..250).map { i ->
            ClipboardItem(
                content = "Normal Clip $i",
                contentType = ContentType.TEXT,
                contentHash = "norm_hash_$i",
                createdAt = now + i * 1000L,
                lastCopiedAt = now + i * 1000L,
                isFavorite = false
            )
        }
        dao.insertAll(normalItems)

        assertEquals(250, dao.getItemCount())

        // Enforce capacity of 200
        repository.enforceMaxCapacity()

        // Check favorites: ALL 50 favorites must still exist!
        val remainingFavorites = dao.getFavorites().first()
        assertEquals(50, remainingFavorites.size)
        for (i in 1..50) {
            assertTrue(
                "Favorite clip $i must be protected",
                remainingFavorites.any { it.content == "Favorite Starred Clip $i" }
            )
        }
    }

    @Test
    fun testRetentionCleanupPreservesFavoritesAndRemovesExpired() = runBlocking {
        prefs.retentionDays = 30
        val dao = db.clipboardDao()
        val now = System.currentTimeMillis()
        val fortyDaysAgo = now - (40L * 24L * 60L * 60L * 1000L)

        // Old normal item (>30 days) -> should be deleted
        dao.insert(
            ClipboardItem(
                content = "Old normal item",
                contentType = ContentType.TEXT,
                contentHash = "old_norm",
                createdAt = fortyDaysAgo,
                lastCopiedAt = fortyDaysAgo,
                isFavorite = false
            )
        )

        // Old favorite item (>30 days) -> must be kept
        dao.insert(
            ClipboardItem(
                content = "Old favorite item",
                contentType = ContentType.TEXT,
                contentHash = "old_fav",
                createdAt = fortyDaysAgo,
                lastCopiedAt = fortyDaysAgo,
                isFavorite = true
            )
        )

        // Recent normal item (1 day ago) -> must be kept
        val oneDayAgo = now - (1L * 24L * 60L * 60L * 1000L)
        dao.insert(
            ClipboardItem(
                content = "Recent normal item",
                contentType = ContentType.TEXT,
                contentHash = "rec_norm",
                createdAt = oneDayAgo,
                lastCopiedAt = oneDayAgo,
                isFavorite = false
            )
        )

        assertEquals(3, dao.getItemCount())

        // Run cleanup
        repository.cleanupExpired()

        val remaining = dao.getAllItems().first()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.content == "Old favorite item" })
        assertTrue(remaining.any { it.content == "Recent normal item" })
        assertFalse(remaining.any { it.content == "Old normal item" })
    }

    @Test
    fun testUnlimitedModeDoesNotDeleteRecords() = runBlocking {
        prefs.maxHistoryCapacity = SecurityPreferences.CAPACITY_UNLIMITED
        assertTrue(prefs.isCapacityUnlimited)
        assertNull(prefs.maxHistoryLimitOrNull)

        val dao = db.clipboardDao()
        val now = System.currentTimeMillis()

        // Insert 1,200 records in batches to test large volume
        val totalToInsert = 1200
        val batchSize = 300
        for (batch in 0 until (totalToInsert / batchSize)) {
            val batchItems = (1..batchSize).map { idx ->
                val idNum = batch * batchSize + idx
                ClipboardItem(
                    content = "Unlimited storage test clip record #$idNum - payload token",
                    contentType = ContentType.TEXT,
                    contentHash = "unlimited_hash_$idNum",
                    createdAt = now + idNum * 100L,
                    lastCopiedAt = now + idNum * 100L,
                    isFavorite = (idNum % 10 == 0)
                )
            }
            dao.insertAll(batchItems)
        }

        assertEquals(totalToInsert, dao.getItemCount())

        // Trigger capacity enforcement while UNLIMITED is set
        repository.enforceMaxCapacity()

        // Verify that NO clips are deleted because Unlimited is selected
        val countAfter = dao.getItemCount()
        assertEquals("Unlimited mode must never auto-delete clips", totalToInsert, countAfter)

        // Verify query/search works with 1,200 records
        val searchResults = dao.searchItems("record #777").first()
        assertEquals(1, searchResults.size)
        assertEquals("Unlimited storage test clip record #777 - payload token", searchResults.first().content)

        // Verify ordered list retrieval works for lazy scrolling
        val allItems = dao.getAllItems().first()
        assertEquals(totalToInsert, allItems.size)
        assertEquals("Unlimited storage test clip record #1200 - payload token", allItems.first().content)
        assertEquals("Unlimited storage test clip record #1 - payload token", allItems.last().content)
    }

    @Test
    fun testClearHistoryStillWorksInUnlimitedMode() = runBlocking {
        prefs.maxHistoryCapacity = SecurityPreferences.CAPACITY_UNLIMITED
        val dao = db.clipboardDao()
        val now = System.currentTimeMillis()

        dao.insert(ClipboardItem(content = "Norm 1", contentHash = "h1", createdAt = now, lastCopiedAt = now, isFavorite = false))
        dao.insert(ClipboardItem(content = "Fav 1", contentHash = "h2", createdAt = now, lastCopiedAt = now, isFavorite = true))
        dao.insert(ClipboardItem(content = "Norm 2", contentHash = "h3", createdAt = now, lastCopiedAt = now, isFavorite = false))

        assertEquals(3, dao.getItemCount())

        // Clear non-favorites
        repository.clearNonFavorites()

        assertEquals(1, dao.getItemCount())
        val remaining = dao.getAllItems().first()
        assertEquals("Fav 1", remaining.first().content)
    }
}
