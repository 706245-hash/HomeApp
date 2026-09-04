package com.agnocode.minimalhomeapp.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BackupSettings(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    autoSyncEnabled: Boolean,
    onSetAutoSyncEnabled: (Boolean) -> Unit,
    autoSyncUri: String?,
    onSelectSyncFile: () -> Unit
) {
    Column {
        SettingsSectionHeader(
            title = "Backup & Restore",
            isExpanded = isExpanded,
            onToggle = onToggle
        )

        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 32.dp, bottom = 16.dp, end = 24.dp)) {
                Text(
                    "Save your notes, focus modes, and settings to a file or restore from a previous backup.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onBackup,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("BACKUP")
                    }
                    Button(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("RESTORE")
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                SettingsRow(label = "Auto-Sync (Daily)") {
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = onSetAutoSyncEnabled,
                        colors = switchColors()
                    )
                }
                
                if (autoSyncEnabled) {
                    TextButton(
                        onClick = onSelectSyncFile,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (autoSyncUri != null) "Syncing to selected file" else "Select file to sync",
                                color = if (autoSyncUri != null) Color.White else Color.Gray,
                                fontSize = 14.sp
                            )
                            if (autoSyncUri != null) {
                                Text(
                                    text = "Tap to change file",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
