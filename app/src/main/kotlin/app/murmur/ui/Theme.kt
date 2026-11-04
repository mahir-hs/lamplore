package app.murmur.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.murmur.R

/**
 * Murmur palette: midnight and aurora. A deep blue-black night, cool slate surfaces, and one bright accent
 * that runs from mint to violet wherever something is alive (play, selected, progress). The warm paper mat
 * around every cover is the only warm colour, so the pictures glow against the cold night.
 * (Names are kept from the earlier lantern palette: `ember` is now the accent.)
 */
object Ink {
    val night = Color(0xFF070B1A)
    val lamp = Color(0xFF111833)
    val lampHigh = Color(0xFF1B2547)
    /** The accent: aurora mint. Used for links, icons, progress. */
    val ember = Color(0xFF6EE7D8)
    val emberDeep = Color(0xFF3BC9BB)
    /** The accent's other end. */
    val violet = Color(0xFF8B7CFF)
    val moth = Color(0xFFEEF2FF)
    val dusk = Color(0xFF95A1C9)
    val moss = Color(0xFF8CEFB0)
    /** Text on the accent. */
    val onEmber = Color(0xFF041A22)
    val line = Color(0xFF27335C)
    val error = Color(0xFFFF8FA3)
    /** The mat every cover sits on. */
    val paper = Color(0xFFF1E9DA)
    /** Mint to violet: the main button, the play button, selected pills. */
    val aurora = Brush.linearGradient(listOf(Color(0xFF5EEAD4), Color(0xFF8B7CFF)))
    /** The screen background: a faint glow at the top fading into night. */
    val sky = Brush.verticalGradient(listOf(Color(0xFF0E1740), Color(0xFF070B1A), Color(0xFF070B1A)))
}

private val Scheme = darkColorScheme(
    primary = Ink.ember,
    onPrimary = Ink.onEmber,
    primaryContainer = Ink.lampHigh,
    onPrimaryContainer = Ink.moth,
    secondary = Ink.violet,
    onSecondary = Ink.night,
    tertiary = Ink.emberDeep,
    background = Ink.night,
    onBackground = Ink.moth,
    surface = Ink.lamp,
    onSurface = Ink.moth,
    surfaceVariant = Ink.lampHigh,
    onSurfaceVariant = Ink.dusk,
    outline = Ink.line,
    outlineVariant = Ink.line,
    error = Ink.error,
)

// Static instances baked from the variable fonts (fontTools instancer), because runtime axis
// settings were not applied on the test device. Display: wght 500 / opsz 72 / SOFT 30. Title: 600 / 36.
val Serif = FontFamily(
    Font(R.font.fraunces_display, weight = FontWeight.W500),
    Font(R.font.fraunces_title, weight = FontWeight.W600),
)
val Sans = FontFamily(
    Font(R.font.manrope_400, weight = FontWeight.W400),
    Font(R.font.manrope_500, weight = FontWeight.W500),
    Font(R.font.manrope_600, weight = FontWeight.W600),
    Font(R.font.manrope_700, weight = FontWeight.W700),
)

/** Story titles keep the serif; it is the one literary note in an otherwise sharp interface. */
val StoryTitle = TextStyle(fontFamily = Serif, fontWeight = FontWeight.W500, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp)

val MurmurType = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W700, fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.0).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W700, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W700, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W700, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W500, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.W500, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun MurmurTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = MurmurType, content = content)
}
