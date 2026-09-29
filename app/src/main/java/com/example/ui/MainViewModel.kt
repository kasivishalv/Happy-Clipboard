package com.example.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.security.SecurityPreferences
import com.example.data.model.ClipboardItem
import com.example.data.model.CollectionEntity
import com.example.data.model.ContentType
import com.example.data.repository.ClipboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab {
    HISTORY,
    CALENDAR,
    COLLECTIONS,
    FAVORITES,
    SETTINGS
}

data class FilterState(
    val tab: NavigationTab = NavigationTab.HISTORY,
    val query: String = "",
    val type: ContentType? = null,
    val collectionId: String? = null,
    val date: Long? = null
)

data class DialogOverlayState(
    val isAppLocked: Boolean = false,
    val isPinSet: Boolean = false,
    val isMonitoringActive: Boolean = true,
    val themeMode: String = "SYSTEM",
    val isSecureScreensEnabled: Boolean = false,
    val startupError: String? = null,
    val selectedItemForDetails: ClipboardItem? = null,
    val itemToEdit: ClipboardItem? = null,
    val itemForCollection: ClipboardItem? = null,
    val showAddDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showNewCollectionDialog: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val snackbarMessage: String? = null
)

data class ClipboardUiState(
    val items: List<ClipboardItem> = emptyList(),
    val favoriteItems: List<ClipboardItem> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val selectedTab: NavigationTab = NavigationTab.HISTORY,
    val searchQuery: String = "",
    val typeFilter: ContentType? = null,
    val collectionFilter: String? = null,
    val selectedDate: Long? = null, // Midnight timestamp for date filtering
    val isAppLocked: Boolean = false,
    val isPinSet: Boolean = false,
    val isMonitoringActive: Boolean = true,
    val themeMode: String = "SYSTEM",
    val isSecureScreensEnabled: Boolean = false,
    val startupError: String? = null,
    val selectedItemForDetails: ClipboardItem? = null,
    val itemToEdit: ClipboardItem? = null,
    val itemForCollection: ClipboardItem? = null,
    val showAddDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showNewCollectionDialog: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val snackbarMessage: String? = null
)

class MainViewModel(
    private val repository: ClipboardRepository,
    val preferences: SecurityPreferences
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    private val _overlayState = MutableStateFlow(
        DialogOverlayState(
            isAppLocked = preferences.isAppLockEnabled,
            isPinSet = preferences.isPinSet,
            isMonitoringActive = preferences.isAutoCaptureEnabled,
            themeMode = preferences.themeMode,
            isSecureScreensEnabled = preferences.isSecureScreensEnabled
        )
    )

    val collections: StateFlow<List<CollectionEntity>> = repository.allCollections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ClipboardUiState> = combine(
        repository.allItems,
        repository.favoriteItems,
        repository.allCollections,
        _filterState,
        _overlayState
    ) { allItems: List<ClipboardItem>, favs: List<ClipboardItem>, cols: List<CollectionEntity>, filter: FilterState, overlay: DialogOverlayState ->
        var filtered = allItems

        // Filter by tab
        if (filter.tab == NavigationTab.FAVORITES) {
            filtered = filtered.filter { it.isFavorite }
        }

        // Filter by collection
        if (filter.collectionId != null) {
            filtered = filtered.filter { it.collectionId == filter.collectionId }
        }

        // Filter by content type
        if (filter.type != null) {
            filtered = filtered.filter { it.contentType == filter.type }
        }

        // Filter by date
        if (filter.date != null) {
            val startOfDay = filter.date
            val endOfDay = filter.date + (24 * 60 * 60 * 1000L) - 1
            filtered = filtered.filter { it.createdAt in startOfDay..endOfDay }
        }

        // Filter by search query
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            filtered = filtered.filter {
                it.content.lowercase().contains(q) ||
                it.notes?.lowercase()?.contains(q) == true ||
                it.collectionName?.lowercase()?.contains(q) == true ||
                it.contentType.name.lowercase().contains(q)
            }
        }

        ClipboardUiState(
            items = filtered,
            favoriteItems = favs,
            collections = cols,
            selectedTab = filter.tab,
            searchQuery = filter.query,
            typeFilter = filter.type,
            collectionFilter = filter.collectionId,
            selectedDate = filter.date,
            isAppLocked = overlay.isAppLocked,
            isPinSet = overlay.isPinSet,
            isMonitoringActive = overlay.isMonitoringActive,
            themeMode = overlay.themeMode,
            isSecureScreensEnabled = overlay.isSecureScreensEnabled,
            startupError = overlay.startupError,
            selectedItemForDetails = overlay.selectedItemForDetails,
            itemToEdit = overlay.itemToEdit,
            itemForCollection = overlay.itemForCollection,
            showAddDialog = overlay.showAddDialog,
            showExportDialog = overlay.showExportDialog,
            showImportDialog = overlay.showImportDialog,
            showNewCollectionDialog = overlay.showNewCollectionDialog,
            selectedItemIds = overlay.selectedItemIds,
            isSelectionMode = overlay.isSelectionMode,
            snackbarMessage = overlay.snackbarMessage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ClipboardUiState(
            isAppLocked = preferences.isAppLockEnabled,
            isPinSet = preferences.isPinSet
        )
    )

    val isAppLocked: Boolean get() = _overlayState.value.isAppLocked
    val isPinSet: Boolean get() = _overlayState.value.isPinSet

    fun setTab(tab: NavigationTab) {
        _filterState.update { it.copy(tab = tab) }
    }

    fun setSearchQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun setTypeFilter(type: ContentType?) {
        _filterState.update { it.copy(type = type) }
    }

    fun setCollectionFilter(collectionId: String?) {
        _filterState.update { it.copy(collectionId = collectionId) }
    }

    fun setSelectedDate(date: Long?) {
        _filterState.update { it.copy(date = date) }
    }

    fun toggleMonitoring() {
        val newState = !_overlayState.value.isMonitoringActive
        preferences.isAutoCaptureEnabled = newState
        _overlayState.update {
            it.copy(
                isMonitoringActive = newState,
                snackbarMessage = if (newState) "Clipboard monitoring active" else "Clipboard monitoring paused"
            )
        }
    }

    fun copyItemToClipboard(context: Context, item: ClipboardItem) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied from Happy Clipboard", item.content)

        // Android 13+ (API 33+) sensitivity marking for clipboard overlay privacy
        if (item.isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        cm.setPrimaryClip(clip)
        val msg = if (item.isSensitive) "Copied sensitive clip (hidden from OS preview)" else "Copied to clipboard!"
        _overlayState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearSystemClipboard(context: Context) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("", ""))
            _overlayState.update { it.copy(snackbarMessage = "Android system clipboard cleared") }
        } catch (_: Exception) {
            _overlayState.update { it.copy(snackbarMessage = "Could not clear system clipboard") }
        }
    }

    // Bulk selection handlers
    fun enterSelectionMode(initialId: Long? = null) {
        _overlayState.update { state ->
            val set = if (initialId != null && initialId > 0) setOf(initialId) else emptySet()
            state.copy(
                selectedItemIds = set,
                isSelectionMode = true
            )
        }
    }

    fun toggleSelection(itemId: Long) {
        _overlayState.update { state ->
            val current = state.selectedItemIds
            val updated = if (current.contains(itemId)) current - itemId else current + itemId
            state.copy(
                selectedItemIds = updated,
                isSelectionMode = state.isSelectionMode || updated.isNotEmpty()
            )
        }
    }

    fun selectAll(itemIds: List<Long>) {
        _overlayState.update { state ->
            state.copy(
                selectedItemIds = itemIds.toSet(),
                isSelectionMode = itemIds.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _overlayState.update { state ->
            state.copy(
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun deleteSelectedItems() {
        val idsToDelete = _overlayState.value.selectedItemIds.toList()
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteItems(idsToDelete)
            _overlayState.update { state ->
                state.copy(
                    selectedItemIds = emptySet(),
                    isSelectionMode = false,
                    snackbarMessage = "Deleted ${idsToDelete.size} items"
                )
            }
        }
    }

    fun favoriteSelectedItems(favorite: Boolean) {
        val ids = _overlayState.value.selectedItemIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            for (id in ids) {
                repository.toggleFavorite(id)
            }
            _overlayState.update { state ->
                state.copy(
                    selectedItemIds = emptySet(),
                    isSelectionMode = false,
                    snackbarMessage = "Updated favorites for ${ids.size} items"
                )
            }
        }
    }

    fun addExcludedPackage(packageName: String) {
        val current = preferences.excludedPackages.toMutableSet()
        current.add(packageName.trim())
        preferences.excludedPackages = current
        _overlayState.update { it.copy(snackbarMessage = "Added '$packageName' to excluded apps") }
    }

    fun removeExcludedPackage(packageName: String) {
        val current = preferences.excludedPackages.toMutableSet()
        current.remove(packageName.trim())
        preferences.excludedPackages = current
        _overlayState.update { it.copy(snackbarMessage = "Removed '$packageName' from excluded apps") }
    }

    fun pasteFromClipboard(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank()) {
                viewModelScope.launch {
                    val captured = repository.captureClip(text, sourceApp = "Manual Paste")
                    _overlayState.update {
                        it.copy(
                            snackbarMessage = if (captured != null) "Captured: ${text.take(30)}..." else "Saved to clipboard history"
                        )
                    }
                }
            } else {
                _overlayState.update { it.copy(snackbarMessage = "Clipboard is empty") }
            }
        } else {
            _overlayState.update { it.copy(snackbarMessage = "Clipboard is empty") }
        }
    }

    fun addNewClip(content: String, notes: String? = null) {
        viewModelScope.launch {
            val item = repository.captureClip(content, sourceApp = "Happy Clipboard")
            if (item != null && !notes.isNullOrBlank()) {
                repository.updateNotes(item.id, notes)
            }
            _overlayState.update {
                it.copy(
                    showAddDialog = false,
                    snackbarMessage = "Item added to history"
                )
            }
        }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun setItemCollection(itemId: Long, collection: CollectionEntity?) {
        viewModelScope.launch {
            repository.setCollection(itemId, collection?.id, collection?.name)
            _overlayState.update {
                it.copy(
                    itemForCollection = null,
                    snackbarMessage = if (collection != null) "Added to ${collection.name}" else "Removed from collection"
                )
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
            _overlayState.update {
                it.copy(
                    selectedItemForDetails = if (it.selectedItemForDetails?.id == id) null else it.selectedItemForDetails,
                    snackbarMessage = "Item deleted"
                )
            }
        }
    }

    fun updateItem(id: Long, newContent: String, notes: String?) {
        viewModelScope.launch {
            repository.updateContent(id, newContent)
            repository.updateNotes(id, notes)
            _overlayState.update {
                it.copy(
                    itemToEdit = null,
                    selectedItemForDetails = null,
                    snackbarMessage = "Item updated"
                )
            }
        }
    }

    fun createCollection(name: String, iconKey: String = "folder", colorHex: Long) {
        viewModelScope.launch {
            repository.createCollection(name, iconKey, colorHex)
            _overlayState.update {
                it.copy(
                    showNewCollectionDialog = false,
                    snackbarMessage = "Collection '$name' created"
                )
            }
        }
    }

    fun deleteCollection(id: String) {
        viewModelScope.launch {
            repository.deleteCollection(id)
            _filterState.update {
                if (it.collectionId == id) it.copy(collectionId = null) else it
            }
            _overlayState.update { it.copy(snackbarMessage = "Collection deleted") }
        }
    }

    fun clearNonFavorites() {
        viewModelScope.launch {
            repository.clearNonFavorites()
            _overlayState.update { it.copy(snackbarMessage = "Cleared history (Favorites kept)") }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _overlayState.update { it.copy(snackbarMessage = "All clipboard history cleared") }
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            preferences.initialDataSeeded = false
            repository.clearAll()
            repository.seedSampleDataIfEmpty()
            _overlayState.update { it.copy(snackbarMessage = "Sample items reloaded") }
        }
    }

    fun exportBackup(password: String?): String {
        val items = uiState.value.items
        return if (!password.isNullOrBlank()) {
            repository.exportToHcb(items, password)
        } else {
            repository.exportToJson(items)
        }
    }

    fun importBackup(content: String, password: String?, mode: String) {
        viewModelScope.launch {
            val count = if (!password.isNullOrBlank()) {
                repository.importFromHcb(content, password, mode)
            } else {
                repository.importFromJson(content, mode)
            }
            _overlayState.update {
                it.copy(
                    showImportDialog = false,
                    snackbarMessage = "Imported $count items successfully"
                )
            }
        }
    }

    fun unlockApp(pin: String): Boolean {
        if (!preferences.isAppLockEnabled) {
            _overlayState.update { it.copy(isAppLocked = false) }
            return true
        }
        if (preferences.verifyPin(pin)) {
            _overlayState.update { it.copy(isAppLocked = false) }
            return true
        }
        return false
    }

    fun setAppPin(pin: String, confirmPin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        if (pin != confirmPin) return false
        preferences.setPin(pin)
        preferences.isAppLockEnabled = true
        _overlayState.update { it.copy(isAppLocked = false, isPinSet = true) }
        return true
    }

    fun changePin(oldPin: String, newPin: String, confirmPin: String): Boolean {
        if (!preferences.verifyPin(oldPin)) return false
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) return false
        if (newPin != confirmPin) return false
        preferences.setPin(newPin)
        _overlayState.update { it.copy(isPinSet = true) }
        return true
    }

    fun lockApp() {
        if (preferences.isAppLockEnabled) {
            _overlayState.update { it.copy(isAppLocked = true) }
        }
    }

    fun purgeSystemClipboard(context: Context) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("", ""))
        _overlayState.update { it.copy(snackbarMessage = "System clipboard cleared") }
    }

    fun clearHistory(keepFavorites: Boolean) {
        if (keepFavorites) {
            clearNonFavorites()
        } else {
            clearAllHistory()
        }
    }

    fun updatePreferences(
        autoCapture: Boolean? = null,
        duplicateHandling: String? = null,
        retentionDays: Int? = null,
        sensitiveHandling: String? = null,
        otpMinutes: Int? = null,
        appLock: Boolean? = null,
        biometric: Boolean? = null,
        screenSecurity: Boolean? = null,
        pin: String? = null,
        compact: Boolean? = null,
        excludedPackages: Set<String>? = null,
        themeMode: String? = null,
        maxHistoryCapacity: String? = null
    ) {
        autoCapture?.let {
            preferences.isAutoCaptureEnabled = it
            _overlayState.update { state -> state.copy(isMonitoringActive = it) }
        }
        duplicateHandling?.let { preferences.duplicateHandling = it }
        retentionDays?.let { preferences.retentionDays = it }
        maxHistoryCapacity?.let {
            preferences.maxHistoryCapacity = it
            viewModelScope.launch {
                repository.enforceMaxCapacity()
            }
        }
        sensitiveHandling?.let { preferences.sensitiveHandling = it }
        otpMinutes?.let { preferences.otpAutoDeleteMinutes = it }
        appLock?.let {
            preferences.isAppLockEnabled = it
            _overlayState.update { state -> state.copy(isAppLocked = it) }
        }
        biometric?.let { preferences.isBiometricEnabled = it }
        screenSecurity?.let {
            preferences.isSecureScreensEnabled = it
            _overlayState.update { state -> state.copy(isSecureScreensEnabled = it) }
        }
        pin?.let {
            if (it.length == 4 && it.all { ch -> ch.isDigit() }) {
                preferences.setPin(it)
                _overlayState.update { state -> state.copy(isPinSet = true) }
            }
        }
        compact?.let { preferences.isCompactView = it }
        excludedPackages?.let { preferences.excludedPackages = it }
        themeMode?.let {
            preferences.themeMode = it
            _overlayState.update { state -> state.copy(themeMode = it) }
        }
        _overlayState.update { it.copy(snackbarMessage = "Settings updated") }
    }

    fun setThemeMode(mode: String) {
        preferences.themeMode = mode
        _overlayState.update { it.copy(themeMode = mode) }
    }

    fun setStartupError(error: String?) {
        _overlayState.update { it.copy(startupError = error) }
    }

    fun retryStartup() {
        _overlayState.update { it.copy(startupError = null) }
        viewModelScope.launch {
            try {
                repository.seedSampleDataIfEmpty()
                repository.cleanupExpired()
            } catch (e: Exception) {
                _overlayState.update { it.copy(startupError = "Initialization failed. Check logs.") }
            }
        }
    }

    fun showDetails(item: ClipboardItem?) {
        _overlayState.update { it.copy(selectedItemForDetails = item) }
    }

    fun showEdit(item: ClipboardItem?) {
        _overlayState.update { it.copy(itemToEdit = item) }
    }

    fun showCollectionPicker(item: ClipboardItem?) {
        _overlayState.update { it.copy(itemForCollection = item) }
    }

    fun setShowAddDialog(show: Boolean) {
        _overlayState.update { it.copy(showAddDialog = show) }
    }

    fun setShowExportDialog(show: Boolean) {
        _overlayState.update { it.copy(showExportDialog = show) }
    }

    fun setShowImportDialog(show: Boolean) {
        _overlayState.update { it.copy(showImportDialog = show) }
    }

    fun setShowNewCollectionDialog(show: Boolean) {
        _overlayState.update { it.copy(showNewCollectionDialog = show) }
    }

    fun clearSnackbar() {
        _overlayState.update { it.copy(snackbarMessage = null) }
    }

    companion object {
        fun provideFactory(
            repository: ClipboardRepository,
            preferences: SecurityPreferences
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, preferences) as T
            }
        }
    }
}
