package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.R
import fr.carnetdefaerun.core.designsystem.draw.VellumGrain
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.shader.CarnetShaders
import fr.carnetdefaerun.core.designsystem.shader.setColor
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import java.util.Random
import kotlinx.coroutines.launch

/**
 * Voile de spoiler : une brume d'encre pailletée d'or qui se dissipe sous le doigt (retour haptique).
 * Il ne montre ni le titre, ni le niveau de ce qu'il cache ; l'acte n'apparaît que si [actLabel] est fourni
 * (réglage explicite du joueur). Le contenu caché n'est jamais composé ici : seul [onReveal] est appelé.
 */
@Composable
fun SpoilerVeil(onReveal: () -> Unit, modifier: Modifier = Modifier, actLabel: String? = null, height: Dp = 92.dp) {
    val c = CarnetTheme.colors
    val motion = CarnetTheme.motion
    val haptics = rememberCarnetHaptics()
    val scope = rememberCoroutineScope()
    val shader = remember { if (motion.ambient) CarnetShaders.create(CarnetShaders.INK_MIST) else null }
    var time by remember { mutableFloatStateOf(0f) }
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    val hole = remember { Animatable(0f) }
    val clear = remember { Animatable(0f) }
    val a11y = stringResource(R.string.ds_veil_a11y)

    // Animation bornée : la brume ondule quelques secondes, puis se fige (budget GPU).
    if (shader != null) {
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                time = (now - start) / 1_000_000_000f
                if (time > 8f && hole.value == 0f) break
            }
        }
    }

    fun reveal(at: Offset) {
        touch = at
        haptics.gestureStart()
        scope.launch {
            hole.animateTo(1f, tween(motion.long))
            clear.animateTo(1f, tween(motion.medium))
            haptics.gestureEnd()
            onReveal()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(CarnetTheme.dimens.radius))
            .semantics {
                role = Role.Button
                contentDescription = a11y
                onClick { onReveal(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    reveal(down.position)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(c.surface)
            val radius = size.maxDimension * 1.2f * hole.value
            if (shader != null) {
                shader.setFloatUniform("uSize", size.width, size.height)
                shader.setFloatUniform("uTime", time)
                shader.setFloatUniform("uClear", clear.value)
                val t = if (touch.isSpecified) touch else center
                shader.setFloatUniform("uTouch", t.x, t.y, radius)
                shader.setColor("uInk", if (c.isDark) c.raised.copy(red = c.raised.red + 0.12f, green = c.raised.green + 0.1f, blue = c.raised.blue + 0.06f) else c.text2)
                shader.setColor("uGold", c.gold)
                drawRect(ShaderBrush(shader))
            } else {
                // Repli statique : nappes de brume en dégradés, grain, paillettes fixes.
                val alpha = 1f - clear.value
                val r = Random(7)
                repeat(7) {
                    val cx = r.nextFloat() * size.width
                    val cy = r.nextFloat() * size.height
                    drawCircle(
                        Brush.radialGradient(listOf(c.text2.copy(alpha = 0.34f * alpha), c.text2.copy(alpha = 0f)), center = Offset(cx, cy), radius = size.height * (0.6f + r.nextFloat())),
                        radius = size.height * 1.6f, center = Offset(cx, cy),
                    )
                }
                drawRect(VellumGrain.brush(), alpha = 0.12f * alpha)
                repeat(36) { drawCircle(c.gold, (0.5f + r.nextFloat()).dp.toPx(), Offset(r.nextFloat() * size.width, r.nextFloat() * size.height), alpha = (0.3f + r.nextFloat() * 0.5f) * alpha) }
                if (touch.isSpecified && radius > 0f) drawCircle(c.surface, radius, touch)
            }
            drawRoundRect(c.goldDeep, cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))))
        }
        if (clear.value < 0.5f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (actLabel != null) stringResource(R.string.ds_veil_message_act, actLabel) else stringResource(R.string.ds_veil_message),
                    style = CarnetTheme.type.label, color = c.gold, textAlign = TextAlign.Center,
                )
                Text(stringResource(R.string.ds_veil_hint), style = CarnetTheme.type.readingItalic, color = c.text, textAlign = TextAlign.Center)
            }
        }
    }
}

private val Offset.isSpecified: Boolean get() = this != Offset.Unspecified
