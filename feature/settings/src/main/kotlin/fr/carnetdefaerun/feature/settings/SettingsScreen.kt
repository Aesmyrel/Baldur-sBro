package fr.carnetdefaerun.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.carnetdefaerun.core.designsystem.component.ActSelector
import fr.carnetdefaerun.core.designsystem.component.CarnetSlider
import fr.carnetdefaerun.core.designsystem.component.CarnetSwitchRow
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.OrnateCard
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SegmentedChoice
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.settings.AppTheme
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import fr.carnetdefaerun.core.ui.actChoices
import fr.carnetdefaerun.core.ui.labelRes

@Composable
fun SettingsRoute(onNavigate: (Destination) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    SettingsScreen(prefs, viewModel::update, onNavigate, onBack)
}

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    onUpdate: ((UserPreferences) -> UserPreferences) -> Unit,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
) {
    val c = CarnetTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CarnetTopBar(stringResource(R.string.settings_crumb), onBack = onBack)
        Text(stringResource(R.string.settings_title), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = 24.dp))

        SectionHeader(stringResource(R.string.settings_progress))
        Label(R.string.settings_act)
        ActSelector(actChoices(), prefs.currentAct.ordinal, { i -> onUpdate { it.copy(currentAct = Act.entries[i]) } })
        Label(R.string.settings_difficulty)
        SegmentedChoice(
            Difficulty.entries.take(4).map { stringResource(it.labelRes) },
            Difficulty.entries.indexOf(prefs.difficulty).coerceAtMost(3),
            { i -> onUpdate { it.copy(difficulty = Difficulty.entries[i]) } },
            Modifier.padding(horizontal = 18.dp),
        )

        SectionHeader(stringResource(R.string.settings_spoilers))
        Label(R.string.settings_tolerance)
        ToleranceChoice(prefs.tolerance) { t -> onUpdate { it.copy(tolerance = t) } }
        Help(
            when (prefs.tolerance) {
                Tolerance.NONE -> R.string.settings_tolerance_help_none
                Tolerance.LIGHT -> R.string.settings_tolerance_help_light
                Tolerance.FULL -> R.string.settings_tolerance_help_full
            },
        )
        Label(R.string.settings_other_works)
        ToleranceChoice(prefs.otherWorksTolerance) { t -> onUpdate { it.copy(otherWorksTolerance = t) } }
        Help(R.string.settings_other_works_help)
        CarnetSwitchRow(stringResource(R.string.settings_show_act), stringResource(R.string.settings_show_act_help), prefs.showActOfHidden, { v -> onUpdate { it.copy(showActOfHidden = v) } })

        SectionHeader(stringResource(R.string.settings_appearance))
        SegmentedChoice(
            listOf(stringResource(R.string.settings_theme_dark), stringResource(R.string.settings_theme_oled), stringResource(R.string.settings_theme_parchment)),
            prefs.theme.ordinal,
            { i -> onUpdate { it.copy(theme = AppTheme.entries[i]) } },
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
        )
        Label(R.string.settings_reading_size)
        val scales = UserPreferences.READING_SCALES
        CarnetSlider(
            value = scales.indexOf(prefs.readingScale).coerceAtLeast(0).toFloat(),
            onValueChange = { v -> onUpdate { it.copy(readingScale = scales[v.toInt().coerceIn(scales.indices)]) } },
            range = 0f..(scales.size - 1).toFloat(),
            steps = scales.size - 2,
        )
        Text(stringResource(R.string.settings_reading_sample), style = CarnetTheme.type.reading, color = c.text, modifier = Modifier.padding(horizontal = 24.dp))
        CarnetSwitchRow(stringResource(R.string.settings_reduce_motion), stringResource(R.string.settings_reduce_motion_help), prefs.reduceMotion, { v -> onUpdate { it.copy(reduceMotion = v) } })

        SectionHeader(stringResource(R.string.settings_session))
        CarnetSwitchRow(stringResource(R.string.settings_keep_screen_on), stringResource(R.string.settings_keep_screen_on_help), prefs.sessionActive, { v -> onUpdate { it.copy(sessionActive = v) } })

        SectionHeader(stringResource(R.string.settings_about))
        OrnateCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), onClick = { onNavigate(Destination.Credits) }) {
            Text(stringResource(R.string.settings_credits), style = CarnetTheme.type.titleSmall, color = c.text)
            Text(stringResource(R.string.settings_credits_meta), style = CarnetTheme.type.dataSmall, color = c.text2)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ToleranceChoice(value: Tolerance, onChange: (Tolerance) -> Unit) {
    SegmentedChoice(Tolerance.entries.map { stringResource(it.labelRes) }, value.ordinal, { onChange(Tolerance.entries[it]) }, Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun Label(res: Int) {
    Text(stringResource(res), style = CarnetTheme.type.data, color = CarnetTheme.colors.text2, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 4.dp))
}

@Composable
private fun Help(res: Int) {
    Text(stringResource(res), style = CarnetTheme.type.dataSmall, color = CarnetTheme.colors.text2, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
}
