package app.murmur.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.murmur.R

/** Murmur palette: a lantern in a dark room. */
object Ink {
    val night = Color(0xFF171311)
    val lamp = Color(0xFF241D19)
    val lampHigh = Color(0xFF302723)
    val ember = Color(0xFFE0A458)
    val emberDeep = Color(0xFFB9803A)
    val moth = Color(0xFFF2E8DC)
    val dusk = Color(0xFF9C8E82)
    val moss = Color(0xFF7FA38A)
    val onEmber = Color(0xFF2A1C0B)
    val line = Color(0xFF3A302A)
    /** The mat every cover sits on. */
    val paper = Color(0xFFEFE3CF)
}

private val Scheme = darkColorScheme(
    primary = Ink.ember,
    onPrimary = Ink.onEmber,
    primaryContainer = Ink.lampHigh,
    onPrimaryContainer = Ink.moth,
    secondary = Ink.moss,
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
    error = Color(0xFFE39C8E),
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

val MurmurType = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.W500, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.W500, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.W500, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.W500, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.W600, fontSize = 18.sp, lineHeight = 24.sp),
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
