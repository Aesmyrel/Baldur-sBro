package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/**
 * Mode session : pastille opaque (lisible sur la gravure), bougie allumée quand la session est active.
 * C'est un interrupteur pour TalkBack (état annoncé).
 */
@Composable
fun SessionToggle(label: String, active: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, if (active) c.gold else c.goldDeep, shape)
            .toggleable(active, role = Role.Switch) { haptics.tick(); onToggle(it) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(14.dp, 22.dp)) {
            val w = size.width
            val k = w / 14f
            // Bougie : fût, mèche, flamme dorée si active.
            drawRect(if (active) c.gold else c.muted, topLeft = Offset(4f * k, 10f * k), size = Size(6f * k, 12f * k), style = Stroke(1.dp.toPx()))
            drawLine(if (active) c.gold else c.muted, Offset(7f * k, 10f * k), Offset(7f * k, 7.5f * k), 1.dp.toPx())
            if (active) {
                val flame = Path().apply { moveTo(7f * k, 0.5f * k); quadraticTo(11f * k, 5f * k, 7f * k, 8f * k); quadraticTo(3f * k, 5f * k, 7f * k, 0.5f * k) }
                drawPath(flame, c.goldLeafDecor)
            }
        }
        Text(label, style = CarnetTheme.type.dataStrong, color = if (active) c.gold else c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
