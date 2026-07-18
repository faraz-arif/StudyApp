package dev.study.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import dev.study.app.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider, weight = FontWeight.ExtraBold)
)

val OutfitFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider, weight = FontWeight.ExtraBold)
)

val RobotoFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Roboto"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Roboto"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Roboto"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Roboto"), fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Roboto"), fontProvider = provider, weight = FontWeight.ExtraBold)
)

fun getThemeTypography(fontFamilyStr: String): Typography {
    val family = when (fontFamilyStr) {
        "INTER" -> InterFontFamily
        "OUTFIT" -> OutfitFontFamily
        "ROBOTO" -> RobotoFontFamily
        else -> FontFamily.Default
    }
    
    val baseStyle = TextStyle(fontFamily = family)
    
    return Typography(
        displayLarge = baseStyle.copy(fontWeight = FontWeight.Normal),
        displayMedium = baseStyle.copy(fontWeight = FontWeight.Normal),
        displaySmall = baseStyle.copy(fontWeight = FontWeight.Normal),
        headlineLarge = baseStyle.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = baseStyle.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = baseStyle.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = baseStyle.copy(fontWeight = FontWeight.Bold),
        titleMedium = baseStyle.copy(fontWeight = FontWeight.Medium),
        titleSmall = baseStyle.copy(fontWeight = FontWeight.Medium),
        bodyLarge = baseStyle.copy(fontWeight = FontWeight.Normal),
        bodyMedium = baseStyle.copy(fontWeight = FontWeight.Normal),
        bodySmall = baseStyle.copy(fontWeight = FontWeight.Normal),
        labelLarge = baseStyle.copy(fontWeight = FontWeight.Medium),
        labelMedium = baseStyle.copy(fontWeight = FontWeight.Medium),
        labelSmall = baseStyle.copy(fontWeight = FontWeight.Medium)
    )
}

// ─── Shape tokens ───────────────────────────────────────────────────────────
object StudyShapes {
    val small  = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large  = RoundedCornerShape(16.dp)
    val xLarge = RoundedCornerShape(20.dp)
}

// ─── Spacing constants (4dp grid) ───────────────────────────────────────────
object StudySpacing {
    val space4  =  4.dp
    val space8  =  8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
}

// ─── Preset accent colors for swatches (representative primary per palette) ─
val PresetSwatchColors = mapOf(
    ColorPreset.OCEAN       to Color(0xFF00668B),
    ColorPreset.FOREST      to Color(0xFF2D6A4F),
    ColorPreset.SUNSET      to Color(0xFFD04848),
    ColorPreset.MONOCHROME  to Color(0xFF555555),
    ColorPreset.SLATE       to Color(0xFF4A5568)
)

// Font family declarations
enum class AppFontFamily {
    DEFAULT,
    INTER,
    OUTFIT,
    ROBOTO
}

enum class ColorPreset {
    OCEAN,
    FOREST,
    SUNSET,
    MONOCHROME,
    SLATE
}

// Preset Colors Hex values
// Ocean (Default blue-teal harmonized)
val OceanLight = lightColorScheme(
    primary = Color(0xFF00668B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2E8FF),
    secondary = Color(0xFF4D616C),
    background = Color(0xFFFBFCFF),
    surface = Color(0xFFFBFCFF),
    surfaceVariant = Color(0xFFE0E3E7)
)
val OceanDark = darkColorScheme(
    primary = Color(0xFF76D1FF),
    onPrimary = Color(0xFF00354B),
    primaryContainer = Color(0xFF004D6B),
    secondary = Color(0xFFB5C9D6),
    background = Color(0xFF191C1E),
    surface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFF41474D)
)

// Forest
val ForestLight = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F3DC),
    secondary = Color(0xFF40916C),
    background = Color(0xFFF7FBF7),
    surface = Color(0xFFF7FBF7)
)
val ForestDark = darkColorScheme(
    primary = Color(0xFF52B788),
    onPrimary = Color(0xFF081C15),
    primaryContainer = Color(0xFF1B4332),
    secondary = Color(0xFF74C69D),
    background = Color(0xFF0B1411),
    surface = Color(0xFF0B1411)
)

// Sunset
val SunsetLight = lightColorScheme(
    primary = Color(0xFFD04848),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDADA),
    secondary = Color(0xFFF3B664),
    background = Color(0xFFFFFBFB),
    surface = Color(0xFFFFFBFB)
)
val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    secondary = Color(0xFFE2C08D),
    background = Color(0xFF201A1A),
    surface = Color(0xFF201A1A)
)

// Monochrome
val MonochromeLight = lightColorScheme(
    primary = Color(0xFF222222),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E5E5),
    secondary = Color(0xFF555555),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFAFAFA)
)
val MonochromeDark = darkColorScheme(
    primary = Color(0xFFE5E5E5),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF333333),
    secondary = Color(0xFF999999),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

// Slate
val SlateLight = lightColorScheme(
    primary = Color(0xFF4A5568),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDF2F7),
    secondary = Color(0xFF718096),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF)
)
val SlateDark = darkColorScheme(
    primary = Color(0xFFCBD5E0),
    onPrimary = Color(0xFF1A202C),
    primaryContainer = Color(0xFF2D3748),
    secondary = Color(0xFFA0AEC0),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B)
)

fun getThemeColorScheme(preset: ColorPreset, isDark: Boolean): ColorScheme {
    return when (preset) {
        ColorPreset.OCEAN -> if (isDark) OceanDark else OceanLight
        ColorPreset.FOREST -> if (isDark) ForestDark else ForestLight
        ColorPreset.SUNSET -> if (isDark) SunsetDark else SunsetLight
        ColorPreset.MONOCHROME -> if (isDark) MonochromeDark else MonochromeLight
        ColorPreset.SLATE -> if (isDark) SlateDark else SlateLight
    }
}
