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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agnocode.minimalhomeapp.ui.HomeViewModel
import com.agnocode.minimalhomeapp.ui.components.AppDrawerView
import com.agnocode.minimalhomeapp.ui.components.HomeView
import com.agnocode.minimalhomeapp.ui.components.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = Color.Black)) {
                Surface(color = Color.Black) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
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
    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HomeView(
                favorites = viewModel.getFavorites(),
                onRemoveFavorite = { viewModel.removeFavorite(it) },
                onBlock = { pkg, expiry -> viewModel.blockApp(pkg, expiry) }
            )
            1 -> AppDrawerView(
                apps = viewModel.getVisibleApps(),
                searchQuery = viewModel.searchQuery.value,
                onSearchQueryChange = { viewModel.searchQuery.value = it },
                onOpenSettings = { showSettings = true },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onBlock = { pkg, expiry -> viewModel.blockApp(pkg, expiry) },
                isFavorite = { viewModel.favoritePackages.contains(it) }
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            blockedApps = viewModel.blockedApps,
            allApps = viewModel.apps,
            onUnblock = { viewModel.unblockApp(it) }
        )
    }
}
