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
import com.agnocode.minimalhomeapp.data.model.AppItem

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
    getIcon: (String) -> android.graphics.drawable.Drawable? = { null },
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
                                "Search apps...",
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
                    contentDescription = "Settings",
                    tint = Color.DarkGray
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
                items(
                    items = apps,
                    key = { it.packageName }
                ) { app ->
                    Box(modifier = Modifier.animateItem()) {
                        AppListItem(
                            app = app,
                            isFavorite = isFavorite(app.packageName),
                            onToggleFavorite = { onToggleFavorite(app.packageName) },
                            onBlock = { duration -> onBlock(app.packageName, duration) },
                            isProtected = isProtected(app.packageName),
                            onToggleProtected = { onToggleProtected(app.packageName) },
                            onProtectedLaunch = onProtectedLaunch,
                            iconOverride = getIcon(app.packageName),
                            showIcon = showIcons,
                            iconPackPackage = iconPackPackage
                        )
                    }
                }
            }
        }
    }
}
