package app.murmur.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val Scheme = darkColorScheme(
    primary = Color(0xFF9FD8BE),
    onPrimary = Color(0xFF07251A),
    primaryContainer = Color(0xFF244238),
    onPrimaryContainer = Color(0xFFCDEEDD),
    secondary = Color(0xFFE6C98F),
    onSecondary = Color(0xFF2E2108),
    background = Color(0xFF0E1615),
    onBackground = Color(0xFFE5EFEA),
    surface = Color(0xFF16211F),
    onSurface = Color(0xFFE5EFEA),
    surfaceVariant = Color(0xFF1F2E2B),
    onSurfaceVariant = Color(0xFFA8BDB5),
    outline = Color(0xFF3B4F4A),
    error = Color(0xFFE8A39B),
)

@Composable
fun MurmurTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val type = base.copy(
        displayMedium = base.displayMedium.copy(fontFamily = FontFamily.Serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.Serif),
    )
    MaterialTheme(colorScheme = Scheme, typography = type, content = content)
}
