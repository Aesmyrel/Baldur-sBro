package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.draw.Fleuron
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Glyphes gravés des raccourcis de « Ma partie ». */
enum class ShortcutGlyph { PROGRESSION, COMPAGNONS, NOTES, REGLAGES }

/**
 * Raccourci de section : médaillon gravé au pigment de la section, titre, ligne d'appoint et fleuron
 * de direction. Toute la carte est la cible tactile.
 */
@Composable
fun SectionShortcut(
    title: String,
    meta: String,
    glyph: ShortcutGlyph,
    section: CarnetSection?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CarnetTheme.colors
    val accent = section?.let(c::accent) ?: c.text2
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius * 2)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Canvas(Modifier.size(40.dp)) {
            val r = size.minDimension / 2
            drawCircle(c.bg, r)
            drawCircle(accent, r - 1.dp.toPx(), style = Stroke(1.2.dp.toPx()))
            drawCircle(accent, r - 4.dp.toPx(), alpha = 0.45f, style = Stroke(0.6.dp.toPx()))
            val inset = 9.dp.toPx()
            scale((size.width - 2 * inset) / size.width) { drawShortcutGlyph(glyph, accent) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = CarnetTheme.type.titleSmall, color = c.text)
            Text(meta, style = CarnetTheme.type.dataSmall, color = c.text2, modifier = Modifier.padding(top = 2.dp))
        }
        Fleuron(Modifier)
    }
}

private fun DrawScope.drawShortcutGlyph(glyph: ShortcutGlyph, color: Color) {
    val k = size.minDimension / 24f
    val s = Stroke(1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    scale(k, k, pivot = Offset.Zero) {
        when (glyph) {
            // Fanion planté sur un tertre : le chemin parcouru.
            ShortcutGlyph.PROGRESSION -> {
                drawLine(color, Offset(8f, 20f), Offset(8f, 4f), 1.4f, StrokeCap.Round)
                drawPath(Path().apply { moveTo(8f, 4.5f); lineTo(18f, 7f); lineTo(8f, 10.5f) }, color, style = s)
                drawPath(Path().apply { moveTo(3f, 21f); quadraticTo(12f, 15f, 21f, 21f) }, color, style = s)
            }
            // Deux anneaux entrelacés : les liens du groupe.
            ShortcutGlyph.COMPAGNONS -> {
                drawCircle(color, 5.5f, Offset(9f, 12f), style = s)
                drawCircle(color, 5.5f, Offset(15f, 12f), style = s)
            }
            // Plume d'oie.
            ShortcutGlyph.NOTES -> {
                drawPath(Path().apply { moveTo(5f, 20f); quadraticTo(9f, 9f, 19f, 3f); quadraticTo(17f, 13f, 7f, 17f) }, color, style = s)
                drawLine(color, Offset(5f, 20f), Offset(13f, 10f), 0.9f, StrokeCap.Round)
            }
            // Rosace à huit pointes : les réglages du carnet.
            ShortcutGlyph.REGLAGES -> {
                drawCircle(color, 3.2f, Offset(12f, 12f), style = s)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val cos = kotlin.math.cos(a).toFloat()
                    val sin = kotlin.math.sin(a).toFloat()
                    drawLine(color, Offset(12f + 5.5f * cos, 12f + 5.5f * sin), Offset(12f + 9f * cos, 12f + 9f * sin), 1.4f, StrokeCap.Round)
                }
            }
        }
    }
}
