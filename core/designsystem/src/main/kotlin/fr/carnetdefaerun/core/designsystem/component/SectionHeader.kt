package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.draw.DoubleRule
import fr.carnetdefaerun.core.designsystem.draw.Fleuron
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import kotlin.math.max

/**
 * Titre de section : fleuron, libellé en petites capitales d'or, double filet, action facultative.
 * L'action ne se coupe jamais : quand elle ne tient plus à côté du libellé (grande police, écran étroit),
 * elle passe sous le filet, alignée à droite.
 */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Layout(
        content = {
            Fleuron()
            Text(label, style = CarnetTheme.type.label, color = CarnetTheme.accent, modifier = Modifier.semantics { heading() })
            DoubleRule(Modifier.height(8.dp))
            if (actionLabel != null && onAction != null) TextAction(actionLabel, onAction, Modifier.widthIn(min = 48.dp))
        },
        modifier = modifier.padding(start = CarnetTheme.dimens.gutter, end = CarnetTheme.dimens.gutter, top = 14.dp, bottom = 2.dp),
    ) { measurables, constraints ->
        val gap = 10.dp.roundToPx()
        val minRule = 24.dp.roundToPx()
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val fleuron = measurables[0].measure(loose)
        val action = measurables.getOrNull(3)?.measure(loose.copy(maxWidth = width))
        val lead = fleuron.width + gap
        // L'action reste sur la ligne du titre si le libellé garde au moins la moitié de la largeur.
        val inline = action != null && width - lead - gap - minRule - gap - action.width >= width / 2
        val labelRoom = width - lead - gap - minRule - (if (inline) gap + action!!.width else 0)
        val text = measurables[1].measure(loose.copy(maxWidth = max(0, labelRoom)))
        val ruleWidth = width - lead - text.width - gap - (if (inline) gap + action!!.width else 0)
        val rule = measurables[2].measure(Constraints.fixed(max(0, ruleWidth), 8.dp.roundToPx()))
        val lineHeight = maxOf(fleuron.height, text.height, if (inline) action!!.height else 0)
        val height = lineHeight + if (action != null && !inline) action.height else 0
        layout(width, height) {
            fun centered(h: Int) = (lineHeight - h) / 2
            fleuron.placeRelative(0, centered(fleuron.height))
            text.placeRelative(lead, centered(text.height))
            rule.placeRelative(lead + text.width + gap, centered(rule.height))
            if (action != null) {
                if (inline) action.placeRelative(width - action.width, centered(action.height))
                else action.placeRelative(width - action.width, lineHeight)
            }
        }
    }
}
