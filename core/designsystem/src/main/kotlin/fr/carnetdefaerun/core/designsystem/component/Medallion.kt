package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.illustration.ClassEmblem
import fr.carnetdefaerun.core.designsystem.illustration.drawClassEmblem
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Médaillon : cercle cerné de feuille d'or, anneau pointillé, emblème gravé au centre. */
@Composable
fun Medallion(emblem: ClassEmblem, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val c = CarnetTheme.colors
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(c.surface, r)
        drawCircle(c.goldLeaf, r - 0.6.dp.toPx(), style = Stroke(1.2.dp.toPx()))
        drawCircle(c.goldDeep, r * 0.85f, style = Stroke(0.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 2.2.dp.toPx()))))
        val inner = r * 1.1f
        translate(center.x - inner / 2f, center.y - inner / 2f) {
            drawClassEmblem(emblem, c.gold, strokeWidth = 1.25f, box = inner)
        }
    }
}

/** Membre du groupe : médaillon, nom, classe. Décrit en une phrase pour TalkBack. */
@Composable
fun PartyMember(name: String, className: String, emblem: ClassEmblem, modifier: Modifier = Modifier) {
    Column(
        modifier.clearAndSetSemantics { contentDescription = "$name, $className" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Medallion(emblem, size = 50.dp)
        Text(name, style = CarnetTheme.type.bodySmall, color = CarnetTheme.colors.text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        Text(className, style = CarnetTheme.type.dataSmall, color = CarnetTheme.colors.text2, textAlign = TextAlign.Center)
    }
}

