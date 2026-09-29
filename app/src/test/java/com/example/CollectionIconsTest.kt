package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Terminal
import com.example.data.model.CollectionEntity
import com.example.ui.util.CollectionIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CollectionIconsTest {

    @Test
    fun `default key is folder`() {
        assertEquals("folder", CollectionIcons.DEFAULT_KEY)
        val defaultIcon = CollectionIcons.resolveIcon(CollectionIcons.DEFAULT_KEY)
        assertEquals(Icons.Default.Folder, defaultIcon)
    }

    @Test
    fun `resolve known icons correctly`() {
        assertEquals(Icons.Default.Code, CollectionIcons.resolveIcon("code"))
        assertEquals(Icons.Default.Terminal, CollectionIcons.resolveIcon("terminal"))
        assertEquals(Icons.Default.Folder, CollectionIcons.resolveIcon("folder"))
    }

    @Test
    fun `resolve is case insensitive`() {
        assertEquals(Icons.Default.Code, CollectionIcons.resolveIcon("CODE"))
        assertEquals(Icons.Default.Terminal, CollectionIcons.resolveIcon("Terminal"))
    }

    @Test
    fun `fallback to folder for null, blank, or unknown legacy iconKey`() {
        assertEquals(Icons.Default.Folder, CollectionIcons.resolveIcon(null))
        assertEquals(Icons.Default.Folder, CollectionIcons.resolveIcon(""))
        assertEquals(Icons.Default.Folder, CollectionIcons.resolveIcon("   "))
        assertEquals(Icons.Default.Folder, CollectionIcons.resolveIcon("unknown_legacy_icon_123"))
        assertEquals(Icons.Default.Folder, CollectionIcons.resolveIcon("non_existent_key"))
    }

    @Test
    fun `all 22 required icon options are present in ALL list`() {
        val expectedKeys = listOf(
            "folder", "code", "terminal", "link", "image",
            "star", "person", "notes", "work", "school",
            "finance", "shopping", "music", "video", "bookmark",
            "favorite", "security", "apps", "database", "cloud",
            "calendar", "custom"
        )
        assertEquals(22, CollectionIcons.ALL.size)
        for (key in expectedKeys) {
            val option = CollectionIcons.ALL.firstOrNull { it.key == key }
            assertNotNull("Option for key '$key' should exist", option)
            assertNotNull("Icon for key '$key' should resolve", option?.icon)
            assertNotNull("Label for key '$key' should exist", option?.label)
        }
    }

    @Test
    fun `collection entity holds iconName and defaults to folder`() {
        val defaultEntity = CollectionEntity(id = "test_1", name = "Test Col")
        assertEquals("folder", defaultEntity.iconName)

        val customEntity = CollectionEntity(id = "code_col", name = "Code Snippets", iconName = "code", colorHex = 0xFF16697A)
        assertEquals("code", customEntity.iconName)
        assertEquals(0xFF16697A, customEntity.colorHex)

        val resolved = CollectionIcons.resolveIcon(customEntity.iconName)
        assertEquals(Icons.Default.Code, resolved)
    }

    @Test
    fun `getLabel returns friendly labels with fallback to Folder`() {
        assertEquals("Code", CollectionIcons.getLabel("code"))
        assertEquals("Terminal", CollectionIcons.getLabel("terminal"))
        assertEquals("Folder", CollectionIcons.getLabel("folder"))
        assertEquals("Folder", CollectionIcons.getLabel("unknown_legacy"))
        assertEquals("Folder", CollectionIcons.getLabel(null))
    }
}
