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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agnocode.minimalhomeapp.ui.HomeViewModel
import com.agnocode.minimalhomeapp.ui.components.AppDrawerView
import com.agnocode.minimalhomeapp.ui.components.HomeView
import com.agnocode.minimalhomeapp.ui.components.NotesView
import com.agnocode.minimalhomeapp.ui.components.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            viewModel = viewModel()
            HomeScreenContainer(viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (::viewModel.isInitialized) {
                viewModel.triggerResetToHome()
            }
        }
    }
}

@Composable
fun HomeScreenContainer(viewModel: HomeViewModel) {
    val fontFamily = when (viewModel.fontFamily.value) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "sans-serif" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    val typography = Typography(
        bodyLarge = TextStyle(fontFamily = fontFamily),
        bodyMedium = TextStyle(fontFamily = fontFamily),
        bodySmall = TextStyle(fontFamily = fontFamily),
        titleLarge = TextStyle(fontFamily = fontFamily),
        titleMedium = TextStyle(fontFamily = fontFamily),
        titleSmall = TextStyle(fontFamily = fontFamily),
        labelLarge = TextStyle(fontFamily = fontFamily),
        labelMedium = TextStyle(fontFamily = fontFamily),
        labelSmall = TextStyle(fontFamily = fontFamily),
        displayLarge = TextStyle(fontFamily = fontFamily),
        displayMedium = TextStyle(fontFamily = fontFamily),
        displaySmall = TextStyle(fontFamily = fontFamily),
        headlineLarge = TextStyle(fontFamily = fontFamily),
        headlineMedium = TextStyle(fontFamily = fontFamily),
        headlineSmall = TextStyle(fontFamily = fontFamily)
    )

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(background = Color.Black),
        typography = typography
    ) {
        Surface(color = Color.Black) {
            HomeScreen(viewModel)
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = android.view.ContextThemeWrapper(androidx.compose.ui.platform.LocalContext.current, 0)

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("HomeApp", "Package change detected: ${intent?.action}")
                viewModel.refreshApps()
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

    LaunchedEffect(viewModel.resetToHomeEvent) {
        viewModel.resetToHomeEvent.collect {
            pagerState.animateScrollToPage(1)
            viewModel.isUniversalSearchActive.value = false
            viewModel.universalSearchQuery.value = ""
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> NotesView(
                date = viewModel.selectedNoteDate.value,
                availableDates = viewModel.getAvailableDates(),
                noteText = viewModel.currentNoteText.value,
                tasks = viewModel.currentTasks,
                onDateSelect = { viewModel.selectNoteDate(it) },
                onNoteTextChange = { viewModel.updateNoteText(it) },
                onAddTask = { viewModel.addTask() },
                onUpdateTaskText = { id, text -> viewModel.updateTaskText(id, text) },
                onToggleTask = { id, checked -> viewModel.toggleTask(id, checked) },
                onDeleteTask = { viewModel.deleteTask(it) }
            )
            1 -> HomeView(
                favorites = viewModel.getFavorites(),
                searchResults = viewModel.getUniversalSearchResults(),
                isSearchActive = viewModel.isUniversalSearchActive.value,
                searchQuery = viewModel.universalSearchQuery.value,
                tasksCount = viewModel.currentTasks.count { !it.isChecked },
                onSearchQueryChange = { viewModel.universalSearchQuery.value = it },
                onSearchToggle = { viewModel.isUniversalSearchActive.value = it },
                onRemoveFavorite = { viewModel.removeFavorite(it) },
                onBlock = { pkg, expiry -> viewModel.blockApp(pkg, expiry) },
                showIcons = viewModel.showIcons.value,
                iconPackPackage = viewModel.iconPackPackage.value
            )
            2 -> AppDrawerView(
                apps = viewModel.getVisibleApps(),
                searchQuery = viewModel.searchQuery.value,
                isRefreshing = viewModel.isRefreshing.value,
                onRefresh = { viewModel.refreshApps() },
                onSearchQueryChange = { viewModel.searchQuery.value = it },
                onOpenSettings = { showSettings = true },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onBlock = { pkg, expiry -> viewModel.blockApp(pkg, expiry) },
                isFavorite = { viewModel.favoritePackages.contains(it) },
                showIcons = viewModel.showIcons.value,
                iconPackPackage = viewModel.iconPackPackage.value
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            blockedApps = viewModel.blockedApps,
            allApps = viewModel.apps,
            onUnblock = { viewModel.unblockApp(it) },
            focusModes = viewModel.focusModes,
            activeFocusModeName = viewModel.activeFocusModeName.value,
            onToggleFocusMode = { viewModel.toggleFocusMode(it) },
            onAddFocusMode = { name, pkgs, start, end -> viewModel.addFocusMode(name, pkgs, start, end) },
            onDeleteFocusMode = { viewModel.deleteFocusMode(it) },
            fontFamily = viewModel.fontFamily.value,
            onSetFontFamily = { viewModel.setFontFamily(it) },
            showIcons = viewModel.showIcons.value,
            onSetShowIcons = { viewModel.setShowIcons(it) },
            availableIconPacks = viewModel.availableIconPacks,
            selectedIconPack = viewModel.iconPackPackage.value,
            onSetIconPack = { viewModel.setIconPack(it) }
        )
    }
}
