package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetFonts
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Lettrine enluminée : initiale en feuille d'or sur fond lapis, cadre d'or, rinceaux et points d'or. */
@Composable
fun Lettrine(letter: Char, size: Dp, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val k = this.size.width / 76f
            drawRoundRect(c.lettrineGround, cornerRadius = CornerRadius(2f * k))
            drawRect(c.goldLeaf, topLeft = Offset(3.5f * k, 3.5f * k), size = Size(69f * k, 69f * k), style = Stroke(1.4f * k))
            val vines = Path().apply {
                moveTo(8f * k, 66f * k); cubicTo(14f * k, 50f * k, 6f * k, 40f * k, 16f * k, 30f * k)
                cubicTo(22f * k, 24f * k, 30f * k, 30f * k, 24f * k, 36f * k); cubicTo(20f * k, 40f * k, 14f * k, 34f * k, 20f * k, 30f * k)
                moveTo(68f * k, 10f * k); cubicTo(62f * k, 26f * k, 70f * k, 36f * k, 60f * k, 46f * k)
                cubicTo(54f * k, 52f * k, 46f * k, 46f * k, 52f * k, 40f * k); cubicTo(56f * k, 36f * k, 62f * k, 42f * k, 56f * k, 46f * k)
            }
            drawPath(vines, c.lettrineVine, alpha = 0.9f, style = Stroke(1.1f * k))
            listOf(10f to 12f, 64f to 64f, 12f to 58f, 62f to 18f).forEach { (x, y) -> drawCircle(c.goldLeaf, 1.6f * k, Offset(x * k, y * k)) }
        }
        Text(
            letter.toString(),
            style = TextStyle(fontFamily = CarnetFonts.Titres, fontSize = with(LocalDensity.current) { (size * 0.74f).toSp() }, brush = c.goldLeaf, textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = size * 0.04f),
        )
    }
}

/**
 * Paragraphe d'ouverture avec lettrine : le texte s'enroule sur [lines] lignes à côté de l'initiale,
 * puis reprend la pleine largeur. TalkBack lit le paragraphe entier d'un seul tenant.
 */
@Composable
fun DropCapParagraph(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = CarnetTheme.type.reading,
    lines: Int = 3,
) {
    if (text.isEmpty()) return
    val c = CarnetTheme.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val first = text.first()
    val rest = text.substring(1)
    BoxWithConstraints(modifier.clearAndSetSemantics { this.text = AnnotatedString(text) }) {
        val lineHeight = with(density) { style.lineHeight.toDp() }
        val capSize = lineHeight * lines - 4.dp
        val gap = 12.dp
        val sideWidth = with(density) { (maxWidth - capSize - gap).roundToPx().coerceAtLeast(1) }
        val split = remember(rest, style, sideWidth) {
            val layout = measurer.measure(rest, style.copy(color = c.text), constraints = Constraints(maxWidth = sideWidth))
            if (layout.lineCount > lines) layout.getLineEnd(lines - 1, visibleEnd = false) else rest.length
        }
        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Lettrine(first, capSize, Modifier.padding(top = 4.dp))
                Text(rest.substring(0, split), style = style, color = c.text)
            }
            if (split < rest.length) Text(rest.substring(split).trimStart(), style = style, color = c.text)
        }
    }
}
