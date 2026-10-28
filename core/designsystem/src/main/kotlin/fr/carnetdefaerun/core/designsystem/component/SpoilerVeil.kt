package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.R
import fr.carnetdefaerun.core.designsystem.draw.VellumGrain
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.shader.CarnetShaders
import fr.carnetdefaerun.core.designsystem.shader.LocalAmbientAnimation
import fr.carnetdefaerun.core.designsystem.shader.setColor
import fr.carnetdefaerun.core.designsystem.theme.CarnetColors
import fr.carnetdefaerun.core.designsystem.theme.CarnetMotion
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import java.util.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Durée de l'ondulation de la brume, après quoi elle se fige (budget GPU). */
private const val AMBIENT_SECONDS = 8f

/**
 * Voile de spoiler : une brume d'encre pailletée d'or, dissipée par un toucher confirmé
 * (jamais par un défilement qui commence dessus), avec retour haptique.
 * Il ne montre ni le titre, ni le niveau de ce qu'il cache ; l'acte n'apparaît que si [actLabel] est fourni
 * (réglage explicite du joueur). Le contenu caché n'est jamais composé ici : seul [onReveal] est appelé.
 * Le message est posé sur un cartouche opaque : sa lisibilité ne dépend pas de la brume.
 */
@Composable
fun SpoilerVeil(onReveal: () -> Unit, modifier: Modifier = Modifier, actLabel: String? = null, minHeight: Dp = 92.dp) {
    val c = CarnetTheme.colors
    val motion = CarnetTheme.motion
    val haptics = rememberCarnetHaptics()
    val scope = rememberCoroutineScope()
    val slot = LocalAmbientAnimation.current
    val token = remember { Any() }
    val shader = remember(motion.ambient) { if (motion.ambient) CarnetShaders.create(CarnetShaders.INK_MIST) else null }
    var time by remember { mutableFloatStateOf(0f) }
    var onScreen by remember { mutableStateOf(false) }
    var revealing by remember { mutableStateOf(false) }
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    val hole = remember { Animatable(0f) }
    val clear = remember { Animatable(0f) }
    val title = if (actLabel != null) stringResource(R.string.ds_veil_title_act, actLabel) else stringResource(R.string.ds_veil_title)
    val a11y = if (actLabel != null) stringResource(R.string.ds_veil_a11y_act, actLabel) else stringResource(R.string.ds_veil_a11y)
    val revealAction = stringResource(R.string.ds_veil_reveal)

    // La brume n'ondule que visible à l'écran, et seulement si aucun autre shader n'est animé.
    if (shader != null) {
        LaunchedEffect(onScreen, shader) {
            if (!onScreen) return@LaunchedEffect
            snapshotFlow { slot.isAvailableTo(token) }.first { it }
            slot.acquire(token)
            try {
                var last = withFrameNanos { it }
                while (time < AMBIENT_SECONDS || (revealing && clear.value < 1f)) {
                    val now = withFrameNanos { it }
                    time += (now - last) / 1_000_000_000f
                    last = now
                }
            } finally {
                slot.release(token)
            }
        }
        DisposableEffect(Unit) { onDispose { slot.release(token) } }
    }

    fun reveal(at: Offset) {
        if (revealing) return
        revealing = true
        touch = at
        haptics.gestureStart()
        scope.launch {
            // 240 + 140 ms : dans la plage de 150 à 400 ms de la charte ; fondu court en mouvement réduit.
            hole.animateTo(1f, tween(if (motion.reduced) 0 else 240, easing = CarnetMotion.Ink))
            clear.animateTo(1f, tween(if (motion.reduced) 150 else 140))
            haptics.gestureEnd()
            onReveal()
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(CarnetTheme.dimens.radius))
            .onGloballyPositioned { onScreen = !it.boundsInWindow().isEmpty }
            .clearAndSetSemantics {
                role = Role.Button
                contentDescription = a11y
                onClick(label = revealAction) { reveal(Offset.Unspecified); true }
            }
            .pointerInput(Unit) {
                // Toucher confirmé : relâché sans avoir glissé au-delà du seuil de défilement.
                detectTapGestures(onTap = { reveal(it) })
            },
        contentAlignment = Alignment.Center,
    ) {
        val mist = mistColor(c)
        Canvas(Modifier.matchParentSize()) {
            drawRect(c.surface)
            val radius = size.maxDimension * 1.2f * hole.value
            val center = if (touch.isSpecified) touch else center
            if (shader != null) {
                shader.setFloatUniform("uSize", size.width, size.height)
                shader.setFloatUniform("uTime", time)
                shader.setFloatUniform("uClear", clear.value)
                shader.setFloatUniform("uTouch", center.x, center.y, radius)
                shader.setColor("uInk", mist)
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
                        Brush.radialGradient(listOf(mist.copy(alpha = 0.55f * alpha), mist.copy(alpha = 0f)), center = Offset(cx, cy), radius = size.height * (0.6f + r.nextFloat())),
                        radius = size.height * 1.6f, center = Offset(cx, cy),
                    )
                }
                drawRect(VellumGrain.brush(), alpha = 0.1f * alpha)
                repeat(36) { drawCircle(c.gold, (0.5f + r.nextFloat()).dp.toPx(), Offset(r.nextFloat() * size.width, r.nextFloat() * size.height), alpha = (0.3f + r.nextFloat() * 0.5f) * alpha) }
                if (radius > 0f) drawCircle(c.surface, radius, center)
            }
            drawRoundRect(c.goldDeep, cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx()))))
        }
        if (clear.value < 0.5f) {
            Column(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .background(c.surface, RoundedCornerShape(CarnetTheme.dimens.radius))
                    .border(1.dp, c.goldDeep, RoundedCornerShape(CarnetTheme.dimens.radius))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = CarnetTheme.type.label, color = c.gold, textAlign = TextAlign.Center)
                Text(stringResource(R.string.ds_veil_hint), style = CarnetTheme.type.readingItalic, color = c.text, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Brume : encre éclaircie en thème sombre, vélin ombré en thème clair (jamais un brun foncé sous du texte foncé). */
private fun mistColor(c: CarnetColors): Color =
    if (c.isDark) lerp(c.raised, c.text2, 0.16f) else lerp(c.surface, c.text2, 0.2f)

private val Offset.isSpecified: Boolean get() = this != Offset.Unspecified
