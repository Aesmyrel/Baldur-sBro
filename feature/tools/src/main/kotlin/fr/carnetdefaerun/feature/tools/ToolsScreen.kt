package fr.carnetdefaerun.feature.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.navigation.Destination

/** Onglet Outils : la liste des outils de table. */
@Composable
fun ToolsRoute(onNavigate: (Destination) -> Unit) {
    val c = CarnetTheme.colors
    CarnetSectionScope(CarnetSection.OUTILS) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.tools_crumb))
            Text(stringResource(R.string.tools_title), style = CarnetTheme.type.headline, color = c.text,
                modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp).semantics { heading() })
            Text(stringResource(R.string.tools_intro), style = CarnetTheme.type.reading, color = c.text2,
                modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp))
            ListRow(stringResource(R.string.tools_probability), { onNavigate(Destination.Probability) }, subtitle = stringResource(R.string.tools_probability_sub))
            ListRow(stringResource(R.string.tools_dice), { onNavigate(Destination.Dice) }, subtitle = stringResource(R.string.tools_dice_sub))
        }
    }
}
