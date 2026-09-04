package com.agnocode.minimalhomeapp.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agnocode.minimalhomeapp.data.model.AppItem

@Composable
fun SecuritySettings(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    biometricFocusLock: Boolean,
    onSetBiometricFocusLock: (Boolean) -> Unit,
    ghostApps: List<AppItem>,
    onRemoveGhost: (String) -> Unit,
    blockedApps: Map<String, Long?>,
    allApps: List<AppItem>,
    onUnblock: (String) -> Unit
) {
    Column {
        SettingsSectionHeader(
            title = "Security & Blocking",
            isExpanded = isExpanded,
            onToggle = onToggle
        )

        if (isExpanded) {
            // Biometrics
            SettingsRow(label = "Biometric Focus Protection") {
                Switch(
                    checked = biometricFocusLock,
                    onCheckedChange = onSetBiometricFocusLock,
                    colors = switchColors()
                )
            }
            Text(
                "Require authentication to turn off Focus Mode if tasks are incomplete.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 16.dp, end = 24.dp)
            )

            // Blocked Apps
            Text(
                "Blocked Apps",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
            )
            if (blockedApps.isEmpty()) {
                Text("No apps blocked", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 32.dp, bottom = 16.dp))
            } else {
                blockedApps.forEach { (pkg, expiry) ->
                    val app = allApps.find { it.packageName == pkg }
                    BlockedAppItem(
                        label = app?.label ?: pkg,
                        expiry = expiry,
                        onUnblock = { onUnblock(pkg) }
                    )
                }
            }

            // Stealth Apps
            if (ghostApps.isNotEmpty()) {
                Text(
                    "Stealth Apps",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                )
                ghostApps.forEach { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(app.label, color = Color.Gray, fontSize = 14.sp)
                        TextButton(onClick = { onRemoveGhost(app.packageName) }) {
                            Text("VISIBLE", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedAppItem(
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
