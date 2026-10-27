package fr.carnetdefaerun.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.carnetdefaerun.core.designsystem.component.ActSelector
import fr.carnetdefaerun.core.designsystem.component.OrnateCard
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.illustration.VignetteHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.ui.actChoices
import fr.carnetdefaerun.core.ui.labelRes
import fr.carnetdefaerun.core.ui.vignette

@Composable
fun HomeRoute(onNavigate: (Destination) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state, viewModel::selectAct, viewModel::toggleSession, onNavigate)
}

@Composable
fun HomeScreen(state: HomeUiState, onSelectAct: (Act) -> Unit, onToggleSession: () -> Unit, onNavigate: (Destination) -> Unit) {
    CarnetSectionScope(CarnetSection.PARTIE) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            VignetteHeader(
                vignette = state.act.vignette,
                label = stringResource(R.string.home_label, stringResource(state.difficulty.labelRes)),
                title = stringResource(state.act.labelRes),
                trailing = { SessionChip(state.sessionActive, onToggleSession) },
            )
            ActSelector(actChoices(), state.act.ordinal, { onSelectAct(Act.entries[it]) }, Modifier.padding(top = 8.dp))
            SectionHeader(stringResource(R.string.home_shortcuts), Modifier.padding(top = 6.dp))
            Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Shortcut(R.string.home_progress, R.string.home_progress_meta) { onNavigate(Destination.Progress()) }
                Shortcut(R.string.home_companions, R.string.home_companions_meta) { onNavigate(Destination.Companions) }
                Shortcut(R.string.home_notes, R.string.home_notes_meta) { onNavigate(Destination.Notes) }
                Shortcut(R.string.home_settings, R.string.home_settings_meta) { onNavigate(Destination.Settings) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Shortcut(title: Int, meta: Int, onClick: () -> Unit) {
    OrnateCard(onClick = onClick) {
        Text(stringResource(title), style = CarnetTheme.type.titleSmall, color = CarnetTheme.colors.text)
        Text(stringResource(meta), style = CarnetTheme.type.dataSmall, color = CarnetTheme.colors.text2, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SessionChip(active: Boolean, onToggle: () -> Unit) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .height(48.dp)
            .clip(shape)
            .background(if (active) c.gold.copy(alpha = 0.18f) else c.bg.copy(alpha = 0.7f))
            .border(1.dp, if (active) c.gold else c.goldDeep, shape)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(if (active) R.string.home_session_on else R.string.home_session),
            style = CarnetTheme.type.dataStrong, color = if (active) c.gold else c.text,
        )
    }
}
