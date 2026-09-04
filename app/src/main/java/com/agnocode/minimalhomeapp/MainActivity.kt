package com.agnocode.minimalhomeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agnocode.minimalhomeapp.ui.AppDrawerViewModel
import com.agnocode.minimalhomeapp.ui.FocusModeViewModel
import com.agnocode.minimalhomeapp.ui.MainViewModel
import com.agnocode.minimalhomeapp.ui.NotesViewModel
import com.agnocode.minimalhomeapp.ui.components.AppDrawerView
import com.agnocode.minimalhomeapp.ui.components.HomeView
import com.agnocode.minimalhomeapp.ui.components.NotesView
import com.agnocode.minimalhomeapp.ui.components.SettingsDialog
import com.agnocode.minimalhomeapp.ui.theme.MinimalHomeAppTheme
import com.agnocode.minimalhomeapp.util.BiometricHelper
import com.agnocode.minimalhomeapp.util.isFuzzyMatch
import dagger.hilt.android.AndroidEntryPoint
import android.net.Uri
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            mainViewModel = viewModel()
            val notesViewModel: NotesViewModel = viewModel()
            val appDrawerViewModel: AppDrawerViewModel = viewModel()
            val focusModeViewModel: FocusModeViewModel = viewModel()
            
            HomeScreenContainer(
                mainViewModel,
                notesViewModel,
                appDrawerViewModel,
                focusModeViewModel
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (::mainViewModel.isInitialized) {
                mainViewModel.triggerResetToHome()
            }
        }
    }
}

@Composable
fun HomeScreenContainer(
    mainViewModel: MainViewModel,
    notesViewModel: NotesViewModel,
    appDrawerViewModel: AppDrawerViewModel,
    focusModeViewModel: FocusModeViewModel
) {
    val accentColor by mainViewModel.accentColorFlow.collectAsStateWithLifecycle()

    MinimalHomeAppTheme(accentColorName = accentColor) {
        Surface(color = Color.Black) {
            HomeScreen(
                mainViewModel,
                notesViewModel,
                appDrawerViewModel,
                focusModeViewModel
            )
        }
    }
}

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    notesViewModel: NotesViewModel,
    appDrawerViewModel: AppDrawerViewModel,
    focusModeViewModel: FocusModeViewModel
) {
    val context = LocalContext.current
    val themeContext = android.view.ContextThemeWrapper(context, 0)
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(themeContext) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("HomeApp", "Package change detected: ${intent?.action}")
                appDrawerViewModel.refreshApps()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            addDataScheme("package")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            themeContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            themeContext.registerReceiver(receiver, filter)
        }

        onDispose {
            themeContext.unregisterReceiver(receiver)
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    BackHandler(enabled = (pagerState.currentPage != 1 || showSettings) && !mainViewModel.isUniversalSearchActive.value) {
        if (showSettings) {
            showSettings = false
        } else if (pagerState.currentPage != 1) {
            scope.launch {
                pagerState.animateScrollToPage(1)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            keyboardController?.hide()
        }
        if (pagerState.currentPage != 0) {
            notesViewModel.resetToToday()
        }
    }

    LaunchedEffect(mainViewModel.resetToHomeEvent) {
        mainViewModel.resetToHomeEvent.collect {
            pagerState.animateScrollToPage(1)
            mainViewModel.isUniversalSearchActive.value = false
            mainViewModel.universalSearchQuery.value = ""
        }
    }

    val visibleApps by appDrawerViewModel.visibleAppsFlow.collectAsStateWithLifecycle()
    val favorites by appDrawerViewModel.favoritesFlow.collectAsStateWithLifecycle()
    val searchQuery by appDrawerViewModel.searchQuery.collectAsStateWithLifecycle()
    val universalSearchQuery by mainViewModel.universalSearchQuery.collectAsStateWithLifecycle()
    val allApps by appDrawerViewModel.apps.collectAsStateWithLifecycle()
    val blockedApps by focusModeViewModel.blockedAppsFlow.collectAsStateWithLifecycle()
    val protectedPackages by focusModeViewModel.protectedPackagesFlow.collectAsStateWithLifecycle()
    val ghostPackages by appDrawerViewModel.ghostPackagesFlow.collectAsStateWithLifecycle()
    val usageStats by appDrawerViewModel.usageStats.collectAsStateWithLifecycle()
    val usageMode by appDrawerViewModel.usageAwarenessModeFlow.collectAsStateWithLifecycle()
    val monochromeIcons by mainViewModel.monochromeIconsFlow.collectAsStateWithLifecycle()
    val noteResults by mainViewModel.noteSearchResults.collectAsStateWithLifecycle()
    val smartActionHome by mainViewModel.smartAction.collectAsStateWithLifecycle()
    val smartActionDrawer by appDrawerViewModel.smartAction.collectAsStateWithLifecycle()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = mainViewModel.exportBackup()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    }
                    if (json != null) {
                        val success = mainViewModel.importBackup(json)
                        if (success) {
                            Toast.makeText(context, "Restore successful", Toast.LENGTH_SHORT).show()
                            // No need to restart as state flows are reactive
                        } else {
                            Toast.makeText(context, "Restore failed: Invalid file", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val syncDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Take persistent permission so we can write to this file in the background
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                mainViewModel.setAutoSyncUri(it.toString())
                Toast.makeText(context, "Auto-sync location set", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to set sync location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun authenticate(onSuccess: () -> Unit) {
        if (BiometricHelper.canAuthenticate(context as FragmentActivity)) {
            BiometricHelper.authenticate(
                activity = context,
                title = "Authentication Required",
                subtitle = "Please authenticate to proceed",
                onSuccess = onSuccess
            )
        } else {
            // Fallback for devices without biometric security
            onSuccess()
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> NotesView(
                date = notesViewModel.selectedNoteDate.value,
                noteText = notesViewModel.currentNoteText.value,
                tasks = notesViewModel.currentTasks,
                weeklyProductivity = notesViewModel.weeklyProductivity.value,
                allDailyNotes = notesViewModel.allDailyNotes,
                isEditingPastNote = notesViewModel.isEditingPastNote.value,
                onDateSelect = { notesViewModel.selectNoteDate(it) },
                onNoteTextChange = { notesViewModel.updateNoteText(it) },
                onAddTask = { notesViewModel.addTask() },
                onUpdateTaskText = { id, text -> notesViewModel.updateTaskText(id, text) },
                onToggleTask = { id, checked -> notesViewModel.toggleTask(id, checked) },
                onDeleteTask = { notesViewModel.deleteTask(it) },
                onToggleEditPastNote = { notesViewModel.unlockPastNote() },
                onSavePastNote = { notesViewModel.saveAndLockPastNote() },
                onUndoPastNote = { notesViewModel.undoAndLockPastNote() }
            )
            1 -> {
                val results = allApps.filter { it.label.isFuzzyMatch(universalSearchQuery) && it.packageName !in blockedApps.keys }.take(5)
                HomeView(
                    favorites = favorites,
                    searchResults = results,
                    isSearchActive = mainViewModel.isUniversalSearchActive.value,
                    searchQuery = universalSearchQuery,
                    tasksCount = notesViewModel.currentTasks.count { !it.isChecked },
                    isVisible = pagerState.currentPage == 1,
                    showFavorites = mainViewModel.showFavorites.value,
                    usageStats = usageStats,
                    usageMode = usageMode,
                    isMonochrome = monochromeIcons,
                    smartAction = smartActionHome,
                    noteResults = noteResults,
                    onNoteClick = { notesViewModel.selectNoteDate(it) },
                    onSearchQueryChange = { mainViewModel.universalSearchQuery.value = it },
                    onSearchToggle = { mainViewModel.isUniversalSearchActive.value = it },
                    onRemoveFavorite = { appDrawerViewModel.toggleFavorite(it) },
                    onBlock = { pkg, expiry -> focusModeViewModel.blockApp(pkg, expiry) },
                    onTasksClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    protectedPackages = protectedPackages,
                    onToggleProtected = { focusModeViewModel.toggleProtectedPackage(it) },
                    onProtectedLaunch = { authenticate(it) },
                    getIcon = { appDrawerViewModel.getIcon(it) },
                    onSearch = {
                        if (results.isNotEmpty()) {
                            val intent = context.packageManager.getLaunchIntentForPackage(results[0].packageName)
                            intent?.let { context.startActivity(it) }
                            mainViewModel.isUniversalSearchActive.value = false
                            mainViewModel.universalSearchQuery.value = ""
                        } else if (universalSearchQuery.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com/search?q=$universalSearchQuery"))
                            context.startActivity(intent)
                            mainViewModel.isUniversalSearchActive.value = false
                            mainViewModel.universalSearchQuery.value = ""
                        }
                    },
                    showIcons = appDrawerViewModel.showIcons.value,
                    iconPackPackage = appDrawerViewModel.iconPackPackage.value
                )
            }
            2 -> AppDrawerView(
                apps = visibleApps,
                searchQuery = searchQuery,
                isRefreshing = appDrawerViewModel.isRefreshing.value,
                onRefresh = { appDrawerViewModel.refreshApps() },
                onSearchQueryChange = { appDrawerViewModel.searchQuery.value = it },
                onOpenSettings = { showSettings = true },
                onToggleFavorite = { appDrawerViewModel.toggleFavorite(it) },
                onBlock = { pkg, expiry -> focusModeViewModel.blockApp(pkg, expiry) },
                isFavorite = { appDrawerViewModel.favoritePackages.contains(it) },
                isProtected = { protectedPackages.contains(it) },
                onToggleProtected = { focusModeViewModel.toggleProtectedPackage(it) },
                onProtectedLaunch = { authenticate(it) },
                isGhost = { ghostPackages.contains(it) },
                onToggleGhost = { appDrawerViewModel.toggleGhost(it) },
                getIcon = { appDrawerViewModel.getIcon(it) },
                usageStats = usageStats,
                usageMode = usageMode,
                isMonochrome = monochromeIcons,
                smartAction = smartActionDrawer,
                onSearch = {
                    if (visibleApps.isNotEmpty()) {
                        val intent = context.packageManager.getLaunchIntentForPackage(visibleApps[0].packageName)
                        intent?.let { context.startActivity(it) }
                        appDrawerViewModel.searchQuery.value = ""
                    }
                },
                showIcons = appDrawerViewModel.showIcons.value,
                iconPackPackage = appDrawerViewModel.iconPackPackage.value
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            blockedApps = blockedApps,
            allApps = allApps,
            onUnblock = { focusModeViewModel.unblockApp(it) },
            focusModes = focusModeViewModel.focusModes,
            activeFocusModeName = focusModeViewModel.activeFocusModeName.value,
            onToggleFocusMode = { name ->
                if (name == null && focusModeViewModel.activeFocusModeName.value != null && focusModeViewModel.biometricFocusLock.value && notesViewModel.hasIncompleteTasks.value) {
                    authenticate { focusModeViewModel.toggleFocusMode(null) }
                } else {
                    focusModeViewModel.toggleFocusMode(name)
                }
            },
            onAddFocusMode = { name, pkgs, start, end, oldName -> focusModeViewModel.addFocusMode(name, pkgs, start, end, oldName) },
            onDeleteFocusMode = { focusModeViewModel.deleteFocusMode(it) },
            showIcons = appDrawerViewModel.showIcons.value,
            onSetShowIcons = { appDrawerViewModel.setShowIcons(it) },
            showFavorites = mainViewModel.showFavorites.value,
            onSetShowFavorites = { mainViewModel.setShowFavorites(it) },
            dndSyncEnabled = focusModeViewModel.dndSyncEnabled.value,
            onSetDndSyncEnabled = { focusModeViewModel.setDndSyncEnabled(it) },
            hasDndPermission = { focusModeViewModel.hasDndPermission() },
            biometricFocusLock = focusModeViewModel.biometricFocusLock.value,
            onSetBiometricFocusLock = { focusModeViewModel.setBiometricFocusLock(it) },
            availableIconPacks = appDrawerViewModel.availableIconPacks,
            selectedIconPack = appDrawerViewModel.iconPackPackage.value,
            onSetIconPack = { appDrawerViewModel.setIconPack(it) },
            usageAwarenessMode = usageMode,
            onSetUsageAwarenessMode = { appDrawerViewModel.setUsageAwarenessMode(it) },
            monochromeIcons = monochromeIcons,
            onSetMonochromeIcons = { mainViewModel.setMonochromeIcons(it) },
            accentColor = mainViewModel.accentColor.value,
            onSetAccentColor = { mainViewModel.setAccentColor(it) },
            ghostApps = allApps.filter { ghostPackages.contains(it.packageName) },
            onRemoveGhost = { appDrawerViewModel.toggleGhost(it) },
            onBackup = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                createDocumentLauncher.launch("minimal_home_backup_$timestamp.json")
            },
            onRestore = {
                openDocumentLauncher.launch(arrayOf("application/json"))
            },
            autoSyncEnabled = mainViewModel.autoSyncEnabled.value,
            onSetAutoSyncEnabled = { mainViewModel.setAutoSyncEnabled(it) },
            autoSyncUri = mainViewModel.autoSyncUri.value,
            onSelectSyncFile = {
                syncDocumentLauncher.launch(arrayOf("application/json"))
            }
        )
    }
}
