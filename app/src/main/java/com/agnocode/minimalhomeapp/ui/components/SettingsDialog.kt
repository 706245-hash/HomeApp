package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.data.model.FocusMode

@OptIn(ExperimentalMaterial3Api::class)
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
    selectedWidget: String,
    onSetSelectedWidget: (String) -> Unit,
    dndSyncEnabled: Boolean,
    onSetDndSyncEnabled: (Boolean) -> Unit,
    hasDndPermission: () -> Boolean,
    biometricFocusLock: Boolean,
    onSetBiometricFocusLock: (Boolean) -> Unit,
    availableIconPacks: List<AppItem>,
    selectedIconPack: String?,
    onSetIconPack: (String?) -> Unit
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkDefault()
    }

    var showAddFocusMode by remember { mutableStateOf(false) }
    var editingMode by remember { mutableStateOf<FocusMode?>(null) }

    var systemExpanded by remember(isDefault) { mutableStateOf(!isDefault) }
    var customizationExpanded by remember { mutableStateOf(false) }
    var focusModesExpanded by remember { mutableStateOf(false) }
    var blockedAppsExpanded by remember { mutableStateOf(false) }

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
                    "Settings",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                // Section: System
                item {
                    SettingsSectionHeader(
                        title = "System",
                        isExpanded = systemExpanded,
                        onToggle = { systemExpanded = !systemExpanded }
                    )
                }

                if (systemExpanded) {
                    if (!isDefault) {
                        item {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text("Set as Default Launcher", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        item {
                            Text(
                                "App is set as default",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 32.dp, bottom = 16.dp)
                            )
                        }
                    }
                }

                item { Divider() }

                // Section: Customization
                item {
                    SettingsSectionHeader(
                        title = "Customization",
                        isExpanded = customizationExpanded,
                        onToggle = { customizationExpanded = !customizationExpanded }
                    )
                }

                if (customizationExpanded) {
                    item {
                        SettingsRow(label = "Show App Icons") {
                            Switch(
                                checked = showIcons,
                                onCheckedChange = onSetShowIcons,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }

                    if (showIcons) {
                        item {
                            SettingsRow(label = "Icon Pack") {
                                var showIconMenu by remember { mutableStateOf(false) }
                                Box {
                                    val label = availableIconPacks.find { it.packageName == selectedIconPack }?.label ?: "Default"
                                    Text(
                                        label,
                                        color = Color.LightGray,
                                        modifier = Modifier.clickable { showIconMenu = true }.padding(8.dp)
                                    )
                                    DropdownMenu(
                                        expanded = showIconMenu,
                                        onDismissRequest = { showIconMenu = false },
                                        modifier = Modifier
                                            .background(Color.Black)
                                            .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Default", color = Color.White) },
                                            onClick = {
                                                onSetIconPack(null)
                                                showIconMenu = false
                                            }
                                        )
                                        availableIconPacks.forEach { pack ->
                                            DropdownMenuItem(
                                                text = { Text(pack.label, color = Color.White) },
                                                onClick = {
                                                    onSetIconPack(pack.packageName)
                                                    showIconMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SettingsRow(label = "Show Favorites") {
                            Switch(
                                checked = showFavorites,
                                onCheckedChange = onSetShowFavorites,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }

                    item {
                        SettingsRow(label = "Home Widget") {
                            var showWidgetMenu by remember { mutableStateOf(false) }
                            Box {
                                val currentLabel = when (selectedWidget) {
                                    "battery" -> "Battery %"
                                    "alarm" -> "Next Alarm"
                                    "date" -> "Full Date"
                                    else -> "None"
                                }
                                Text(
                                    currentLabel,
                                    color = Color.LightGray,
                                    modifier = Modifier.clickable { showWidgetMenu = true }.padding(8.dp)
                                )
                                DropdownMenu(
                                    expanded = showWidgetMenu,
                                    onDismissRequest = { showWidgetMenu = false },
                                    modifier = Modifier
                                        .background(Color.Black)
                                        .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
                                ) {
                                    val options = listOf(
                                        "none" to "None",
                                        "battery" to "Battery %",
                                        "alarm" to "Next Alarm",
                                        "date" to "Full Date"
                                    )
                                    options.forEach { (id, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = Color.White) },
                                            onClick = {
                                                onSetSelectedWidget(id)
                                                showWidgetMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Divider() }

                // Section: Focus Modes
                item {
                    SettingsSectionHeader(
                        title = "Focus Modes",
                        isExpanded = focusModesExpanded,
                        onToggle = { focusModesExpanded = !focusModesExpanded },
                        trailing = {
                            IconButton(onClick = { showAddFocusMode = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                            }
                        }
                    )
                }

                if (focusModesExpanded) {
                    item {
                        SettingsRow(label = "Sync with Do Not Disturb") {
                            Switch(
                                checked = dndSyncEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasDndPermission()) {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                    } else {
                                        onSetDndSyncEnabled(enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }

                    if (focusModes.isEmpty()) {
                        item {
                            Text("No focus modes defined", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 32.dp, bottom = 16.dp))
                        }
                    } else {
                        items(focusModes) { mode ->
                            FocusModeItem(
                                mode = mode,
                                isActive = activeFocusModeName == mode.name,
                                onToggle = { onToggleFocusMode(if (activeFocusModeName == mode.name) null else mode.name) },
                                onEdit = { editingMode = mode },
                                onDelete = { onDeleteFocusMode(mode.name) }
                            )
                        }
                    }
                }

                item { Divider() }

                // Section: Blocked Apps
                item {
                    SettingsSectionHeader(
                        title = "Blocked Apps",
                        isExpanded = blockedAppsExpanded,
                        onToggle = { blockedAppsExpanded = !blockedAppsExpanded }
                    )
                }

                if (blockedAppsExpanded) {
                    if (blockedApps.isEmpty()) {
                        item {
                            Text("No apps blocked", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 32.dp, bottom = 16.dp))
                        }
                    } else {
                        items(blockedApps.toList()) { (pkg, expiry) ->
                            val app = allApps.find { it.packageName == pkg }
                            BlockedAppItem(
                                label = app?.label ?: pkg,
                                expiry = expiry,
                                onUnblock = { onUnblock(pkg) }
                            )
                        }
                    }
                }

                item { Divider() }

                // Section: Security
                item {
                    var securityExpanded by remember { mutableStateOf(false) }
                    SettingsSectionHeader(
                        title = "Security",
                        isExpanded = securityExpanded,
                        onToggle = { securityExpanded = !securityExpanded }
                    )
                    
                    if (securityExpanded) {
                        SettingsRow(label = "Biometric Focus Protection") {
                            Switch(
                                checked = biometricFocusLock,
                                onCheckedChange = onSetBiometricFocusLock,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                        Text(
                            "Require authentication to turn off Focus Mode if tasks are incomplete.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 32.dp, bottom = 16.dp, end = 24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddFocusMode) {
        FocusModePage(
            allApps = allApps,
            onDismiss = { showAddFocusMode = false },
            onConfirm = { name, pkgs, start, end, oldName ->
                onAddFocusMode(name, pkgs, start, end, oldName)
                showAddFocusMode = false
            }
        )
    }

    if (editingMode != null) {
        FocusModePage(
            allApps = allApps,
            existingMode = editingMode,
            onDismiss = { editingMode = null },
            onConfirm = { name, pkgs, start, end, oldName ->
                onAddFocusMode(name, pkgs, start, end, oldName)
                editingMode = null
            }
        )
    }
}

@Composable
fun SettingsRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        content()
    }
}

@Composable
fun Divider() {
    HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun FocusModeItem(
    mode: FocusMode,
    isActive: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mode.name,
                    color = if (isActive) Color.White else Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                val scheduleText = if (mode.startTime != null && mode.endTime != null) {
                    "${formatMinutes(mode.startTime)} - ${formatMinutes(mode.endTime)}"
                } else "Manual"
                Text(
                    "${mode.allowedPackages.size} apps • $scheduleText",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Row {
                TextButton(onClick = onToggle) {
                    Text(if (isActive) "OFF" else "ON", color = if (isActive) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onEdit) {
                    Text("EDIT", color = Color.White)
                }
                TextButton(onClick = onDelete) {
                    // Fixed visibility: White text on black background
                    Text("DELETE", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun BlockedAppItem(
    label: String,
    expiry: Long?,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 16.sp)
            val timeLabel = if (expiry == null) "Forever" else {
                val remaining = expiry - System.currentTimeMillis()
                if (remaining <= 0) "Expired" else {
                    val mins = (remaining / 60000).toInt()
                    val hrs = mins / 60
                    if (hrs > 0) "${hrs}h ${mins % 60}m remaining" else "${mins}m remaining"
                }
            }
            Text(timeLabel, color = Color.Gray, fontSize = 12.sp)
        }
        TextButton(onClick = onUnblock) {
            Text("UNBLOCK", color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModePage(
    allApps: List<AppItem>,
    existingMode: FocusMode? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Set<String>, Int?, Int?, String?) -> Unit
) {
    var name by remember { mutableStateOf(existingMode?.name ?: "") }
    val selectedPackages = remember { mutableStateListOf<String>().apply { if (existingMode != null) addAll(existingMode.allowedPackages) } }
    
    var startTime by remember { mutableStateOf(existingMode?.startTime) }
    var endTime by remember { mutableStateOf(existingMode?.endTime) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (existingMode == null) "New Focus Mode" else "Edit Mode",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(Modifier.height(24.dp))

                Text("Schedule", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeSelectButton(
                        label = "Start",
                        time = startTime,
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelectButton(
                        label = "End",
                        time = endTime,
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (startTime != null || endTime != null) {
                    TextButton(onClick = { startTime = null; endTime = null }) {
                        Text("Clear Schedule", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("Allowed Apps (${selectedPackages.size})", color = Color.White, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    items(allApps) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedPackages.remove(app.packageName)
                                    else selectedPackages.add(app.packageName)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color.White,
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Text(app.label, color = Color.White, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name, selectedPackages.toSet(), startTime, endTime, existingMode?.name)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(if (existingMode == null) "CREATE" else "SAVE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialogWrapper(
            initialMinutes = startTime ?: 540, // 9:00
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogWrapper(
            initialMinutes = endTime ?: 1020, // 17:00
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = it
                showEndTimePicker = false
            }
        )
    }
}

@Composable
fun TimeSelectButton(label: String, time: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(Color.White.copy(alpha = 0.05f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time?.let { formatMinutes(it) } ?: "--:--",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogWrapper(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text("OK", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color.Black,
        title = { Text("Select Time", color = Color.White) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Color.DarkGray,
                    clockDialSelectedContentColor = Color.Black,
                    clockDialUnselectedContentColor = Color.White,
                    selectorColor = Color.White,
                    periodSelectorBorderColor = Color.White,
                    periodSelectorSelectedContainerColor = Color.White,
                    periodSelectorUnselectedContainerColor = Color.Black,
                    periodSelectorSelectedContentColor = Color.Black,
                    periodSelectorUnselectedContentColor = Color.White,
                    timeSelectorSelectedContainerColor = Color.White,
                    timeSelectorUnselectedContainerColor = Color.DarkGray,
                    timeSelectorSelectedContentColor = Color.Black,
                    timeSelectorUnselectedContentColor = Color.White
                )
            )
        }
    )
}

fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%02d:%02d".format(h, m)
}

@Composable
fun SettingsSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Light,
                fontSize = 20.sp
            )
        }
        trailing?.invoke()
    }
}
