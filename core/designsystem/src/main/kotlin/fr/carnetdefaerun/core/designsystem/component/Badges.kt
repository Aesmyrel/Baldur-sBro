package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Étiquette compacte bordée (acte, catégorie, « Manquable »). */
@Composable
fun Tag(text: String, color: Color = CarnetTheme.colors.text2, modifier: Modifier = Modifier) {
    Text(
        text,
        style = CarnetTheme.type.labelSmall,
        color = color,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
    )
}

/** Nature d'une information : canon de BG3, canon de D&D (avec édition), interprétation. */
enum class CanonKind { BG3, DND, INTERPRETATION, NEUTRE }

@Composable
fun CanonBadge(text: String, kind: CanonKind, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val color = when (kind) {
        CanonKind.BG3 -> c.vermilion
        CanonKind.DND -> c.lapis
        CanonKind.INTERPRETATION -> c.tyrian
        CanonKind.NEUTRE -> c.text2
    }
    Tag(text, color, modifier)
}
