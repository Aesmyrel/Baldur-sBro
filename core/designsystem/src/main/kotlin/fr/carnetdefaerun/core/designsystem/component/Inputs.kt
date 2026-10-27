package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Interrupteur : glissière d'encre, curseur en feuille d'or quand actif. Toute la ligne est cliquable. */
@Composable
fun CarnetSwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    val position by animateFloatAsState(if (checked) 1f else 0f, tween(CarnetTheme.motion.short), label = "interrupteur")
    Row(
        modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
            .toggleable(checked, role = Role.Switch) { haptics.tick(); onCheckedChange(it) }
            .padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = CarnetTheme.type.reading.copy(fontSize = CarnetTheme.type.bodySmall.fontSize * 1.08f), color = c.text)
            if (subtitle != null) Text(subtitle, style = CarnetTheme.type.dataSmall, color = c.text2)
        }
        Canvas(Modifier.size(46.dp, 26.dp)) {
            val h = size.height
            drawRoundRect(if (checked) c.gold.copy(alpha = 0.25f) else c.surface, cornerRadius = CornerRadius(h / 2))
            drawRoundRect(if (checked) c.goldDeep else c.line, cornerRadius = CornerRadius(h / 2), style = Stroke(1.dp.toPx()))
            val x = h / 2 + (size.width - h) * position
            drawCircle(if (checked) c.gold else c.muted, h / 2 - 4.dp.toPx(), Offset(x, h / 2))
        }
    }
}

/** Choix exclusif en segments (thème, tolérance aux spoilers). */
@Composable
fun SegmentedChoice(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    val shape = RoundedCornerShape(24.dp)
    Row(modifier.fillMaxWidth().clip(shape).border(1.dp, c.goldDeep, shape).selectableGroup()) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                    .background(if (on) c.gold.copy(alpha = 0.18f) else c.surface)
                    .selectable(on, role = Role.RadioButton) { if (!on) { haptics.tick(); onSelect(i) } }
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = CarnetTheme.type.dataStrong, color = if (on) c.gold else c.text2)
            }
        }
    }
}

/** Curseur (taille du texte de lecture). */
@Composable
fun CarnetSlider(value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, steps: Int, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Slider(
        value = value, onValueChange = onValueChange, valueRange = range, steps = steps, modifier = modifier.padding(horizontal = CarnetTheme.dimens.gutter),
        colors = SliderDefaults.colors(thumbColor = c.gold, activeTrackColor = c.goldDeep, inactiveTrackColor = c.line, activeTickColor = c.onGold, inactiveTickColor = c.muted),
    )
}

/** Champ de recherche : filet d'or, loupe gravée, texte de lecture. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(shape).background(c.surface).border(1.dp, c.goldDeep, shape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(Modifier.size(20.dp)) {
            val k = size.width / 24f
            drawCircle(c.gold, 6.5f * k, Offset(10.5f * k, 10.5f * k), style = Stroke(1.8f * k))
            drawLine(c.gold, Offset(15.5f * k, 15.5f * k), Offset(21f * k, 21f * k), 2f * k, StrokeCap.Round)
        }
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(placeholder, style = CarnetTheme.type.readingItalic, color = c.muted)
            BasicTextField(query, onQueryChange, singleLine = true, textStyle = CarnetTheme.type.reading.copy(color = c.text), cursorBrush = SolidColor(c.gold), modifier = Modifier.fillMaxWidth())
        }
    }
}
