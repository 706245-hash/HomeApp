package com.agnocode.minimalhomeapp.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

fun getAccentColor(name: String): Color {
    return when (name) {
        "gold" -> Color(0xFFFFD700)
        "emerald" -> Color(0xFF50C878)
        "blue" -> Color(0xFF3498DB)
        "rose" -> Color(0xFFF08080)
        else -> Color.White
    }
}

private fun getDarkColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
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
    accentColorName: String = "white",
    content: @Composable () -> Unit,
) {
    val accentColor = getAccentColor(accentColorName)
    val typography = Typography()
    MaterialTheme(
        colorScheme = getDarkColorScheme(accentColor),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalTextStyle provides typography.bodyLarge) {
            content()
        }
    }
}
