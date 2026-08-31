package com.agnocode.minimalhomeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
    MinimalHomeAppTheme(fontFamilyName = mainViewModel.fontFamily.value) {
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
    val context = android.view.ContextThemeWrapper(androidx.compose.ui.platform.LocalContext.current, 0)

    DisposableEffect(context) {
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
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    LaunchedEffect(mainViewModel.resetToHomeEvent) {
        mainViewModel.resetToHomeEvent.collect {
            pagerState.animateScrollToPage(1)
            mainViewModel.isUniversalSearchActive.value = false
            mainViewModel.universalSearchQuery.value = ""
        }
    }

    val activeFocusMode = focusModeViewModel.focusModes.find { it.name == focusModeViewModel.activeFocusModeName.value }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> NotesView(
                date = notesViewModel.selectedNoteDate.value,
                availableDates = notesViewModel.getAvailableDates(),
                noteText = notesViewModel.currentNoteText.value,
                tasks = notesViewModel.currentTasks,
                onDateSelect = { notesViewModel.selectNoteDate(it) },
                onNoteTextChange = { notesViewModel.updateNoteText(it) },
                onAddTask = { notesViewModel.addTask() },
                onUpdateTaskText = { id, text -> notesViewModel.updateTaskText(id, text) },
                onToggleTask = { id, checked -> notesViewModel.toggleTask(id, checked) },
                onDeleteTask = { notesViewModel.deleteTask(it) }
            )
            1 -> HomeView(
                favorites = appDrawerViewModel.getVisibleApps(activeFocusMode?.allowedPackages).filter { it.packageName in appDrawerViewModel.favoritePackages },
                searchResults = appDrawerViewModel.apps.filter { it.label.contains(mainViewModel.universalSearchQuery.value, ignoreCase = true) && it.packageName !in focusModeViewModel.blockedApps }.take(5),
                isSearchActive = mainViewModel.isUniversalSearchActive.value,
                searchQuery = mainViewModel.universalSearchQuery.value,
                tasksCount = notesViewModel.currentTasks.count { !it.isChecked },
                onSearchQueryChange = { mainViewModel.universalSearchQuery.value = it },
                onSearchToggle = { mainViewModel.isUniversalSearchActive.value = it },
                onRemoveFavorite = { appDrawerViewModel.toggleFavorite(it) },
                onBlock = { pkg, expiry -> focusModeViewModel.blockApp(pkg, expiry) },
                showIcons = appDrawerViewModel.showIcons.value,
                iconPackPackage = appDrawerViewModel.iconPackPackage.value
            )
            2 -> AppDrawerView(
                apps = appDrawerViewModel.getVisibleApps(activeFocusMode?.allowedPackages),
                searchQuery = appDrawerViewModel.searchQuery.value,
                isRefreshing = appDrawerViewModel.isRefreshing.value,
                onRefresh = { appDrawerViewModel.refreshApps() },
                onSearchQueryChange = { appDrawerViewModel.searchQuery.value = it },
                onOpenSettings = { showSettings = true },
                onToggleFavorite = { appDrawerViewModel.toggleFavorite(it) },
                onBlock = { pkg, expiry -> focusModeViewModel.blockApp(pkg, expiry) },
                isFavorite = { appDrawerViewModel.favoritePackages.contains(it) },
                showIcons = appDrawerViewModel.showIcons.value,
                iconPackPackage = appDrawerViewModel.iconPackPackage.value
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            blockedApps = focusModeViewModel.blockedApps,
            allApps = appDrawerViewModel.apps,
            onUnblock = { focusModeViewModel.unblockApp(it) },
            focusModes = focusModeViewModel.focusModes,
            activeFocusModeName = focusModeViewModel.activeFocusModeName.value,
            onToggleFocusMode = { focusModeViewModel.toggleFocusMode(it) },
            onAddFocusMode = { name, pkgs, start, end, oldName -> focusModeViewModel.addFocusMode(name, pkgs, start, end, oldName) },
            onDeleteFocusMode = { focusModeViewModel.deleteFocusMode(it) },
            fontFamily = mainViewModel.fontFamily.value,
            onSetFontFamily = { mainViewModel.setFontFamily(it) },
            showIcons = appDrawerViewModel.showIcons.value,
            onSetShowIcons = { appDrawerViewModel.setShowIcons(it) },
            availableIconPacks = appDrawerViewModel.availableIconPacks,
            selectedIconPack = appDrawerViewModel.iconPackPackage.value,
            onSetIconPack = { appDrawerViewModel.setIconPack(it) }
        )
    }
}
