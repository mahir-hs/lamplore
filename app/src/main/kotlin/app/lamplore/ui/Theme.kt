package app.lamplore.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.lamplore.R

/**
 * Lamplore palette: midnight and aurora. A deep blue-black night, cool slate surfaces, and one bright accent
 * that runs from mint to violet wherever something is alive (play, selected, progress). The warm paper mat
 * around every cover is the only warm colour, so the pictures glow against the cold night.
 * (Names are kept from the earlier lantern palette: `ember` is now the accent.)
 */
object Ink {
    private val amoled get() = Looks.background == 1

    val night: Color get() = if (amoled) Color(0xFF000000) else Color(0xFF070B1A)
    val lamp: Color get() = if (amoled) Color(0xFF0C0C12) else Color(0xFF111833)
    val lampHigh: Color get() = if (amoled) Color(0xFF17171F) else Color(0xFF1B2547)
    /** The accent: aurora mint by default. Used for links, icons, progress. */
    val ember: Color get() = Looks.accentColors().ember
    val emberDeep: Color get() = Looks.accentColors().deep
    /** The accent's other end. */
    val violet: Color get() = Looks.accentColors().other
    val moth = Color(0xFFEEF2FF)
    val dusk = Color(0xFF95A1C9)
    val moss = Color(0xFF8CEFB0)
    /** Text on the accent. */
    val onEmber: Color get() = Looks.accentColors().on
    val line: Color get() = if (amoled) Color(0xFF24242F) else Color(0xFF27335C)
    val error = Color(0xFFFF8FA3)
    /** The mat every cover sits on. */
    val paper = Color(0xFFF1E9DA)
    /** The accent's gradient: the main button, the play button, selected pills. */
    val aurora: Brush get() = Looks.accentColors().let { Brush.linearGradient(listOf(it.gradFrom, it.gradTo)) }
    /** The screen background: a faint glow at the top fading into night. */
    val sky: Brush get() = if (amoled) Brush.verticalGradient(listOf(Color(0xFF05060F), Color.Black, Color.Black))
    else Brush.verticalGradient(listOf(Color(0xFF0E1740), Color(0xFF070B1A), Color(0xFF070B1A)))
}

/** Colours of one accent. */
class Accent(val ember: Color, val deep: Color, val other: Color, val on: Color, val gradFrom: Color, val gradTo: Color)

/**
 * The look you chose: midnight or black background, and one of a few accents (or the phone's own colour
 * on Android 12+). It only changes colours, so every screen keeps working the same.
 */
object Looks {
    var background by androidx.compose.runtime.mutableIntStateOf(0)
        private set
    var accent by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    private var dynamic: Pair<Color, Color>? = null
    val hasDynamic get() = dynamic != null

    private val aurora = Accent(Color(0xFF6EE7D8), Color(0xFF3BC9BB), Color(0xFF8B7CFF), Color(0xFF041A22), Color(0xFF5EEAD4), Color(0xFF8B7CFF))
    private val sunrise = Accent(Color(0xFFFFB86B), Color(0xFFE8943A), Color(0xFFFF7A8A), Color(0xFF2B1300), Color(0xFFFFC27A), Color(0xFFFF7A8A))
    private val violet = Accent(Color(0xFFB69CFF), Color(0xFF8E70F0), Color(0xFF6FA8FF), Color(0xFF14083A), Color(0xFFB69CFF), Color(0xFF6FA8FF))
    private val rose = Accent(Color(0xFFFF9EC2), Color(0xFFE06A98), Color(0xFFFFB38A), Color(0xFF330A1A), Color(0xFFFF9EC2), Color(0xFFFFB38A))

    fun accentColors(): Accent {
        val d = dynamic
        return when (accent) {
            1 -> sunrise
            2 -> violet
            3 -> rose
            4 -> if (d != null) {
                val on = if (d.first.luminance() > 0.5f) Color(0xFF101010) else Color(0xFFFFFFFF)
                Accent(d.first, androidx.compose.ui.graphics.lerp(d.first, Color.Black, 0.25f), d.second, on, d.first, d.second)
            } else aurora
            else -> aurora
        }
    }

    fun init(ctx: android.content.Context) {
        val p = ctx.getSharedPreferences("murmur_extra", android.content.Context.MODE_PRIVATE)
        background = p.getInt("look_bg", 0)
        accent = p.getInt("look_accent", 0)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            runCatching {
                val d = androidx.compose.material3.dynamicDarkColorScheme(ctx)
                dynamic = d.primary to d.tertiary
            }
        }
    }

    private fun prefs() = app.lamplore.App.context.getSharedPreferences("murmur_extra", android.content.Context.MODE_PRIVATE)
    fun chooseBackground(i: Int) { background = i; prefs().edit().putInt("look_bg", i).apply() }
    fun chooseAccent(i: Int) { accent = i; prefs().edit().putInt("look_accent", i).apply() }
}

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

val LamploreType = Typography(
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
fun LamploreTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
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
    MaterialTheme(colorScheme = scheme, typography = LamploreType, content = content)
}
