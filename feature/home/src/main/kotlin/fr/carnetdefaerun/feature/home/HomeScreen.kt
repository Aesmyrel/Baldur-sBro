package fr.carnetdefaerun.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.carnetdefaerun.core.designsystem.component.ActSelector
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.SectionShortcut
import fr.carnetdefaerun.core.designsystem.component.SessionToggle
import fr.carnetdefaerun.core.designsystem.component.ShortcutGlyph
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.illustration.VignetteHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
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
                trailing = {
                    SessionToggle(
                        stringResource(if (state.sessionActive) R.string.home_session_on else R.string.home_session),
                        state.sessionActive, { onToggleSession() },
                    )
                },
            )
            ActSelector(actChoices(), state.act.ordinal, { onSelectAct(Act.entries[it]) }, Modifier.padding(top = 8.dp))
            ReadingColumn {
                SectionHeader(stringResource(R.string.home_shortcuts), Modifier.padding(top = 6.dp))
                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Shortcut(R.string.home_progress, R.string.home_progress_meta, ShortcutGlyph.PROGRESSION, CarnetSection.PROGRESSION) { onNavigate(Destination.Progress()) }
                    Shortcut(R.string.home_companions, R.string.home_companions_meta, ShortcutGlyph.COMPAGNONS, CarnetSection.COMPAGNONS) { onNavigate(Destination.Companions) }
                    Shortcut(R.string.home_notes, R.string.home_notes_meta, ShortcutGlyph.NOTES, CarnetSection.OUTILS) { onNavigate(Destination.Notes) }
                    Shortcut(R.string.home_settings, R.string.home_settings_meta, ShortcutGlyph.REGLAGES, null) { onNavigate(Destination.Settings) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Shortcut(title: Int, meta: Int, glyph: ShortcutGlyph, section: CarnetSection?, onClick: () -> Unit) {
    SectionShortcut(stringResource(title), stringResource(meta), glyph, section, onClick)
}
