package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.CollectionEntity
import com.example.ui.util.CollectionIcons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Happy Clipboard", appName)
  }

  @Test
  fun `collection persists iconName and resolves correctly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    val dao = db.collectionDao()

    val colCode = CollectionEntity(
      id = "test_code_1",
      name = "Docker & DevOps",
      iconName = "terminal",
      colorHex = 0xFF0288D1
    )
    val colStar = CollectionEntity(
      id = "test_star_2",
      name = "Favorites & Pinned",
      iconName = "star",
      colorHex = 0xFFFFA62B
    )
    dao.insert(colCode)
    dao.insert(colStar)

    val list = dao.getAllCollections().first()
    assertEquals(2, list.size)

    val retrievedCode = list.first { it.id == "test_code_1" }
    assertEquals("terminal", retrievedCode.iconName)
    assertEquals("Docker & DevOps", retrievedCode.name)
    assertEquals(0xFF0288D1, retrievedCode.colorHex)

    val retrievedStar = list.first { it.id == "test_star_2" }
    assertEquals("star", retrievedStar.iconName)

    // Verify icon resolution
    assertEquals(androidx.compose.material.icons.Icons.Default.Terminal, CollectionIcons.resolveIcon(retrievedCode.iconName))
    assertEquals(androidx.compose.material.icons.Icons.Default.Star, CollectionIcons.resolveIcon(retrievedStar.iconName))

    db.close()
  }
}
