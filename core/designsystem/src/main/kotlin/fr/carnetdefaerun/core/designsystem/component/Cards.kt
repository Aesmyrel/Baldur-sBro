package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.R
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Carte de base : surface, filet d'ombre, coins à peine adoucis (un feuillet, pas une « card » Material). */
@Composable
fun OrnateCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CarnetTheme.colors.line,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.raised, c.surface)))
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

/** Alerte « point de non-retour » : sablier gravé, titre en vermillon, action en or. */
@Composable
fun AlertCard(title: String, body: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    OrnateCard(modifier, borderColor = c.vermilion.copy(alpha = 0.55f), onClick = onAction) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Hourglass(c.vermilion, Modifier.size(34.dp, 40.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = CarnetTheme.type.dataStrong.copy(fontSize = CarnetTheme.type.dataStrong.fontSize * 1.07f), color = c.vermilion)
                Spacer(Modifier.height(3.dp))
                Text(body, style = CarnetTheme.type.reading.copy(fontSize = CarnetTheme.type.bodySmall.fontSize * 1.03f, lineHeight = CarnetTheme.type.bodySmall.lineHeight), color = c.text)
                Text(stringResource(R.string.ds_action_more, actionLabel), style = CarnetTheme.type.dataStrong, color = c.gold, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun Hourglass(color: Color, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val k = size.width / 38f
        val s = Stroke(1.3f * k)
        drawLine(color, androidx.compose.ui.geometry.Offset(8f * k, 3f * k), androidx.compose.ui.geometry.Offset(30f * k, 3f * k), 1.6f * k)
        drawLine(color, androidx.compose.ui.geometry.Offset(8f * k, 41f * k), androidx.compose.ui.geometry.Offset(30f * k, 41f * k), 1.6f * k)
        val glass = Path().apply {
            moveTo(10f * k, 4f * k); quadraticTo(10f * k, 16f * k, 19f * k, 22f * k); quadraticTo(28f * k, 16f * k, 28f * k, 4f * k)
            moveTo(10f * k, 40f * k); quadraticTo(10f * k, 28f * k, 19f * k, 22f * k); quadraticTo(28f * k, 28f * k, 28f * k, 40f * k)
        }
        drawPath(glass, color, style = s)
        val sand = Path().apply { moveTo(14f * k, 36f * k); quadraticTo(19f * k, 30f * k, 24f * k, 36f * k); close() }
        drawPath(sand, color)
    }
}

/** « Le saviez-vous ? » : première lettre en vermillon (rubrique), texte de lecture. */
@Composable
fun FactCard(label: String, text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = CarnetTheme.colors
    OrnateCard(modifier, onClick = onClick) {
        Text(label, style = CarnetTheme.type.label, color = c.gold)
        Spacer(Modifier.height(6.dp))
        val rubric = buildAnnotatedString {
            if (text.isNotEmpty()) {
                withStyle(SpanStyle(color = c.vermilion)) { append(text.first()) }
                append(text.substring(1))
            }
        }
        Text(rubric, style = CarnetTheme.type.reading.copy(fontSize = CarnetTheme.type.reading.fontSize * 0.94f), color = c.text)
    }
}

/** Ligne d'info compacte, alignée sur la colonne : libellé + valeur. */
@Composable
fun InfoLine(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CarnetTheme.type.data, color = CarnetTheme.colors.text2, modifier = Modifier.weight(1f))
        Text(value, style = CarnetTheme.type.dataStrong, color = CarnetTheme.colors.text)
    }
}
