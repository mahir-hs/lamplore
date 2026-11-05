package app.lamplore.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val tagPalette = listOf(
    Color(0xFF6FC3B2), Color(0xFF8E9BF5), Color(0xFFE0B36A), Color(0xFF9A7BD6),
    Color(0xFF7FB77E), Color(0xFFE58F7A), Color(0xFF5FA8E8), Color(0xFFD98BB5),
)

/** One colour per genre or author, the same every time it appears. */
fun entityColor(name: String): Color = tagPalette[(name.hashCode() and 0x7fffffff) % tagPalette.size]

/**
 * A row of thin bars with a label under each. The tallest bar is the brightest; empty slots keep a faint
 * tick so the axis still reads. [labels] may contain blanks to label only some bars.
 */
@Composable
fun MiniBars(values: List<Int>, labels: List<String>, description: String, modifier: Modifier = Modifier, height: Int = 90) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val top = values.indexOf(values.maxOrNull() ?: 0)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height.dp).semantics { contentDescription = description }) {
            val slot = size.width / values.size
            val bar = slot * 0.6f
            values.forEachIndexed { i, v ->
                val x = slot * i + (slot - bar) / 2
                if (v <= 0) {
                    drawRoundRect(Color(0xFF1B2547), Offset(x, size.height - 3.dp.toPx()), Size(bar, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
                } else {
                    val h = (v.toFloat() / max * (size.height - 4.dp.toPx())).coerceAtLeast(4.dp.toPx())
                    drawRoundRect(if (i == top) Color(0xFF8DEBD0) else Color(0xFF2B8A8C), Offset(x, size.height - h), Size(bar, h), CornerRadius(3.dp.toPx()))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Top) {
            labels.forEach { l ->
                Text(l, style = MaterialTheme.typography.labelSmall, color = Ink.dusk, modifier = Modifier.weight(1f), maxLines = 1, softWrap = false)
            }
        }
    }
}
