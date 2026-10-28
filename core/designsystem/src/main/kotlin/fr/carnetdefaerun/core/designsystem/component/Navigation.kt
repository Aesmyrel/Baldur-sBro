package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.cappedScale

/** Destinations principales de l'appli (barre du bas sur téléphone, rail sur grand écran). */
enum class NavGlyph { PARTIE, GRIMOIRE, CODEX, BUILDS, OUTILS }

data class NavDestination(val glyph: NavGlyph, val label: String)

/**
 * Barre de navigation dans la zone du pouce : cinq destinations et la recherche, médaillon d'or
 * à droite. Fond d'encre, filet d'or en lisière, destination active soulignée d'un fleuron.
 */
@Composable
fun CarnetNavigationBar(
    destinations: List<NavDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onSearch: () -> Unit,
    searchLabel: String,
    modifier: Modifier = Modifier,
) {
    val c = CarnetTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(c.navBg)
            .drawBehind {
                drawLine(
                    Brush.horizontalGradient(0f to Color.Transparent, 0.2f to c.goldDeep, 0.8f to c.goldDeep, 1f to Color.Transparent),
                    Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx(),
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(start = 2.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).selectableGroup()) {
                destinations.forEachIndexed { i, d -> NavItem(d, i == selected, { onSelect(i) }, Modifier.weight(1f)) }
            }
            SearchMedallion(onSearch, searchLabel)
        }
    }
}

/** Rail vertical pour les largeurs moyennes et étendues (pliable ouvert, paysage, tablette). */
@Composable
fun CarnetNavigationRail(
    destinations: List<NavDestination>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onSearch: () -> Unit,
    searchLabel: String,
    modifier: Modifier = Modifier,
) {
    val c = CarnetTheme.colors
    Column(
        modifier
            .fillMaxHeight()
            .width(92.dp)
            .background(c.navBg)
            .drawBehind {
                drawLine(c.goldDeep, Offset(size.width, 0f), Offset(size.width, size.height), 1.dp.toPx())
            }
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(vertical = 16.dp)
            .selectableGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchMedallion(onSearch, searchLabel)
        Spacer(Modifier.height(12.dp))
        destinations.forEachIndexed { i, d -> NavItem(d, i == selected, { onSelect(i) }, Modifier.fillMaxWidth().height(72.dp)) }
    }
}

@Composable
private fun NavItem(d: NavDestination, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = CarnetTheme.colors
    val color by animateColorAsState(if (active) c.gold else c.muted, tween(CarnetTheme.motion.short), label = "nav")
    Column(
        modifier.selectable(active, role = Role.Tab, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Canvas(Modifier.size(26.dp)) { drawNavGlyph(d.glyph, color) }
        // Libellé sur une ligne, taille plafonnée : la barre ne se déforme pas à 200 %.
        Text(
            d.label, style = CarnetTheme.type.dataSmall.copy(fontSize = CarnetTheme.type.dataSmall.fontSize.cappedScale(1.4f)),
            color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
        )
        Canvas(Modifier.size(18.dp, 5.dp)) {
            if (active) {
                drawLine(c.goldDeep, Offset(0f, size.height / 2), Offset(size.width * 0.33f, size.height / 2), 0.8.dp.toPx())
                drawLine(c.goldDeep, Offset(size.width * 0.67f, size.height / 2), Offset(size.width, size.height / 2), 0.8.dp.toPx())
                val d2 = Path().apply {
                    moveTo(size.width / 2, 0f); lineTo(size.width / 2 + size.height / 2, size.height / 2)
                    lineTo(size.width / 2, size.height); lineTo(size.width / 2 - size.height / 2, size.height / 2); close()
                }
                drawPath(d2, c.gold)
            }
        }
    }
}

/** Médaillon d'or de la recherche, accessible depuis tous les écrans principaux. */
@Composable
fun SearchMedallion(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Box(
        modifier
            .size(56.dp)
            .shadow(if (c.isDark) 10.dp else 4.dp, CircleShape, ambientColor = c.scrim, spotColor = c.scrim)
            .clip(CircleShape)
            .background(c.goldLeafRadial)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val k = size.width / 24f
            drawCircle(c.onGold, 6f * k, Offset(10.5f * k, 10.5f * k), style = Stroke(2f * k))
            drawLine(c.onGold, Offset(15f * k, 15f * k), Offset(20.5f * k, 20.5f * k), 2.4f * k, StrokeCap.Round)
        }
    }
}

fun DrawScope.drawNavGlyph(glyph: NavGlyph, color: Color) {
    val k = size.minDimension / 24f
    val s = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val thin = Stroke(1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    scale(k, k, pivot = Offset.Zero) {
        when (glyph) {
            NavGlyph.PARTIE -> {
                drawPath(Path().apply { moveTo(5f, 21f); lineTo(12f, 5f); lineTo(19f, 21f) }, color, style = s)
                drawPath(Path().apply { moveTo(8.5f, 21f); quadraticTo(12f, 15f, 15.5f, 21f); moveTo(12f, 13f); quadraticTo(10f, 10f, 12f, 8f); quadraticTo(14f, 10f, 12f, 13f) }, color, style = thin)
            }
            NavGlyph.GRIMOIRE -> {
                drawRoundRect(color, topLeft = Offset(5f, 4f), size = androidx.compose.ui.geometry.Size(14f, 17f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f), style = s)
                drawPath(Path().apply { moveTo(8f, 4f); lineTo(8f, 21f); moveTo(19f, 10f); lineTo(21f, 10f); lineTo(21f, 15f); lineTo(19f, 15f) }, color, style = thin)
            }
            NavGlyph.CODEX -> {
                drawPath(Path().apply {
                    moveTo(4f, 6f); quadraticTo(8f, 4f, 12f, 6f); quadraticTo(16f, 4f, 20f, 6f); lineTo(20f, 20f)
                    quadraticTo(16f, 18f, 12f, 20f); quadraticTo(8f, 18f, 4f, 20f); close(); moveTo(12f, 6f); lineTo(12f, 20f)
                }, color, style = s)
                drawPath(Path().apply { moveTo(6.5f, 10f); lineTo(9.5f, 10f); moveTo(6.5f, 13f); lineTo(9.5f, 13f); moveTo(14.5f, 10f); lineTo(17.5f, 10f) }, color, style = thin)
            }
            NavGlyph.BUILDS -> {
                drawPath(Path().apply { moveTo(12f, 3f); lineTo(20f, 6f); lineTo(20f, 12f); quadraticTo(20f, 18f, 12f, 21f); quadraticTo(4f, 18f, 4f, 12f); lineTo(4f, 6f); close() }, color, style = s)
                drawPath(Path().apply { moveTo(7.5f, 11f); lineTo(12f, 14.5f); lineTo(16.5f, 11f) }, color, style = thin)
            }
            NavGlyph.OUTILS -> {
                drawPath(Path().apply { moveTo(12f, 3f); lineTo(20f, 8f); lineTo(20f, 16f); lineTo(12f, 21f); lineTo(4f, 16f); lineTo(4f, 8f); close() }, color, style = s)
                drawPath(Path().apply {
                    moveTo(12f, 3f); lineTo(12f, 9f); moveTo(4f, 8f); lineTo(12f, 9f); lineTo(20f, 8f)
                    moveTo(12f, 9f); lineTo(7.5f, 16f); lineTo(16.5f, 16f); close(); moveTo(7.5f, 16f); lineTo(12f, 21f); moveTo(16.5f, 16f); lineTo(12f, 21f)
                    moveTo(4f, 16f); lineTo(7.5f, 16f); moveTo(20f, 16f); lineTo(16.5f, 16f)
                }, color, style = Stroke(0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
