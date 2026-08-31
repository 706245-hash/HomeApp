package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.data.model.FocusMode

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    blockedApps: Map<String, Long?>,
    allApps: List<AppItem>,
    onUnblock: (String) -> Unit,
    focusModes: List<FocusMode>,
    activeFocusModeName: String?,
    onToggleFocusMode: (String?) -> Unit,
    onAddFocusMode: (String, Set<String>, Int?, Int?) -> Unit,
    onDeleteFocusMode: (String) -> Unit,
    fontFamily: String,
    onSetFontFamily: (String) -> Unit,
    showIcons: Boolean,
    onSetShowIcons: (Boolean) -> Unit,
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

    var systemExpanded by remember { mutableStateOf(!isDefault) }
    var customizationExpanded by remember { mutableStateOf(false) }
    var focusModesExpanded by remember { mutableStateOf(false) }
    var blockedAppsExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.DarkGray,
        title = { Text("Settings", color = Color.White) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
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
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Set as Default Launcher", color = Color.White)
                            }
                        }
                    } else {
                        item {
                            Text(
                                "App is set as default",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp)) }

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
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Family", color = Color.White, fontSize = 16.sp)
                            var showFontMenu by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { showFontMenu = true }) {
                                    Text(fontFamily.replaceFirstChar { it.uppercase() }, color = Color.Gray)
                                }
                                DropdownMenu(
                                    expanded = showFontMenu,
                                    onDismissRequest = { showFontMenu = false },
                                    modifier = Modifier.background(Color.DarkGray)
                                ) {
                                    listOf("default", "serif", "monospace", "sans-serif").forEach { font ->
                                        DropdownMenuItem(
                                            text = { Text(font.replaceFirstChar { it.uppercase() }, color = Color.White) },
                                            onClick = {
                                                onSetFontFamily(font)
                                                showFontMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show App Icons", color = Color.White, fontSize = 16.sp)
                            Switch(
                                checked = showIcons,
                                onCheckedChange = onSetShowIcons,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color.Gray
                                )
                            )
                        }
                    }

                    if (showIcons) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Icon Pack", color = Color.White, fontSize = 16.sp)
                                var showIconMenu by remember { mutableStateOf(false) }
                                Box {
                                    TextButton(onClick = { showIconMenu = true }) {
                                        val label = availableIconPacks.find { it.packageName == selectedIconPack }?.label ?: "Default"
                                        Text(label, color = Color.Gray)
                                    }
                                    DropdownMenu(
                                        expanded = showIconMenu,
                                        onDismissRequest = { showIconMenu = false },
                                        modifier = Modifier.background(Color.DarkGray)
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
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp)) }

                // Section: Focus Modes
                item {
                    SettingsSectionHeader(
                        title = "Focus Modes",
                        isExpanded = focusModesExpanded,
                        onToggle = { focusModesExpanded = !focusModesExpanded },
                        trailing = {
                            TextButton(onClick = { showAddFocusMode = true }) {
                                Text("Add", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    )
                }

                if (focusModesExpanded) {
                    if (focusModes.isEmpty()) {
                        item {
                            Text("No focus modes defined", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                        }
                    } else {
                        items(focusModes) { mode ->
                            val isActive = activeFocusModeName == mode.name
                            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            mode.name,
                                            color = if (isActive) Color.White else Color.Gray,
                                            fontSize = 16.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                        )
                                        val scheduleText = if (mode.startTime != null && mode.endTime != null) {
                                            "Schedule: ${formatMinutes(mode.startTime)} - ${formatMinutes(mode.endTime)}"
                                        } else "Manual"
                                        Text(
                                            "${mode.allowedPackages.size} apps • $scheduleText",
                                            color = Color.DarkGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Row {
                                        TextButton(onClick = { onToggleFocusMode(if (isActive) null else mode.name) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                            Text(if (isActive) "Off" else "On", color = if (isActive) Color.Red else Color.Green, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { editingMode = mode }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                            Text("Edit", color = Color.White, fontSize = 12.sp)
                                        }
                                        TextButton(onClick = { onDeleteFocusMode(mode.name) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                            Text("Del", color = Color.DarkGray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp)) }

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
                            Text("No apps blocked", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
                        }
                    } else {
                        items(blockedApps.toList()) { (pkg, expiry) ->
                            val app = allApps.find { it.packageName == pkg }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app?.label ?: pkg, color = Color.White, fontSize = 16.sp)
                                    val timeLabel = if (expiry == null) "∞" else {
                                        val remaining = expiry - System.currentTimeMillis()
                                        if (remaining <= 0) "Expired" else {
                                            val mins = (remaining / 60000).toInt()
                                            val hrs = mins / 60
                                            if (hrs > 0) "${hrs}h ${mins % 60}m" else "${mins}m"
                                        }
                                    }
                                    Text(timeLabel, color = Color.Gray, fontSize = 12.sp)
                                }
                                TextButton(onClick = { onUnblock(pkg) }) {
                                    Text("Unblock", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        }
    )

    if (showAddFocusMode) {
        FocusModeDialog(
            allApps = allApps,
            onDismiss = { showAddFocusMode = false },
            onConfirm = { name, pkgs, start, end ->
                onAddFocusMode(name, pkgs, start, end)
                showAddFocusMode = false
            }
        )
    }

    if (editingMode != null) {
        FocusModeDialog(
            allApps = allApps,
            existingMode = editingMode,
            onDismiss = { editingMode = null },
            onConfirm = { name, pkgs, start, end ->
                onAddFocusMode(name, pkgs, start, end)
                editingMode = null
            }
        )
    }
}

@Composable
fun FocusModeDialog(
    allApps: List<AppItem>,
    existingMode: FocusMode? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Set<String>, Int?, Int?) -> Unit
) {
    var name by remember { mutableStateOf(existingMode?.name ?: "") }
    val selectedPackages = remember { mutableStateListOf<String>().apply { if (existingMode != null) addAll(existingMode.allowedPackages) } }
    
    var startH by remember { mutableStateOf(existingMode?.startTime?.let { it / 60 }?.toString() ?: "") }
    var startM by remember { mutableStateOf(existingMode?.startTime?.let { it % 60 }?.toString() ?: "") }
    var endH by remember { mutableStateOf(existingMode?.endTime?.let { it / 60 }?.toString() ?: "") }
    var endM by remember { mutableStateOf(existingMode?.endTime?.let { it % 60 }?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.DarkGray,
        title = { Text(if (existingMode == null) "New Focus Mode" else "Edit Focus Mode", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mode Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    enabled = existingMode == null // Name is unique ID
                )
                Spacer(Modifier.height(8.dp))
                
                Text("Schedule (24h format, optional):", color = Color.LightGray, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeInput(value = startH, onValueChange = { if (it.length <= 2) startH = it }, placeholder = "HH")
                    Text(":", color = Color.White)
                    TimeInput(value = startM, onValueChange = { if (it.length <= 2) startM = it }, placeholder = "MM")
                    Text(" to ", color = Color.LightGray)
                    TimeInput(value = endH, onValueChange = { if (it.length <= 2) endH = it }, placeholder = "HH")
                    Text(":", color = Color.White)
                    TimeInput(value = endM, onValueChange = { if (it.length <= 2) endM = it }, placeholder = "MM")
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Select Allowed Apps:", color = Color.LightGray, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(allApps) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedPackages.remove(app.packageName)
                                    else selectedPackages.add(app.packageName)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Text(app.label, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val start = startH.toIntOrNull()?.let { h -> startM.toIntOrNull()?.let { m -> h * 60 + m } }
                        val end = endH.toIntOrNull()?.let { h -> endM.toIntOrNull()?.let { m -> h * 60 + m } }
                        onConfirm(name, selectedPackages.toSet(), start, end)
                    }
                }
            ) {
                Text(if (existingMode == null) "Create" else "Save", color = Color.Green)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        }
    )
}

@Composable
fun TimeInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.DarkGray, fontSize = 12.sp) },
        modifier = Modifier.width(60.dp).padding(2.dp),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
        singleLine = true
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        trailing?.invoke()
    }
}
