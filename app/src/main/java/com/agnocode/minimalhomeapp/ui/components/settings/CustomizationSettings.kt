package com.agnocode.minimalhomeapp.ui.components.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.agnocode.minimalhomeapp.R
import com.agnocode.minimalhomeapp.data.model.AppItem

@Composable
fun CustomizationSettings(
    context: Context,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    showIcons: Boolean,
    onSetShowIcons: (Boolean) -> Unit,
    showFavorites: Boolean,
    onSetShowFavorites: (Boolean) -> Unit,
    usageAwarenessMode: String,
    onSetUsageAwarenessMode: (String) -> Unit,
    monochromeIcons: Boolean,
    onSetMonochromeIcons: (Boolean) -> Unit,
    accentColor: String,
    onSetAccentColor: (String) -> Unit,
    hasUsageStatsPermission: () -> Boolean,
    availableIconPacks: List<AppItem>,
    selectedIconPack: String?,
    onSetIconPack: (String?) -> Unit
) {
    Column {
        SettingsSectionHeader(
            title = stringResource(R.string.settings_section_customization),
            isExpanded = isExpanded,
            onToggle = onToggle
        )

        if (isExpanded) {
            // Icons Sub-section
            var iconsExpanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(start = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { iconsExpanded = !iconsExpanded }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.custom_icons), color = Color.White, fontSize = 16.sp)
                    Icon(
                        imageVector = if (iconsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (iconsExpanded) stringResource(R.string.settings_collapse) else stringResource(R.string.settings_expand),
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (iconsExpanded) {
                    SettingsRow(label = stringResource(R.string.custom_show_icons)) {
                        Switch(
                            checked = showIcons,
                            onCheckedChange = { 
                                onSetShowIcons(it)
                                if (it) onSetMonochromeIcons(true)
                            },
                            colors = switchColors()
                        )
                    }

                    if (showIcons) {
                        SettingsRow(label = stringResource(R.string.custom_monochrome)) {
                            Switch(
                                checked = monochromeIcons,
                                onCheckedChange = onSetMonochromeIcons,
                                colors = switchColors()
                            )
                        }

                        SettingsRow(label = stringResource(R.string.custom_icon_pack)) {
                            IconPackDropdown(
                                availableIconPacks = availableIconPacks,
                                selectedIconPack = selectedIconPack,
                                onSetIconPack = onSetIconPack
                            )
                        }
                    }
                }
            }

            SettingsRow(label = stringResource(R.string.custom_show_favorites)) {
                Switch(
                    checked = showFavorites,
                    onCheckedChange = onSetShowFavorites,
                    colors = switchColors()
                )
            }

            SettingsRow(label = stringResource(R.string.custom_usage_awareness)) {
                UsageAwarenessDropdown(
                    context = context,
                    mode = usageAwarenessMode,
                    onSetMode = onSetUsageAwarenessMode,
                    hasPermission = hasUsageStatsPermission
                )
            }

            SettingsRow(label = stringResource(R.string.custom_accent_style)) {
                AccentStyleDropdown(
                    current = accentColor,
                    onSetColor = onSetAccentColor
                )
            }
        }
    }
}

@Composable
private fun IconPackDropdown(
    availableIconPacks: List<AppItem>,
    selectedIconPack: String?,
    onSetIconPack: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val label = availableIconPacks.find { it.packageName == selectedIconPack }?.label ?: stringResource(R.string.custom_default)
        Text(
            label,
            color = Color.LightGray,
            modifier = Modifier.clickable { expanded = true }.padding(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.Black)
                .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.custom_default), color = Color.White) },
                onClick = {
                    onSetIconPack(null)
                    expanded = false
                }
            )
            availableIconPacks.forEach { pack ->
                DropdownMenuItem(
                    text = { Text(pack.label, color = Color.White) },
                    onClick = {
                        onSetIconPack(pack.packageName)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UsageAwarenessDropdown(
    context: Context,
    mode: String,
    onSetMode: (String) -> Unit,
    hasPermission: () -> Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf<String?>(null) }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.usage_permission_title), color = Color.White) },
            text = { Text(stringResource(R.string.usage_permission_msg), color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                    showPermissionDialog = false
                    pendingMode?.let { onSetMode(it) }
                }) {
                    Text(stringResource(R.string.usage_permission_grant), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            },
            containerColor = Color.Black
        )
    }

    Box {
        val currentLabel = when (mode) {
            "time" -> stringResource(R.string.usage_mode_time)
            "percentage" -> stringResource(R.string.usage_mode_pct)
            else -> stringResource(R.string.usage_mode_none)
        }
        Text(
            currentLabel,
            color = Color.LightGray,
            modifier = Modifier.clickable { expanded = true }.padding(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.Black)
                .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
        ) {
            val options = listOf(
                "none" to R.string.usage_mode_none,
                "time" to R.string.usage_mode_time,
                "percentage" to R.string.usage_mode_pct
            )
            options.forEach { (id, resId) ->
                DropdownMenuItem(
                    text = { Text(stringResource(resId), color = Color.White) },
                    onClick = {
                        if (id != "none" && !hasPermission()) {
                            pendingMode = id
                            showPermissionDialog = true
                        } else {
                            onSetMode(id)
                        }
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AccentStyleDropdown(
    current: String,
    onSetColor: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val currentLabel = when (current) {
            "gold" -> stringResource(R.string.accent_gold)
            "emerald" -> stringResource(R.string.accent_emerald)
            "blue" -> stringResource(R.string.accent_blue)
            "rose" -> stringResource(R.string.accent_rose)
            else -> stringResource(R.string.accent_white)
        }
        Text(
            currentLabel,
            color = Color.LightGray,
            modifier = Modifier.clickable { expanded = true }.padding(8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.Black)
                .border(1.dp, Color.White, MaterialTheme.shapes.extraSmall)
        ) {
            val options = listOf(
                "white" to R.string.accent_white,
                "gold" to R.string.accent_gold,
                "emerald" to R.string.accent_emerald,
                "blue" to R.string.accent_blue,
                "rose" to R.string.accent_rose
            )
            options.forEach { (id, resId) ->
                DropdownMenuItem(
                    text = { Text(stringResource(resId), color = Color.White) },
                    onClick = {
                        onSetColor(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
