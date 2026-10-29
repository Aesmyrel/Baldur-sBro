package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.carnetdefaerun.core.designsystem.draw.DoubleRule
import fr.carnetdefaerun.core.designsystem.draw.Fleuron
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import kotlin.math.max

/**
 * Titre de section : fleuron, libellé en petites capitales d'or, double filet, action facultative.
 * Ni le libellé ni l'action ne se coupent en plein mot : l'action reste sur la ligne du titre seulement si
 * tout y tient (libellé entier, filet minimal), sinon elle passe sous le filet, alignée à droite ; un mot trop
 * long pour la largeur (grande police, écran étroit) resserre l'interlettrage du libellé.
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.padding(start = CarnetTheme.dimens.gutter, end = CarnetTheme.dimens.gutter, top = 14.dp, bottom = 2.dp)) {
        val base = CarnetTheme.type.label
        val measurer = rememberTextMeasurer()
        // Place du libellé à côté du fleuron (22 dp) et de son écart (10 dp).
        val room = constraints.maxWidth - with(LocalDensity.current) { 32.dp.roundToPx() }
        val density = LocalDensity.current
        val style = remember(label, base, room, density) {
            val longest = label.split(' ').maxOfOrNull { measurer.measure(it, base).size.width } ?: 0
            if (constraints.hasBoundedWidth && longest > room) base.copy(letterSpacing = 0.04.em) else base
        }
        Layout(
            content = {
                Fleuron()
                Text(label, style = style, color = CarnetTheme.accent, modifier = Modifier.semantics { heading() })
                DoubleRule(Modifier.height(8.dp))
                if (actionLabel != null && onAction != null) TextAction(actionLabel, onAction, Modifier.widthIn(min = 48.dp))
            },
        ) { measurables, constraints ->
            val gap = 10.dp.roundToPx()
            val minRule = 24.dp.roundToPx()
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val fleuron = measurables[0].measure(loose)
            val labelNatural = measurables[1].maxIntrinsicWidth(Constraints.Infinity)
            val action = measurables.getOrNull(3)?.measure(loose)
            val lead = fleuron.width + gap
            // Largeur non bornée (rangée défilante) : la largeur naturelle de l'ensemble.
            val width = if (constraints.hasBoundedWidth) constraints.maxWidth
            else lead + labelNatural + gap + minRule + (action?.let { gap + it.width } ?: 0)
            val actionWidth = action?.let { gap + it.width } ?: 0
            val inline = action != null && lead + labelNatural + gap + minRule + actionWidth <= width
            val besideRoom = width - lead - (if (inline) actionWidth else 0)
            // Le filet ne prend de place que si le libellé tient sur une ligne avec lui.
            val withRule = labelNatural + gap + minRule <= besideRoom
            val text = measurables[1].measure(loose.copy(maxWidth = max(0, if (withRule) besideRoom - gap - minRule else besideRoom)))
            val ruleWidth = if (withRule) besideRoom - text.width - gap else 0
            val rule = measurables[2].measure(Constraints.fixed(max(0, ruleWidth), 8.dp.roundToPx()))
            val lineHeight = maxOf(fleuron.height, text.height, if (inline) action.height else 0)
            val height = lineHeight + if (action != null && !inline) action.height else 0
            layout(width, height) {
                fun centered(h: Int) = (lineHeight - h) / 2
                fleuron.placeRelative(0, centered(fleuron.height))
                text.placeRelative(lead, centered(text.height))
                if (withRule) rule.placeRelative(lead + text.width + gap, centered(rule.height))
                if (action != null) {
                    if (inline) action.placeRelative(width - action.width, centered(action.height))
                    else action.placeRelative(width - action.width, lineHeight)
                }
            }
        }
    }
}
