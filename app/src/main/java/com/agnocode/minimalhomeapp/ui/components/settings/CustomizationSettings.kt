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
    availableIconPacks: List<AppItem>,
    selectedIconPack: String?,
    onSetIconPack: (String?) -> Unit
) {
    Column {
        SettingsSectionHeader(
            title = "Customization",
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
                    Text("Icons", color = Color.White, fontSize = 16.sp)
                    Icon(
                        imageVector = if (iconsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (iconsExpanded) {
                    SettingsRow(label = "Show App Icons") {
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
                        SettingsRow(label = "Monochrome Icons") {
                            Switch(
                                checked = monochromeIcons,
                                onCheckedChange = onSetMonochromeIcons,
                                colors = switchColors()
                            )
                        }

                        SettingsRow(label = "Icon Pack") {
                            IconPackDropdown(
                                availableIconPacks = availableIconPacks,
                                selectedIconPack = selectedIconPack,
                                onSetIconPack = onSetIconPack
                            )
                        }
                    }
                }
            }

            SettingsRow(label = "Show Favorites") {
                Switch(
                    checked = showFavorites,
                    onCheckedChange = onSetShowFavorites,
                    colors = switchColors()
                )
            }

            SettingsRow(label = "Usage Awareness") {
                UsageAwarenessDropdown(
                    context = context,
                    mode = usageAwarenessMode,
                    onSetMode = onSetUsageAwarenessMode
                )
            }

            SettingsRow(label = "Accent Style") {
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
        val label = availableIconPacks.find { it.packageName == selectedIconPack }?.label ?: "Default"
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
                text = { Text("Default", color = Color.White) },
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
    onSetMode: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val currentLabel = when (mode) {
            "time" -> "Time spent"
            "percentage" -> "% of day"
            else -> "None"
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
                "none" to "None",
                "time" to "Time spent",
                "percentage" to "% of day"
            )
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.White) },
                    onClick = {
                        if (id != "none") {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                        onSetMode(id)
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
        val currentLabel = current.replaceFirstChar { it.uppercase() }
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
                "white" to "Majestic White",
                "gold" to "Royal Gold",
                "emerald" to "Deep Emerald",
                "blue" to "Minimal Blue",
                "rose" to "Soft Rose"
            )
            options.forEach { (id, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = Color.White) },
                    onClick = {
                        onSetColor(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
