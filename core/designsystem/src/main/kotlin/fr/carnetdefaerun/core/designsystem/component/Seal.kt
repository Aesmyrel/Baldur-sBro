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

/**
 * Case à cocher en forme de sceau : vide, c'est un anneau d'or pointillé ; cochée, un sceau de cire
 * vermillon s'appose (léger rebond, retour haptique). En mouvement réduit, simple fondu.
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
                drawCircle(c.vermilion, r * 0.86f, alpha = s.coerceAtMost(1f))
                drawCircle(Color.Black, r * 0.86f, alpha = 0.18f * s, style = Stroke(1.5.dp.toPx()))
                val tick = Path().apply {
                    moveTo(center.x - r * 0.36f, center.y + r * 0.02f)
                    lineTo(center.x - r * 0.08f, center.y + r * 0.3f)
                    lineTo(center.x + r * 0.4f, center.y - r * 0.3f)
                }
                drawPath(tick, c.goldLeaf, alpha = s.coerceAtMost(1f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
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
            if (tag != null) Tag(tag, tagColor)
        }
        if (showDivider) HorizontalDivider(color = CarnetTheme.colors.line, thickness = 1.dp)
    }
}

