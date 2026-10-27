package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.haptics.rememberCarnetHaptics
import fr.carnetdefaerun.core.designsystem.theme.CarnetFonts
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Un moment de la partie dans le sélecteur : libellé court (chiffre romain) et libellé complet (TalkBack). */
data class ActChoice(val short: String, val label: String)

/**
 * Sélecteur d'acte en un appui : médaillons reliés par un fil d'or. L'acte courant est en feuille d'or,
 * les actes passés cernés d'or, les actes à venir à peine tracés.
 */
@Composable
fun ActSelector(choices: List<ActChoice>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val haptics = rememberCarnetHaptics()
    Box(modifier.fillMaxWidth().height(60.dp).padding(horizontal = CarnetTheme.dimens.gutter)) {
        Canvas(Modifier.matchParentSize()) {
            val y = size.height / 2f
            val inset = 22.dp.toPx()
            drawLine(
                Brush.horizontalGradient(listOf(c.goldDeep, c.line)), Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx(),
            )
        }
        Row(Modifier.matchParentSize().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            choices.forEachIndexed { index, choice ->
                val now = index == selected
                val past = index < selected
                val diameter by animateDpAsState(if (now) 52.dp else 44.dp, tween(CarnetTheme.motion.medium), label = "acte")
                val textColor by animateColorAsState(
                    when { now -> c.onGold; past -> c.gold; else -> c.text2 }, tween(CarnetTheme.motion.medium), label = "texte",
                )
                Box(
                    Modifier
                        .size(52.dp)
                        .selectable(now, role = Role.RadioButton) {
                            if (!now) { haptics.tick(); onSelect(index) }
                        }
                        .semantics { contentDescription = choice.label },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(diameter)
                            .then(if (now) Modifier.shadow(10.dp, CircleShape, ambientColor = c.gold, spotColor = c.gold) else Modifier)
                            .clip(CircleShape)
                            .then(
                                if (now) Modifier.background(Brush.radialGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFBEBB6), c.goldLeafMid, c.goldDeep)))
                                else Modifier.background(c.bg).border(1.dp, if (past) c.goldDeep else c.line, CircleShape),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(choice.short, style = CarnetTheme.type.dataStrong.copy(fontFamily = CarnetFonts.AlmendraSC, fontSize = if (now) CarnetTheme.type.title.fontSize * 0.75f else CarnetTheme.type.dataStrong.fontSize), color = textColor)
                    }
                }
            }
        }
    }
}

private val fr.carnetdefaerun.core.designsystem.theme.CarnetColors.goldLeafMid get() = androidx.compose.ui.graphics.Color(0xFFD8B260)
