package fr.carnetdefaerun.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Largeur de la colonne de lecture, en cadratins de la taille de lecture : environ 65 à 75 signes par ligne. */
private const val READING_EMS = 32f

/**
 * Colonne de lecture centrée, plafonnée à environ 32 cadratins de la taille de lecture : elle suit
 * le réglage de taille et la police système, et ne s'étire jamais sur toute la largeur d'un pliable.
 */
@Composable
fun ReadingColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val maxWidth = with(LocalDensity.current) { (CarnetTheme.type.reading.fontSize * READING_EMS).toDp() }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = maxWidth).fillMaxWidth(), content = content)
    }
}

/** Rangée d'étiquettes ou de badges qui passe à la ligne au lieu de sortir de l'écran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
