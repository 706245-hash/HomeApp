package com.agnocode.minimalhomeapp.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    blockedApps: Map<String, Long?>,
    allApps: List<AppItem>,
    onUnblock: (String) -> Unit
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Function to check if this app is the default launcher
    fun checkDefault() {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val resolvedPackage = resolveInfo?.activityInfo?.packageName
        
        // If the resolved package is "android" or "com.google.android.permissioncontroller", 
        // it usually means the system chooser will be shown (no default set).
        isDefault = resolvedPackage == context.packageName
    }

    // Refresh the check when the dialog is shown and every time the activity resumes
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

    // Initial check
    LaunchedEffect(Unit) {
        checkDefault()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.DarkGray,
        title = {
            Text(
                "Settings",
                color = Color.White
            )
        },
        text = {
            Column {
                if (!isDefault) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Set as Default Launcher",
                            color = Color.White
                        )
                    }
                }

                HorizontalDivider(color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    "Blocked Apps",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (blockedApps.isEmpty()) {
                    Text(
                        "No apps blocked",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(blockedApps.toList()) { (pkg, expiry) ->
                            val app = allApps.find { it.packageName == pkg }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        app?.label ?: pkg,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    val timeLabel = if (expiry == null) {
                                        "∞"
                                    } else {
                                        val remaining = expiry - System.currentTimeMillis()
                                        if (remaining <= 0) "Expired" else {
                                            val mins = (remaining / 60000).toInt()
                                            val hrs = mins / 60
                                            if (hrs > 0) "${hrs}h ${mins % 60}m" else "${mins}m"
                                        }
                                    }
                                    Text(
                                        timeLabel,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                TextButton(onClick = { onUnblock(pkg) }) {
                                    Text("Unblock", color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val versionName = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    "1.0.0"
                }
                Text(
                    text = "v$versionName",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White)
                }
            }
        }
    )
}
