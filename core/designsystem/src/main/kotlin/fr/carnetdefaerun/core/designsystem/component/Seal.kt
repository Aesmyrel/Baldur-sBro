package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.R
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.GoldLeaf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Case à cocher en forme de sceau : vide, c'est un anneau d'or pointillé ; cochée, un sceau de cire
 * vermillon au bord festonné s'appose (léger rebond, retour haptique). En mouvement réduit, simple fondu.
 */
@Composable
fun SealMark(checked: Boolean, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val motion = CarnetTheme.motion
    val stamp = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        val target = if (checked) 1f else 0f
        if (motion.reduced) stamp.animateTo(target, tween(motion.short))
        else stamp.animateTo(target, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    Canvas(modifier.size(30.dp)) {
        val r = size.minDimension / 2f
        drawCircle(c.goldDeep, r - 1.2.dp.toPx(), style = Stroke(1.2.dp.toPx()))
        drawCircle(c.line, r * 0.72f, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.2.dp.toPx(), 2.dp.toPx()))))
        val s = stamp.value
        if (s > 0.01f) {
            scale(0.6f + 0.4f * s) {
                val a = s.coerceAtMost(1f)
                // Cire coulée : bord festonné irrégulier, anneau imprimé en creux, coche gravée.
                val wax = Path()
                val lobes = 16
                for (i in 0..lobes * 4) {
                    val t = i / (lobes * 4f) * 2f * PI.toFloat()
                    val bump = 1f + 0.06f * cos(t * lobes) + 0.025f * sin(t * 3f + 1.3f)
                    val p = Offset(center.x + r * 0.86f * bump * cos(t), center.y + r * 0.86f * bump * sin(t))
                    if (i == 0) wax.moveTo(p.x, p.y) else wax.lineTo(p.x, p.y)
                }
                wax.close()
                drawPath(wax, c.vermilion, alpha = a)
                drawCircle(Color.Black, r * 0.6f, alpha = 0.22f * a, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White, r * 0.6f - 1.dp.toPx(), alpha = 0.12f * a, style = Stroke(0.6.dp.toPx()))
                val tick = Path().apply {
                    moveTo(center.x - r * 0.3f, center.y + r * 0.02f)
                    lineTo(center.x - r * 0.06f, center.y + r * 0.26f)
                    lineTo(center.x + r * 0.34f, center.y - r * 0.26f)
                }
                // Coche lisible sur la cire (≥ 3:1) : encre sur le vermillon clair, vélin sur le vermillon sombre.
                drawPath(tick, if (c.isDark) c.onGold else GoldLeaf.highlight, alpha = a, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

/** Objectif cochable en un appui : toute la ligne est la cible tactile. */
@Composable
fun ObjectiveRow(
    title: String,
    meta: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
    tagColor: Color = CarnetTheme.colors.text2,
    showDivider: Boolean = true,
) {
    val haptics = rememberCarnetHaptics()
    val state = stringResource(if (checked) R.string.ds_checked else R.string.ds_unchecked)
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .toggleable(checked, role = Role.Checkbox) {
                    if (it) haptics.confirm() else haptics.tick()
                    onCheckedChange(it)
                }
                .semantics { stateDescription = state }
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SealMark(checked)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = CarnetTheme.type.reading.copy(fontSize = CarnetTheme.type.reading.fontSize, lineHeight = CarnetTheme.type.reading.fontSize * 1.2f),
                    color = if (checked) CarnetTheme.colors.text2 else CarnetTheme.colors.text,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                )
                Text(meta, style = CarnetTheme.type.dataSmall, color = CarnetTheme.colors.text2, modifier = Modifier.padding(top = 2.dp))
            }
            if (tag != null) Tag(tag, color = tagColor)
        }
        if (showDivider) HorizontalDivider(color = CarnetTheme.colors.line, thickness = 1.dp)
    }
}

