package com.masalik.yemen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.masalik.yemen.R

val MasalikObsidian = Color(0xFF090D16)
val MasalikSlate = Color(0xFF111827)
val MasalikCard = Color(0xFF161F31)
val MasalikBorder = Color(0xFF2B3548)
val MasalikAmber = Color(0xFFF59E0B)
val MasalikAmberSoft = Color(0xFFFCD34D)
val MasalikEmerald = Color(0xFF10B981)
val MasalikSky = Color(0xFF38BDF8)
val MasalikCrimson = Color(0xFFEF4444)
val MasalikIvory = Color(0xFFF5F2EB)
val MasalikMuted = Color(0xFFA8A29E)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.Black)
)

val ChakraPetchFontFamily = FontFamily(
    Font(R.font.chakra_petch, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

private val MasalikDarkColors = darkColorScheme(
    primary = MasalikAmber,
    onPrimary = Color(0xFF090D16),
    primaryContainer = Color(0xFF452806),
    onPrimaryContainer = MasalikAmberSoft,
    secondary = MasalikEmerald,
    onSecondary = Color(0xFF062016),
    tertiary = MasalikSky,
    onTertiary = Color(0xFF062231),
    background = MasalikObsidian,
    onBackground = MasalikIvory,
    surface = MasalikSlate,
    onSurface = MasalikIvory,
    surfaceVariant = MasalikCard,
    onSurfaceVariant = MasalikMuted,
    outline = MasalikBorder,
    error = MasalikCrimson,
    onError = Color.White
)

val MasalikTypography = Typography(
    displayLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = CairoFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp)
)

@Composable
fun MasalikTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = MasalikDarkColors,
            typography = MasalikTypography,
            content = content
        )
    }
}
