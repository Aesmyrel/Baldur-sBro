package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.draw.DoubleRule
import fr.carnetdefaerun.core.designsystem.draw.Fleuron
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Titre de section : fleuron, libellé en petites capitales d'or, double filet, action facultative. */
@Composable
fun SectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = CarnetTheme.dimens.gutter, end = CarnetTheme.dimens.gutter, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Fleuron()
        Text(label, style = CarnetTheme.type.label, color = CarnetTheme.accent, modifier = Modifier.semantics { heading() })
        DoubleRule(Modifier.weight(1f).height(8.dp))
        if (actionLabel != null && onAction != null) {
            TextAction(actionLabel, onAction, Modifier.widthIn(min = 48.dp))
        }
    }
}
