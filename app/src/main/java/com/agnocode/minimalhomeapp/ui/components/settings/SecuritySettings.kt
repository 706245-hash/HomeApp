package com.agnocode.minimalhomeapp.ui.components.settings

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
            title = stringResource(R.string.settings_section_security),
            isExpanded = isExpanded,
            onToggle = onToggle
        )

        if (isExpanded) {
            // Biometrics
            SettingsRow(label = stringResource(R.string.security_biometric_lock)) {
                Switch(
                    checked = biometricFocusLock,
                    onCheckedChange = onSetBiometricFocusLock,
                    colors = switchColors()
                )
            }
            Text(
                stringResource(R.string.security_biometric_desc),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 32.dp, bottom = 16.dp, end = 24.dp)
            )

            // Blocked Apps Dropdown
            var blockedExpanded by remember { mutableStateOf(false) }
            Column(modifier = Modifier.padding(start = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { blockedExpanded = !blockedExpanded }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.security_blocked_apps),
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = if (blockedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (blockedExpanded) stringResource(R.string.settings_collapse) else stringResource(R.string.settings_expand),
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (blockedExpanded) {
                    if (blockedApps.isEmpty()) {
                        Text(
                            stringResource(R.string.security_no_blocked),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
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
                }
            }

            // Stealth Apps Dropdown
            if (ghostApps.isNotEmpty()) {
                var stealthExpanded by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(start = 32.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stealthExpanded = !stealthExpanded }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.security_stealth_apps),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = if (stealthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (stealthExpanded) stringResource(R.string.settings_collapse) else stringResource(R.string.settings_expand),
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (stealthExpanded) {
                        ghostApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(app.label, color = Color.Gray, fontSize = 14.sp)
                                TextButton(onClick = { onRemoveGhost(app.packageName) }) {
                                    Text(
                                        stringResource(R.string.security_visible),
                                        color = Color.White,
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
}

@Composable
private fun BlockedAppItem(
    label: String,
    expiry: Long?,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 16.sp)
            val timeLabel = if (expiry == null) stringResource(R.string.security_forever) else {
                val remaining = expiry - System.currentTimeMillis()
                if (remaining <= 0) stringResource(R.string.security_expired) else {
                    val mins = (remaining / 60000).toInt()
                    val hrs = mins / 60
                    if (hrs > 0) stringResource(R.string.security_remaining_format, hrs, mins % 60) else stringResource(R.string.security_mins_remaining_format, mins)
                }
            }
            Text(timeLabel, color = Color.Gray, fontSize = 12.sp)
        }
        TextButton(onClick = onUnblock) {
            Text(stringResource(R.string.security_unblock), color = Color.White)
        }
    }
}
