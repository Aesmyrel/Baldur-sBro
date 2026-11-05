package fr.carnetdefaerun.feature.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.Stepper
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.draw.Hatch
import fr.carnetdefaerun.core.designsystem.draw.engravedFrame
import fr.carnetdefaerun.core.designsystem.draw.hatch
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.theme.CarnetColors
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

/** Réservoir et derniers lancers, gardés le temps de l'écran (rotation comprise). */
class DiceViewModel : ViewModel() {
    var pool by mutableStateOf(DicePool(mapOf(20 to 1)))
    var last by mutableStateOf<DiceRoll?>(null)
    var history by mutableStateOf(emptyList<DiceRoll>())

    /** Numéro du lancer : il entre dans l'annonce de TalkBack, pour que deux totaux identiques soient tous deux annoncés. */
    var rolls by mutableStateOf(0)

    fun record(roll: DiceRoll) {
        last = roll
        rolls++
        history = (listOf(roll) + history).take(HISTORY)
    }

    private companion object {
        const val HISTORY = 6
    }
}

@Composable
fun DiceRoute(onBack: () -> Unit) {
    val vm: DiceViewModel = viewModel()
    DiceScreen(vm.pool, vm.last, vm.history, onPool = { vm.pool = it }, onRolled = vm::record, onBack = onBack, rollNumber = vm.rolls)
}

@Composable
fun DiceScreen(
    pool: DicePool,
    last: DiceRoll?,
    history: List<DiceRoll>,
    onPool: (DicePool) -> Unit,
    onRolled: (DiceRoll) -> Unit,
    onBack: () -> Unit,
    random: Random = Random.Default,
    rollNumber: Int = if (last != null) 1 else 0,
) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    val motion = CarnetTheme.motion
    val haptics = rememberCarnetHaptics()
    val scope = rememberCoroutineScope()
    // Animation du lancer : progression de 0 à 1, direction du geste, faces affichées pendant la course.
    val progress = remember { Animatable(1f) }
    var direction by remember { mutableStateOf(Offset(1f, -0.4f)) }
    var tumbling by remember { mutableStateOf<List<Int>?>(null) }

    fun throwDice(fling: Offset) {
        if (pool.isEmpty) return
        val result = pool.roll(random)
        val speed = hypot(fling.x, fling.y)
        if (speed > 0f) direction = Offset(fling.x / speed, fling.y / speed)
        scope.launch {
            if (motion.reduced) {
                tumbling = null
                onRolled(result)
                haptics.confirm()
                return@launch
            }
            haptics.gestureStart()
            progress.snapTo(0f)
            // Quatre rebonds : les faces changent, un cran haptique à chacun ; puis le résultat, confirmé.
            val bounces = listOf(0.18f, 0.42f, 0.64f, 0.82f)
            var next = 0
            progress.animateTo(1f, tween(motion.long, easing = fr.carnetdefaerun.core.designsystem.theme.CarnetMotion.Ink)) {
                if (next < bounces.size && value >= bounces[next]) {
                    tumbling = pool.dice.map { random.nextInt(1, it + 1) }
                    haptics.tick()
                    next++
                }
            }
            tumbling = null
            onRolled(result)
            haptics.confirm()
        }
    }

    CarnetSectionScope(CarnetSection.OUTILS) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.tools_crumb), onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.fillMaxHeight().widthIn(max = CarnetTheme.dimens.readingMaxWidth).fillMaxWidth().wrapContentWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(R.string.dice_title), style = CarnetTheme.type.headline, color = c.text,
                        modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp).semantics { heading() })

                    val faces = tumbling ?: last?.faces?.takeIf { last.pool == pool }
                    // La progression n'est lue qu'au dessin : l'écran ne se recompose pas à chaque image de la course.
                    Tray(pool, faces, { progress.value }, direction, onFling = ::throwDice, Modifier.padding(horizontal = gutter, vertical = 10.dp))
                    // Le total reste en place pendant la course (atténué) : TalkBack annonce un changement, pas une apparition.
                    Result(pool, last, rolling = tumbling != null, rollNumber = rollNumber, Modifier.padding(horizontal = gutter))

                    Row(Modifier.padding(horizontal = gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GoldButton(stringResource(R.string.dice_roll), { throwDice(Offset.Zero) }, enabled = !pool.isEmpty)
                        QuietButton(stringResource(R.string.dice_clear), { onPool(DicePool(modifier = pool.modifier)) }, enabled = !pool.isEmpty)
                    }

                    SectionHeader(stringResource(R.string.dice_choose))
                    TagRow(Modifier.padding(horizontal = gutter, vertical = 6.dp)) {
                        for (sides in DIE_SIDES) DieTile(sides, pool.counts[sides] ?: 0, canAdd = !pool.isFull, onAdd = { onPool(pool.add(sides)) }, onRemove = { onPool(pool.remove(sides)) })
                    }
                    val modLabel = stringResource(R.string.dice_modifier)
                    Stepper(modLabel, pool.modifier, { onPool(pool.copy(modifier = it)) }, -10..20,
                        stringResource(R.string.proba_decrease, modLabel), stringResource(R.string.proba_increase, modLabel),
                        Modifier.padding(horizontal = gutter), valueText = signedValue(pool.modifier))

                    if (history.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.dice_history))
                        Column(Modifier.padding(horizontal = gutter, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (roll in history) {
                                Text(stringResource(R.string.dice_history_line, notation(roll.pool), roll.total), style = CarnetTheme.type.data, color = c.text2)
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Notation du réservoir : « 2d6 + 1d8 + 3 ». */
@Composable
private fun notation(pool: DicePool): String {
    val terms = DIE_SIDES.mapNotNull { sides -> pool.counts[sides]?.let { n -> stringResource(R.string.dice_term, n, sides) } }
    val dice = terms.joinToString(stringResource(R.string.dice_plus))
    return when {
        pool.modifier > 0 -> dice + stringResource(R.string.dice_plus) + pool.modifier
        pool.modifier < 0 -> dice + stringResource(R.string.dice_minus) + (-pool.modifier)
        else -> dice
    }
}

/**
 * Plateau gravé où roulent les dés. Un geste vif (relâché avec de la vitesse) lance ; TalkBack lance par l'action
 * de clic. Pendant la course, les dés glissent dans le sens du geste en tournant, et ralentissent.
 */
@Composable
private fun Tray(pool: DicePool, faces: List<Int>?, progress: () -> Float, direction: Offset, onFling: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val measurer = rememberTextMeasurer()
    val label = if (pool.isEmpty) stringResource(R.string.dice_tray_empty_a11y) else stringResource(R.string.dice_tray_a11y, notation(pool))
    val rollLabel = stringResource(R.string.dice_roll)
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius * 2)
    val numberStyle = CarnetTheme.type.numeric
    Box(
        modifier.fillMaxWidth().height(200.dp).clip(shape).background(c.surface).border(1.dp, c.goldDeep, shape).engravedFrame(c, inset = 8f)
            .pointerInput(pool) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDrag = { change, _ -> tracker.addPosition(change.uptimeMillis, change.position) },
                    onDragEnd = {
                        val v = tracker.calculateVelocity()
                        if (hypot(v.x, v.y) > 600f) onFling(Offset(v.x, v.y))
                    },
                )
            }
            .semantics { contentDescription = label; onClick(label = rollLabel) { onFling(Offset.Zero); true } },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val dice = pool.dice
            if (dice.isEmpty()) return@Canvas
            val progress = progress()
            val perRow = minOf(dice.size, 6)
            val cell = minOf(size.width / (perRow + 0.5f), size.height / ((dice.size + perRow - 1) / perRow + 0.5f))
            val travel = (1 - progress) * size.width * 0.35f
            dice.forEachIndexed { i, sides ->
                val row = i / perRow
                val col = i % perRow
                val rowCount = minOf(perRow, dice.size - row * perRow)
                val x = size.width / 2 + (col - (rowCount - 1) / 2f) * cell - direction.x * travel
                val y = size.height / 2 + (row - ((dice.size - 1) / perRow) / 2f) * cell - direction.y * travel
                val spin = (1 - progress) * 540f * (if (i % 2 == 0) 1 else -1)
                rotate(spin, Offset(x, y)) { drawDie(sides, Offset(x, y), cell * 0.42f, pigment(sides, c), c, engraved = true) }
                val face = faces?.getOrNull(i)
                if (face != null) {
                    // Pendant la course, l'encre est terne ; au repos, la face obtenue passe à l'or.
                    val ink = if (progress < 1f) c.text2 else c.gold
                    val text = measurer.measure(face.toString(), numberStyle.copy(color = ink, fontSize = numberStyle.fontSize * (if (cell < 120f) 0.8f else 1f)))
                    drawText(text, topLeft = Offset(x - text.size.width / 2f, y - text.size.height / 2f))
                }
            }
        }
        if (faces == null && !pool.isEmpty) {
            Text(stringResource(R.string.dice_tray_hint), style = CarnetTheme.type.bodySmall, color = c.text2,
                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp).clearAndSetSemantics {})
        }
        if (pool.isEmpty) {
            Text(stringResource(R.string.dice_empty), style = CarnetTheme.type.readingItalic, color = c.text2,
                modifier = Modifier.align(Alignment.Center).padding(16.dp).clearAndSetSemantics {})
        }
    }
}

/** Le total, en grand, annoncé à chaque lancer ; dessous, les faces et le modificateur, puis l'étendue et la moyenne. */
@Composable
private fun Result(pool: DicePool, roll: DiceRoll?, rolling: Boolean, rollNumber: Int, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Column(modifier.fillMaxWidth()) {
        // Le nœud du total reste monté dès qu'il y a des dés : TalkBack annonce un changement de son texte (tiret,
        // puis total), jamais une apparition, qu'il pourrait taire. Le numéro du lancer rend chaque annonce distincte.
        if (!pool.isEmpty) {
            val current = roll?.takeIf { it.pool == pool }
            val total = if (current != null) stringResource(R.string.dice_total_a11y, rollNumber, current.total) else stringResource(R.string.dice_no_roll)
            Row(Modifier.alpha(if (rolling) 0.35f else 1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(current?.total?.toString() ?: stringResource(R.string.stat_dash), style = CarnetTheme.type.numericLarge, color = if (current != null) c.gold else c.muted,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = total; liveRegion = LiveRegionMode.Polite })
                if (current != null) {
                    val faces = current.faces.joinToString(stringResource(R.string.dice_plus))
                    Text(
                        if (pool.modifier != 0) stringResource(R.string.dice_detail_modifier, faces, signedValue(pool.modifier)) else faces,
                        style = CarnetTheme.type.data, color = c.text2, modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            Text(
                stringResource(R.string.dice_stats, pool.minimum, pool.maximum, String.format(Locale.FRANCE, "%.1f", pool.mean)),
                style = CarnetTheme.type.bodySmall, color = c.text2,
            )
        }
    }
}

/** Tuile d'un type de dé : toucher ajoute, le compteur retire. Chaque bouton fait 48 dp au moins. */
@Composable
private fun DieTile(sides: Int, count: Int, canAdd: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius * 2)
    val addLabel = stringResource(R.string.dice_add, sides)
    val removeLabel = stringResource(R.string.dice_remove, sides)
    val countLabel = pluralStringResource(R.plurals.dice_count_a11y, count, count)
    Row(
        Modifier.heightIn(min = 48.dp).clip(shape).background(if (count > 0) c.selectedTint else c.surface).border(1.dp, if (count > 0) c.gold else c.line, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).clickable(enabled = canAdd, role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = addLabel; stateDescription = countLabel }.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(Modifier.size(22.dp)) { drawDie(sides, center, size.minDimension * 0.46f, pigment(sides, c), c) }
            Text(if (count > 0) stringResource(R.string.dice_count, sides, count) else stringResource(R.string.dice_type, sides),
                style = CarnetTheme.type.dataStrong, color = if (count > 0) c.gold else c.text, modifier = Modifier.clearAndSetSemantics {})
        }
        if (count > 0) {
            Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onRemove).semantics { contentDescription = removeLabel }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(14.dp)) { drawLine(c.text2, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.6.dp.toPx()) }
            }
        }
    }
}

/** Pigment d'enluminure de chaque dé. */
private fun pigment(sides: Int, c: CarnetColors): Color = when (sides) {
    4 -> c.vermilion
    6 -> c.orpiment
    8 -> c.verdigris
    10 -> c.lapis
    100 -> c.sepia
    12 -> c.tyrian
    else -> c.gold
}

/** Dé gravé, vu de dessus : son contour selon le nombre de faces, un filet intérieur pour la profondeur. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDie(sides: Int, center: Offset, radius: Float, ink: Color, c: CarnetColors, engraved: Boolean = false) {
    val corners = when (sides) {
        4 -> 3
        6 -> 4
        8 -> 4
        10, 100 -> 5
        12 -> 5
        else -> 6
    }
    val start = when (sides) {
        6 -> PI / 4
        8 -> 0.0
        else -> -PI / 2
    }
    val outline = Path().apply {
        for (k in 0 until corners) {
            val a = start + 2 * PI * k / corners
            val p = Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat())
            if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(outline, c.raised)
    // Gravure : hachures légères dans la moitié basse, comme l'ombre d'un objet en relief.
    if (engraved) {
        val shade = Path().apply { addPath(outline) }
        clipRect(top = center.y) { hatch(shade, ink, Hatch(spacing = 3.2f, angle = 38f, width = 0.5f, alpha = 0.35f), unit = density) }
    }
    drawPath(outline, ink, style = Stroke(1.4.dp.toPx()))
    // Dé de pourcentage : un second contour intérieur, pour le distinguer du d10 d'un coup d'œil.
    if (sides == 100) {
        val inner = Path().apply {
            for (k in 0 until corners) {
                val a = start + 2 * PI * k / corners
                val p = Offset(center.x + radius * 0.72f * cos(a).toFloat(), center.y + radius * 0.72f * sin(a).toFloat())
                if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(inner, ink, style = Stroke(0.9.dp.toPx()))
    }
    if (sides == 20 || sides == 12 || sides == 10 || sides == 100) {
        // Facette centrale : un petit polygone, comme une face vue de face.
        val inner = Path().apply {
            for (k in 0 until 3) {
                val a = -PI / 2 + 2 * PI * k / 3
                val p = Offset(center.x + radius * 0.55f * cos(a).toFloat(), center.y + radius * 0.55f * sin(a).toFloat())
                if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(inner, ink.copy(alpha = 0.45f), style = Stroke(0.8.dp.toPx()))
    }
}

private fun signedValue(value: Int): String = if (value >= 0) "+$value" else "\u2212${-value}"
