package fr.carnetdefaerun.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetMotion
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/**
 * Feuille montante, posée dans l'écran (pas dans une fenêtre à part) : voile sombre, panneau à filet d'or,
 * poignée décorative. Le voile, le bouton retour et l'action de fermeture du contenu la referment.
 * Glissement de 280 ms, fondu court en mouvement réduit. Hauteur plafonnée ; le contenu défile.
 * À placer en dernier dans une Box plein écran, au-dessus du contenu de l'écran.
 */
@Composable
fun CarnetSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    closeLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = CarnetTheme.colors
    val motion = CarnetTheme.motion
    BackHandler(enabled = visible, onBack = onDismiss)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxSheet = maxHeight * 0.88f
        AnimatedVisibility(visible, enter = fadeIn(tween(motion.short)), exit = fadeOut(tween(motion.short))) {
            Box(
                Modifier.fillMaxSize().background(c.scrim)
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = closeLabel, onClick = onDismiss)
                    .semantics { contentDescription = closeLabel },
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (motion.reduced) fadeIn(tween(motion.short)) else slideInVertically(tween(motion.medium, easing = CarnetMotion.Ink)) { it },
            exit = if (motion.reduced) fadeOut(tween(motion.short)) else slideOutVertically(tween(motion.short)) { it },
        ) {
            val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(max = maxSheet)
                    .clip(shape).background(c.raised)
                    .drawBehind { drawLine(c.goldDeep, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx()) }
                    .semantics { paneTitle = title; isTraversalGroup = true }
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = CarnetTheme.dimens.gutter, vertical = 10.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(c.line))
                }
                content()
                Box(Modifier.height(8.dp))
            }
        }
    }
}
