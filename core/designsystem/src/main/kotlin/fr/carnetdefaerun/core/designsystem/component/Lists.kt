package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/**
 * Étiquette de filtre : bordée, l'accent de la section quand elle est choisie. La cible tactile fait 48 dp
 * de haut, la marque visible reste fine.
 */
@Composable
fun ChoiceTag(text: String, selected: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val accent = CarnetTheme.accent
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.heightIn(min = 48.dp).toggleable(selected, role = Role.Checkbox, onValueChange = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = CarnetTheme.type.data,
            color = if (selected) c.text else c.text2,
            maxLines = 1,
            modifier = Modifier
                .clip(shape)
                .background(if (selected) c.selectedTint else Color.Transparent)
                .border(1.dp, if (selected) accent else c.line, shape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Ligne de liste : nom en titre, précision en italique dessous, élément facultatif à droite, filet fin
 * en pied. Hauteur minimale de 56 dp ; le texte passe à la ligne plutôt que d'être coupé.
 */
@Composable
fun ListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = CarnetTheme.colors
    Column(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = CarnetTheme.type.titleSmall, color = c.text)
                if (subtitle != null) {
                    Text(subtitle, style = CarnetTheme.type.readingItalic.copy(fontSize = CarnetTheme.type.bodySmall.fontSize), color = c.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            trailing()
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = CarnetTheme.dimens.gutter).height(1.dp).background(c.line))
    }
}

/** Signet gravé : contour seul, ou rempli d'or quand la fiche est parmi les favoris. */
@Composable
fun BookmarkGlyph(filled: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val k = size.width / 24f
        val path = Path().apply {
            moveTo(6f * k, 3.5f * k); lineTo(18f * k, 3.5f * k); lineTo(18f * k, 21f * k)
            lineTo(12f * k, 16.5f * k); lineTo(6f * k, 21f * k); close()
        }
        if (filled) drawPath(path, color, style = Fill)
        drawPath(path, color, style = Stroke(1.6f * k, join = StrokeJoin.Round))
    }
}
