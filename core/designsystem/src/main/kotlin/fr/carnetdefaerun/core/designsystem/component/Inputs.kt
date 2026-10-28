package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
            Text(title, style = CarnetTheme.type.bodySmall, color = c.text)
            if (subtitle != null) Text(subtitle, style = CarnetTheme.type.dataSmall, color = c.text2)
        }
        Canvas(Modifier.size(46.dp, 26.dp)) {
            val h = size.height
            drawRoundRect(if (checked) c.gold.copy(alpha = 0.25f) else c.surface, cornerRadius = CornerRadius(h / 2))
            // Contour en `muted` quand éteint : la glissière reste visible (≥ 3:1).
            drawRoundRect(if (checked) c.goldDeep else c.muted, cornerRadius = CornerRadius(h / 2), style = Stroke(1.dp.toPx()))
            val x = h / 2 + (size.width - h) * position
            drawCircle(if (checked) c.gold else c.muted, h / 2 - 4.dp.toPx(), Offset(x, h / 2))
        }
    }
}

/**
 * Choix exclusif (thème, difficulté, tolérance). En segments quand chaque libellé tient sur une ligne ;
 * sinon (grande police, écran étroit) en liste verticale d'anneaux gravés : jamais de mot coupé.
 * Le choix courant porte une marque de forme (filet et losange), pas seulement une couleur.
 */
@Composable
fun SegmentedChoice(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val style = CarnetTheme.type.dataStrong
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widest = remember(options, style, density) { options.maxOf { measurer.measure(it, style).size.width } }
        val needed = with(density) { widest.toDp() } + 28.dp
        if (needed * options.size <= maxWidth) Segments(options, selected, onSelect) else ChoiceList(options, selected, onSelect)
    }
}

@Composable
private fun Segments(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    val shape = RoundedCornerShape(24.dp)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape).border(1.dp, c.goldDeep, shape).selectableGroup()) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Column(
                Modifier.weight(1f).fillMaxHeight().defaultMinSize(minHeight = 48.dp)
                    .background(if (on) c.gold.copy(alpha = 0.18f) else c.surface)
                    .selectable(on, role = Role.RadioButton) { if (!on) { haptics.tick(); onSelect(i) } }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(label, style = CarnetTheme.type.dataStrong, color = if (on) c.gold else c.text2, softWrap = false, textAlign = TextAlign.Center)
                Spacer(Modifier.height(3.dp))
                Box(Modifier.width(18.dp).height(2.dp).background(if (on) c.gold else c.surface))
            }
        }
    }
}

@Composable
private fun ChoiceList(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius * 3)
    Column(Modifier.fillMaxWidth().clip(shape).border(1.dp, c.goldDeep, shape).background(c.surface).selectableGroup()) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .selectable(on, role = Role.RadioButton) { if (!on) { haptics.tick(); onSelect(i) } }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val r = size.minDimension / 2
                    drawCircle(if (on) c.gold else c.muted, r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    if (on) drawCircle(c.gold, r * 0.45f)
                }
                Text(label, style = CarnetTheme.type.dataStrong, color = if (on) c.gold else c.text, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Curseur gravé : filet d'or à graduations, médaillon en losange de feuille d'or (cible de 48 dp
 * assurée par le composant de base). [valueLabel] est lu par TalkBack (« 110 % »).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarnetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    modifier: Modifier = Modifier,
) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    Slider(
        value = value,
        onValueChange = { if (it != value) haptics.tick(); onValueChange(it) },
        valueRange = range,
        steps = steps,
        modifier = modifier.padding(horizontal = CarnetTheme.dimens.gutter).semantics { stateDescription = valueLabel },
        thumb = {
            Canvas(Modifier.size(26.dp)) {
                val w = size.width
                val diamond = Path().apply {
                    moveTo(w / 2, 1.dp.toPx()); lineTo(w - 1.dp.toPx(), w / 2); lineTo(w / 2, w - 1.dp.toPx()); lineTo(1.dp.toPx(), w / 2); close()
                }
                drawPath(diamond, c.goldLeaf)
                drawPath(diamond, c.onGold, style = Stroke(1.dp.toPx()))
                drawCircle(c.onGold, 1.6.dp.toPx())
            }
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                val y = size.height / 2
                val fraction = (state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)
                drawLine(c.line, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
                drawLine(c.gold, Offset(0f, y), Offset(size.width * fraction, y), 2.dp.toPx(), StrokeCap.Round)
                val ticks = state.steps + 2
                for (i in 0 until ticks) {
                    val x = size.width * i / (ticks - 1)
                    val tall = i == 0 || i == ticks - 1
                    drawLine(if (x <= size.width * fraction) c.gold else c.muted, Offset(x, y - (if (tall) 6 else 4).dp.toPx()), Offset(x, y + (if (tall) 6 else 4).dp.toPx()), 1.dp.toPx())
                }
            }
        },
    )
}

/** Champ de recherche : filet d'or, loupe gravée, texte de lecture. Il grandit avec la police. */
@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(26.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = CarnetTheme.type.reading.copy(color = c.text),
        cursorBrush = SolidColor(c.gold),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        decorationBox = { field ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(shape).background(c.surface).border(1.dp, c.goldDeep, shape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    field()
                }
            }
        },
    )
}
