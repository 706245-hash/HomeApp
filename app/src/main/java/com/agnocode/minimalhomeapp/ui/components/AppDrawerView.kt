package com.agnocode.minimalhomeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.agnocode.minimalhomeapp.R
import com.agnocode.minimalhomeapp.data.model.AppItem
import com.agnocode.minimalhomeapp.util.SmartAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerView(
    apps: List<AppItem>,
    searchQuery: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onBlock: (String, Long?) -> Unit,
    isFavorite: (String) -> Boolean,
    isProtected: (String) -> Boolean = { false },
    onToggleProtected: (String) -> Unit = {},
    onProtectedLaunch: (() -> Unit) -> Unit = { it() },
    isGhost: (String) -> Boolean = { false },
    onToggleGhost: (String) -> Unit = {},
    getIcon: (String) -> android.graphics.drawable.Drawable? = { null },
    usageStats: Map<String, Long> = emptyMap(),
    usageMode: String = "none",
    isMonochrome: Boolean = false,
    smartAction: SmartAction? = null,
    onSearch: () -> Unit = {},
    showIcons: Boolean = false,
    iconPackPackage: String? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 20.sp
                ),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch()
                    keyboardController?.hide()
                }),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                stringResource(R.string.drawer_search_placeholder),
                                color = Color.DarkGray,
                                fontSize = 20.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (smartAction != null) {
                    item {
                        SmartResultItem(
                            action = smartAction,
                            onClick = { onSearchQueryChange("") }
                        )
                    }
                }

                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val usageMillis = usageStats[app.packageName] ?: 0L
                    val usageSubtitle = when (usageMode) {
                        "time" -> if (usageMillis > 0) formatUsageTime(context, usageMillis) else null
                        "percentage" -> if (usageMillis > 0) formatUsagePercentage(context, usageMillis) else null
                        else -> null
                    }
                    
                    Box(modifier = Modifier.animateItem()) {
                        AppListItem(
                            app = app,
                            isFavorite = isFavorite(app.packageName),
                            onToggleFavorite = { onToggleFavorite(app.packageName) },
                            onBlock = { duration -> onBlock(app.packageName, duration) },
                            isProtected = isProtected(app.packageName),
                            onToggleProtected = { onToggleProtected(app.packageName) },
                            onProtectedLaunch = onProtectedLaunch,
                            isGhost = isGhost(app.packageName),
                            onToggleGhost = { onToggleGhost(app.packageName) },
                            iconOverride = getIcon(app.packageName),
                            usageSubtitle = usageSubtitle,
                            isMonochrome = isMonochrome,
                            showIcon = showIcons,
                            iconPackPackage = iconPackPackage
                        )
                    }
                }
            }
        }
    }
}

private fun formatUsageTime(context: android.content.Context, millis: Long): String {
    val mins = (millis / 60000)
    val hrs = mins / 60
    return if (hrs > 0) context.getString(R.string.usage_time_format, hrs, mins % 60) else context.getString(R.string.usage_mins_format, mins)
}

private fun formatUsagePercentage(context: android.content.Context, millis: Long): String {
    val awakeMillis = 16 * 60 * 60 * 1000L // Assume 16 hours awake
    val pct = (millis.toFloat() / awakeMillis * 100).toInt()
    return if (pct > 0) context.getString(R.string.usage_pct_format, pct) else context.getString(R.string.usage_pct_low)
}
