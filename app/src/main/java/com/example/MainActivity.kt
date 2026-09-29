package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.ClipboardMonitorService
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.components.AppLockOverlay
import com.example.ui.dialogs.AddClipDialog
import com.example.ui.dialogs.CollectionPickerDialog
import com.example.ui.dialogs.EditClipDialog
import com.example.ui.dialogs.ExportBackupDialog
import com.example.ui.dialogs.ImportBackupDialog
import com.example.ui.dialogs.ItemDetailsDialog
import com.example.ui.dialogs.NewCollectionDialog
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HappyOrange
import com.example.ui.theme.HappyTeal600
import com.example.ui.theme.HappyTeal900
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as HappyClipboardApp
        MainViewModel.provideFactory(app.repository, app.preferences)
    }

    private var clipboardManager: ClipboardManager? = null
    private var lastObservedClip: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkAndCaptureCurrentClip()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("HappyClipboard", "MAIN_ACTIVITY_CREATED")

        val app = application as HappyClipboardApp
        updateSecureScreenState(app.preferences.isSecureScreensEnabled)

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

        // Handle Share to Happy Clipboard Intent
        handleIncomingIntent(intent)

        Log.d("HappyClipboard", "SET_CONTENT_STARTED")
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isSecureScreensEnabled) {
                updateSecureScreenState(uiState.isSecureScreensEnabled)
            }

            MyApplicationTheme(themeMode = uiState.themeMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    private fun updateSecureScreenState(enabled: Boolean) {
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu")

        if (enabled && !isEmulator) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val app = application as HappyClipboardApp
                app.applicationScope.launch {
                    app.repository.captureClip(sharedText, sourceApp = "Share Sheet")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
        checkAndCaptureCurrentClip()
    }

    override fun onPause() {
        super.onPause()
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
    }

    private fun checkAndCaptureCurrentClip() {
        val app = application as HappyClipboardApp
        if (!app.preferences.isAutoCaptureEnabled) return

        try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: clip.getItemAt(0).coerceToText(this)?.toString()
                if (!text.isNullOrBlank() && text != lastObservedClip) {
                    lastObservedClip = text
                    app.applicationScope.launch {
                        app.repository.captureClip(text, sourceApp = "System Clipboard")
                    }
                }
            }
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    Log.d("HappyClipboard", "ROOT_COMPOSABLE_ENTERED")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Show snackbar messages from ViewModel
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Visible Startup Error Boundary (Debug Only)
    if (uiState.startupError != null && BuildConfig.DEBUG) {
        StartupErrorScreen(
            errorMessage = uiState.startupError ?: "Initialization failed.",
            onRetry = { viewModel.retryStartup() }
        )
        return
    }

    // App Lock Guard
    if (uiState.isAppLocked) {
        AppLockOverlay(
            isPinSet = uiState.isPinSet,
            onUnlock = { pin -> viewModel.unlockApp(pin) },
            onSetPin = { pin, confirm -> viewModel.setAppPin(pin, confirm) }
        )
        return
    }

    Log.d("HappyClipboard", "NAVIGATION_ENTERED")
    var showMoreMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.5.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Happy Clipboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Offline & Encrypted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Search Action (navigates to History search)
                        IconButton(
                            onClick = { viewModel.setTab(NavigationTab.HISTORY) },
                            modifier = Modifier.testTag("top_search_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search clips",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // More Menu Action
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("top_more_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (uiState.isMonitoringActive) "Pause Monitoring" else "Resume Monitoring")
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (uiState.isMonitoringActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                                                    CircleShape
                                                )
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.toggleMonitoring()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Paste from Clipboard") },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.pasteFromClipboard(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export Backup") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.setShowExportDialog(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.setTab(NavigationTab.SETTINGS)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.HISTORY,
                        onClick = { viewModel.setTab(NavigationTab.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontWeight = if (uiState.selectedTab == NavigationTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tab_history")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.CALENDAR,
                        onClick = { viewModel.setTab(NavigationTab.CALENDAR) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                        label = { Text("Calendar", fontWeight = if (uiState.selectedTab == NavigationTab.CALENDAR) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tab_calendar")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.COLLECTIONS,
                        onClick = { viewModel.setTab(NavigationTab.COLLECTIONS) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Collections") },
                        label = { Text("Collections", fontWeight = if (uiState.selectedTab == NavigationTab.COLLECTIONS) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tab_collections")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.FAVORITES,
                        onClick = { viewModel.setTab(NavigationTab.FAVORITES) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites", fontWeight = if (uiState.selectedTab == NavigationTab.FAVORITES) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tab_favorites")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == NavigationTab.SETTINGS,
                        onClick = { viewModel.setTab(NavigationTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontWeight = if (uiState.selectedTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tab_settings")
                    )
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab == NavigationTab.HISTORY || uiState.selectedTab == NavigationTab.COLLECTIONS) {
                FloatingActionButton(
                    onClick = { viewModel.setShowAddDialog(true) },
                    containerColor = HappyOrange,
                    contentColor = Color(0xFF261200),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_clip_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Clipboard Item", modifier = Modifier.size(24.dp))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = uiState.selectedTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    NavigationTab.HISTORY -> HomeScreen(uiState = uiState, viewModel = viewModel)
                    NavigationTab.CALENDAR -> CalendarScreen(uiState = uiState, viewModel = viewModel)
                    NavigationTab.COLLECTIONS -> CollectionsScreen(uiState = uiState, viewModel = viewModel)
                    NavigationTab.FAVORITES -> FavoritesScreen(uiState = uiState, viewModel = viewModel)
                    NavigationTab.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }

    // Dialog Hosts
    if (uiState.selectedItemForDetails != null) {
        ItemDetailsDialog(
            item = uiState.selectedItemForDetails!!,
            onDismiss = { viewModel.showDetails(null) },
            onCopy = { viewModel.copyItemToClipboard(context, uiState.selectedItemForDetails!!) },
            onEdit = { viewModel.showEdit(uiState.selectedItemForDetails) },
            onAddToCollection = { viewModel.showCollectionPicker(uiState.selectedItemForDetails) },
            onDelete = { viewModel.deleteItem(uiState.selectedItemForDetails!!.id) }
        )
    }

    if (uiState.itemToEdit != null) {
        EditClipDialog(
            item = uiState.itemToEdit!!,
            onDismiss = { viewModel.showEdit(null) },
            onSave = { content, notes ->
                viewModel.updateItem(uiState.itemToEdit!!.id, content, notes)
            }
        )
    }

    if (uiState.itemForCollection != null) {
        CollectionPickerDialog(
            item = uiState.itemForCollection!!,
            collections = uiState.collections,
            onDismiss = { viewModel.showCollectionPicker(null) },
            onSelectCollection = { col ->
                viewModel.setItemCollection(uiState.itemForCollection!!.id, col)
            },
            onCreateNewCollection = {
                viewModel.setShowNewCollectionDialog(true)
            }
        )
    }

    if (uiState.showAddDialog) {
        AddClipDialog(
            onDismiss = { viewModel.setShowAddDialog(false) },
            onAdd = { content, notes ->
                viewModel.addNewClip(content, notes)
            }
        )
    }

    if (uiState.showNewCollectionDialog) {
        NewCollectionDialog(
            onDismiss = { viewModel.setShowNewCollectionDialog(false) },
            onCreate = { name, iconKey, colorHex ->
                viewModel.createCollection(name, iconKey, colorHex)
            }
        )
    }

    if (uiState.showExportDialog) {
        ExportBackupDialog(
            itemCount = uiState.items.size,
            onDismiss = { viewModel.setShowExportDialog(false) },
            onExport = { pass -> viewModel.exportBackup(pass) }
        )
    }

    if (uiState.showImportDialog) {
        ImportBackupDialog(
            onDismiss = { viewModel.setShowImportDialog(false) },
            onImport = { content, pass, mode ->
                viewModel.importBackup(content, pass, mode)
            }
        )
    }
}

@Composable
fun StartupErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = RoundedCornerShape(20.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(cardShape)
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), cardShape),
                shape = cardShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Startup Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Happy Clipboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Startup Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Check Logcat for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
