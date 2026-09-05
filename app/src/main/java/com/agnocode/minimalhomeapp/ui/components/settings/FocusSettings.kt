package com.agnocode.minimalhomeapp.ui.components.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.agnocode.minimalhomeapp.R
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.data.model.FocusMode

@Composable
fun FocusSettings(
    context: Context,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    focusModes: List<FocusMode>,
    activeFocusModeName: String?,
    onToggleFocusMode: (String?) -> Unit,
    onAddFocusMode: (String, Set<String>, Int?, Int?, String?) -> Unit,
    onDeleteFocusMode: (String) -> Unit,
    dndSyncEnabled: Boolean,
    onSetDndSyncEnabled: (Boolean) -> Unit,
    hasDndPermission: () -> Boolean,
    allApps: List<AppItem>
) {
    var showAddFocusMode by remember { mutableStateOf(false) }
    var editingMode by remember { mutableStateOf<FocusMode?>(null) }

    Column {
        SettingsSectionHeader(
            title = stringResource(R.string.settings_section_focus),
            isExpanded = isExpanded,
            onToggle = onToggle,
            trailing = {
                IconButton(onClick = { showAddFocusMode = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.focus_add), tint = Color.White)
                }
            }
        )

        if (isExpanded) {
            SettingsRow(label = stringResource(R.string.focus_dnd_sync)) {
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
                    colors = switchColors()
                )
            }

            if (focusModes.isEmpty()) {
                Text(stringResource(R.string.focus_none_defined), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 32.dp, bottom = 16.dp))
            } else {
                focusModes.forEach { mode ->
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
private fun FocusModeItem(
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
                } else stringResource(R.string.focus_manual)
                Text(
                    text = stringResource(R.string.focus_summary_format, mode.allowedPackages.size, scheduleText),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Row {
                TextButton(onClick = onToggle) {
                    Text(if (isActive) stringResource(R.string.focus_off) else stringResource(R.string.focus_on), color = if (isActive) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.edit), color = Color.White)
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete), color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusModePage(
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

    BackHandler { onDismiss() }

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
                        if (existingMode == null) stringResource(R.string.focus_new_title) else stringResource(R.string.focus_edit_title),
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel), tint = Color.White)
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.focus_name_label), color = Color.Gray) },
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

                Text(stringResource(R.string.focus_schedule), color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimeSelectButton(
                        label = stringResource(R.string.focus_start),
                        time = startTime,
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    TimeSelectButton(
                        label = stringResource(R.string.focus_end),
                        time = endTime,
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (startTime != null || endTime != null) {
                    TextButton(onClick = { startTime = null; endTime = null }) {
                        Text(stringResource(R.string.focus_clear_schedule), color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.focus_allowed_apps, selectedPackages.size), color = Color.White, fontWeight = FontWeight.Bold)
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
                    Text(if (existingMode == null) stringResource(R.string.focus_create) else stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialogWrapper(
            initialMinutes = startTime ?: 540,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialogWrapper(
            initialMinutes = endTime ?: 1020,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = it
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun TimeSelectButton(label: String, time: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
private fun TimePickerDialogWrapper(
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
                Text(stringResource(R.string.ok), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.Gray)
            }
        },
        containerColor = Color.Black,
        title = { Text(stringResource(R.string.focus_select_time), color = Color.White) },
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
