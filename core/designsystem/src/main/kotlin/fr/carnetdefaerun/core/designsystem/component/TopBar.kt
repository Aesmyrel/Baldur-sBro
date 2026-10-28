package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.R
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Barre supérieure des fiches : retour, fil d'Ariane en petites capitales, actions. */
@Composable
fun CarnetTopBar(
    crumb: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            val back = stringResource(R.string.ds_back)
            IconSlot(onBack, back) { color ->
                Canvas(Modifier.size(24.dp)) {
                    val k = size.width / 24f
                    drawPath(Path().apply { moveTo(15f * k, 5f * k); lineTo(8f * k, 12f * k); lineTo(15f * k, 19f * k) }, color,
                        style = Stroke(1.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Text(
            crumb, style = CarnetTheme.type.label, color = CarnetTheme.colors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
        actions()
    }
}

/** Emplacement d'icône cliquable, cible tactile de 48 dp. */
@Composable
fun IconSlot(onClick: () -> Unit, label: String, modifier: Modifier = Modifier, icon: @Composable (Color) -> Unit) {
    Box(
        modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { icon(CarnetTheme.colors.text) }
}

/** Filet de progression de lecture. */
@Composable
fun ReadingProgress(progress: Float, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    Box(modifier.fillMaxWidth().padding(horizontal = CarnetTheme.dimens.gutter).height(2.dp).background(c.line)) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            drawLine(c.gold, Offset(0f, size.height / 2), Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2), size.height)
        }
    }
}
