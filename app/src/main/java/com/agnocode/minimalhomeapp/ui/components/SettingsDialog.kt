package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import com.agnocode.minimalhomeapp.R
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.data.model.FocusMode
import com.agnocode.minimalhomeapp.ui.components.settings.*

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    blockedApps: Map<String, Long?>,
    allApps: List<AppItem>,
    onUnblock: (String) -> Unit,
    focusModes: List<FocusMode>,
    activeFocusModeName: String?,
    onToggleFocusMode: (String?) -> Unit,
    onAddFocusMode: (String, Set<String>, Int?, Int?, String?) -> Unit,
    onDeleteFocusMode: (String) -> Unit,
    showIcons: Boolean,
    onSetShowIcons: (Boolean) -> Unit,
    showFavorites: Boolean,
    onSetShowFavorites: (Boolean) -> Unit,
    dndSyncEnabled: Boolean,
    onSetDndSyncEnabled: (Boolean) -> Unit,
    hasDndPermission: () -> Boolean,
    biometricFocusLock: Boolean,
    onSetBiometricFocusLock: (Boolean) -> Unit,
    availableIconPacks: List<AppItem>,
    selectedIconPack: String?,
    onSetIconPack: (String?) -> Unit,
    usageAwarenessMode: String,
    onSetUsageAwarenessMode: (String) -> Unit,
    monochromeIcons: Boolean,
    onSetMonochromeIcons: (Boolean) -> Unit,
    accentColor: String,
    onSetAccentColor: (String) -> Unit,
    hasUsageStatsPermission: () -> Boolean,
    ghostApps: List<AppItem>,
    onRemoveGhost: (String) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    autoSyncEnabled: Boolean,
    onSetAutoSyncEnabled: (Boolean) -> Unit,
    autoSyncUri: String?,
    onSelectSyncFile: () -> Unit
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun checkDefault() {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolvedPackage = resolveInfo?.activityInfo?.packageName
        isDefault = resolvedPackage == context.packageName
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkDefault()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        checkDefault()
    }

    var expandedSection by remember(isDefault) { mutableStateOf(if (isDefault) "" else "system") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    SystemSettings(
                        context = context,
                        isDefault = isDefault,
                        isExpanded = expandedSection == "system",
                        onToggle = { expandedSection = if (expandedSection == "system") "" else "system" }
                    )
                }

                item { SettingsDivider() }

                item {
                    CustomizationSettings(
                        context = context,
                        isExpanded = expandedSection == "customization",
                        onToggle = { expandedSection = if (expandedSection == "customization") "" else "customization" },
                        showIcons = showIcons,
                        onSetShowIcons = onSetShowIcons,
                        showFavorites = showFavorites,
                        onSetShowFavorites = onSetShowFavorites,
                        usageAwarenessMode = usageAwarenessMode,
                        onSetUsageAwarenessMode = onSetUsageAwarenessMode,
                        monochromeIcons = monochromeIcons,
                        onSetMonochromeIcons = onSetMonochromeIcons,
                        accentColor = accentColor,
                        onSetAccentColor = onSetAccentColor,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        availableIconPacks = availableIconPacks,
                        selectedIconPack = selectedIconPack,
                        onSetIconPack = onSetIconPack
                    )
                }

                item { SettingsDivider() }

                item {
                    FocusSettings(
                        context = context,
                        isExpanded = expandedSection == "focus",
                        onToggle = { expandedSection = if (expandedSection == "focus") "" else "focus" },
                        focusModes = focusModes,
                        activeFocusModeName = activeFocusModeName,
                        onToggleFocusMode = onToggleFocusMode,
                        onAddFocusMode = onAddFocusMode,
                        onDeleteFocusMode = onDeleteFocusMode,
                        dndSyncEnabled = dndSyncEnabled,
                        onSetDndSyncEnabled = onSetDndSyncEnabled,
                        hasDndPermission = hasDndPermission,
                        allApps = allApps
                    )
                }

                item { SettingsDivider() }

                item {
                    SecuritySettings(
                        isExpanded = expandedSection == "security",
                        onToggle = { expandedSection = if (expandedSection == "security") "" else "security" },
                        biometricFocusLock = biometricFocusLock,
                        onSetBiometricFocusLock = onSetBiometricFocusLock,
                        ghostApps = ghostApps,
                        onRemoveGhost = onRemoveGhost,
                        blockedApps = blockedApps,
                        allApps = allApps,
                        onUnblock = onUnblock
                    )
                }

                item { SettingsDivider() }

                item {
                    BackupSettings(
                        isExpanded = expandedSection == "backup",
                        onToggle = { expandedSection = if (expandedSection == "backup") "" else "backup" },
                        onBackup = onBackup,
                        onRestore = onRestore,
                        autoSyncEnabled = autoSyncEnabled,
                        onSetAutoSyncEnabled = onSetAutoSyncEnabled,
                        autoSyncUri = autoSyncUri,
                        onSelectSyncFile = onSelectSyncFile
                    )
                }
            }
        }
    }
}
