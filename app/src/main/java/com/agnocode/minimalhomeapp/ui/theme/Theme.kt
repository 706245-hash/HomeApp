package com.agnocode.minimalhomeapp.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color.Gray,
    tertiary = Color.DarkGray,
    background = Color.Black,
    surface = Color.Black,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun MinimalHomeAppTheme(
    content: @Composable () -> Unit
) {
    val typography = Typography()

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = typography
    ) {
        CompositionLocalProvider(LocalTextStyle provides typography.bodyLarge) {
            content()
        }
    }
}
