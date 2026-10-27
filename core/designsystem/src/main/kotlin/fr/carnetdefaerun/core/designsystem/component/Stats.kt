package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.draw.sparklePath
import fr.carnetdefaerun.core.designsystem.illustration.ClassEmblem
import fr.carnetdefaerun.core.designsystem.illustration.drawClassEmblem
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Caractéristique : abréviation, valeur, modificateur en or. */
@Composable
fun AbilityTile(key: String, value: Int, modifier: String, highlighted: Boolean, a11y: String, modifierBox: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius)
    Column(
        modifierBox
            .clip(shape)
            .background(if (highlighted) Brush.verticalGradient(listOf(c.gold.copy(alpha = 0.14f), c.surface)) else Brush.verticalGradient(listOf(c.surface, c.surface)))
            .border(1.dp, if (highlighted) c.goldDeep else c.line, shape)
            .padding(vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = a11y },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(key, style = CarnetTheme.type.labelSmall, color = c.text2)
        Text(value.toString(), style = CarnetTheme.type.numeric, color = c.text, modifier = Modifier.padding(top = 4.dp))
        Text(modifier, style = CarnetTheme.type.dataSmall, color = c.gold)
    }
}

/** Statistique dérivée ; un toucher ouvre le détail du calcul. */
@Composable
fun StatTile(label: String, value: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius)
    Column(
        modifier
            .clip(shape)
            .background(c.surface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.gold else c.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(label, style = CarnetTheme.type.dataSmall, color = c.text2)
        Text(value, style = CarnetTheme.type.numeric.copy(fontSize = CarnetTheme.type.numeric.fontSize * 1.08f), color = c.text, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Un niveau de la frise : emblème de la classe prise, repère de sous-classe ou de don. */
data class LevelPip(val emblem: ClassEmblem, val color: Color, val marker: LevelMarker? = null, val description: String)
enum class LevelMarker { SOUS_CLASSE, DON }

@Composable
fun LevelTrack(levels: List<LevelPip>, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        levels.forEachIndexed { i, pip ->
            Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = pip.description }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(26.dp).clip(CircleShape).background(c.surface).border(1.dp, pip.color.copy(alpha = 0.7f), CircleShape), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(17.dp)) { drawClassEmblem(pip.emblem, pip.color, strokeWidth = 1.4f) }
                }
                Text("${i + 1}", style = CarnetTheme.type.dataSmall, color = c.text2, modifier = Modifier.padding(top = 3.dp))
                Canvas(Modifier.size(10.dp, 14.dp)) {
                    when (pip.marker) {
                        LevelMarker.SOUS_CLASSE -> drawPath(sparklePath(size.width / 2, size.height / 2, size.width / 2), c.gold)
                        LevelMarker.DON -> drawPath(Path().apply {
                            moveTo(size.width / 2, size.height * 0.15f); lineTo(size.width * 0.8f, size.height / 2)
                            lineTo(size.width / 2, size.height * 0.85f); lineTo(size.width * 0.2f, size.height / 2); close()
                        }, c.gold)
                        null -> Unit
                    }
                }
            }
        }
    }
}

/** Ligne du détail d'un calcul : terme, explication, valeur. */
@Composable
fun CalcRow(term: String, explanation: String, value: String, modifier: Modifier = Modifier, total: Boolean = false) {
    val c = CarnetTheme.colors
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(term, style = if (total) CarnetTheme.type.reading.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else CarnetTheme.type.reading.copy(fontSize = CarnetTheme.type.bodySmall.fontSize * 1.06f), color = c.text)
                if (explanation.isNotEmpty()) Text(explanation, style = CarnetTheme.type.dataSmall.copy(fontSize = CarnetTheme.type.data.fontSize * 0.93f), color = c.text2)
            }
            Text(value, style = CarnetTheme.type.numeric.copy(fontSize = CarnetTheme.type.dataStrong.fontSize * 1.2f), color = if (total) c.gold else c.text, textAlign = TextAlign.End)
        }
        if (!total) HorizontalDivider(color = c.line, thickness = 1.dp)
    }
}

/** Emplacements de sorts par niveau : losanges lapis. */
@Composable
fun SpellSlots(slots: List<Pair<String, Int>>, a11y: String, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Row(modifier.clearAndSetSemantics { contentDescription = a11y }, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        slots.forEach { (level, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(level, style = CarnetTheme.type.dataStrong.copy(fontFamily = fr.carnetdefaerun.core.designsystem.theme.CarnetFonts.AlmendraSC), color = c.text)
                repeat(count) {
                    Canvas(Modifier.size(9.dp)) {
                        drawPath(Path().apply { moveTo(size.width / 2, 0f); lineTo(size.width, size.height / 2); lineTo(size.width / 2, size.height); lineTo(0f, size.height / 2); close() }, c.lapis)
                    }
                }
            }
        }
    }
}

