package fr.carnetdefaerun.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.FactCard
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme

/** Écran sobre pour une partie du carnet pas encore rédigée. */
@Composable
fun InProgressScreen(crumb: String, title: String, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize()) {
        CarnetTopBar(crumb, onBack = onBack)
        Text(title, style = CarnetTheme.type.headline, color = CarnetTheme.colors.text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Spacer(Modifier.height(8.dp))
        FactCard(stringResource(R.string.in_progress_title), stringResource(R.string.in_progress_body), Modifier.padding(horizontal = 18.dp))
    }
}
