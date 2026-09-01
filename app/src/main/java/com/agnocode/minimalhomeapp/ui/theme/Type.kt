package com.agnocode.minimalhomeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.agnocode.minimalhomeapp.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold)
)

val RobotoSlabFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Roboto Slab"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("Roboto Slab"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Roboto Slab"), fontProvider = provider, weight = FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = provider, weight = FontWeight.Bold)
)

fun getTypography(fontFamily: FontFamily): Typography {
    return Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontSize = 57.sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontSize = 45.sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontSize = 36.sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontSize = 32.sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontSize = 28.sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontSize = 24.sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = fontFamily, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontSize = 11.sp)
    )
}
