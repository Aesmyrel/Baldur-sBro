package fr.carnetdefaerun.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.carnetdefaerun.core.designsystem.component.ActSelector
import fr.carnetdefaerun.core.designsystem.component.CarnetSlider
import fr.carnetdefaerun.core.designsystem.component.CarnetSwitchRow
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.OrnateCard
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SegmentedChoice
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.data.backup.BackupRepository
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.settings.AppTheme
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Tolerance
import fr.carnetdefaerun.core.ui.actChoices
import fr.carnetdefaerun.core.ui.labelRes
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Actions de la section Données : choisir où écrire la sauvegarde, choisir le fichier à relire. */
class DataActions(
    val onExport: () -> Unit = {},
    val onImport: () -> Unit = {},
    val message: DataMessage? = null,
    val skipped: Int = 0,
    /** Confirmation d'import déjà ouverte (captures). */
    val confirming: Boolean = false,
)

@Composable
fun SettingsRoute(onNavigate: (Destination) -> Unit, onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val skipped by viewModel.skipped.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Sélecteur de documents du système : aucune permission, le joueur choisit l'emplacement.
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CARNET_MIME)) { uri ->
        // Mode « wt » : un fichier existant est tronqué, jamais réécrit en partie.
        if (uri != null) viewModel.export { bytes -> context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("flux") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import { readBounded(context, uri) }
    }
    val fileName = stringResource(R.string.settings_data_file_name, LocalDate.now().toString())
    // Rien n'est affiché avant la lecture des réglages : jamais une image avec de fausses valeurs.
    SettingsScreen(
        prefs ?: return, viewModel::update, onNavigate, onBack,
        DataActions(onExport = { exporter.launch(fileName) }, onImport = { importer.launch(arrayOf("*/*")) }, message = message, skipped = skipped),
    )
}

private const val CARNET_MIME = "application/octet-stream"

/** Lit le fichier choisi sans dépasser la taille admise (un fichier plus gros est refusé ensuite). */
private suspend fun readBounded(context: android.content.Context, uri: android.net.Uri): ByteArray? = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0 || out.size() > BackupRepository.MAX_FILE_BYTES) break
            out.write(buffer, 0, n)
        }
        out.toByteArray()
    }
}

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    onUpdate: ((UserPreferences) -> UserPreferences) -> Unit,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    data: DataActions = DataActions(),
) {
    val c = CarnetTheme.colors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        CarnetTopBar(stringResource(R.string.settings_crumb), onBack = onBack)
        ReadingColumn {
        Text(stringResource(R.string.settings_title), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = 24.dp))

        SectionHeader(stringResource(R.string.settings_progress))
        Label(R.string.settings_act)
        ActSelector(actChoices(), prefs.currentAct.ordinal, { i -> onUpdate { it.copy(currentAct = Act.entries[i]) } })
        Label(R.string.settings_difficulty)
        SegmentedChoice(
            Difficulty.entries.map { stringResource(it.labelRes) },
            Difficulty.entries.indexOf(prefs.difficulty),
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
        val scales = UserPreferences.READING_SCALES
        val scaleLabel = stringResource(R.string.settings_reading_size_value, (prefs.readingScale * 100).roundToInt())
        Label(R.string.settings_reading_size, scaleLabel)
        CarnetSlider(
            value = scales.indexOf(prefs.readingScale).coerceAtLeast(0).toFloat(),
            onValueChange = { v -> onUpdate { it.copy(readingScale = scales[v.toInt().coerceIn(scales.indices)]) } },
            range = 0f..(scales.size - 1).toFloat(),
            steps = scales.size - 2,
            valueLabel = scaleLabel,
        )
        Text(stringResource(R.string.settings_reading_sample), style = CarnetTheme.type.reading, color = c.text, modifier = Modifier.padding(horizontal = 24.dp))
        CarnetSwitchRow(stringResource(R.string.settings_reduce_motion), stringResource(R.string.settings_reduce_motion_help), prefs.reduceMotion, { v -> onUpdate { it.copy(reduceMotion = v) } })

        SectionHeader(stringResource(R.string.settings_session))
        CarnetSwitchRow(stringResource(R.string.settings_keep_screen_on), stringResource(R.string.settings_keep_screen_on_help), prefs.sessionActive, { v -> onUpdate { it.copy(sessionActive = v) } })

        SectionHeader(stringResource(R.string.settings_data))
        DataSection(data)

        SectionHeader(stringResource(R.string.settings_about))
        Help(R.string.credits_notice)
        OrnateCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp), onClick = { onNavigate(Destination.Credits) }) {
            Text(stringResource(R.string.settings_credits), style = CarnetTheme.type.titleSmall, color = c.text)
            Text(stringResource(R.string.settings_credits_meta), style = CarnetTheme.type.dataSmall, color = c.text2)
        }
        Spacer(Modifier.height(32.dp))
        }
    }
}

/** Données : export et import d'une sauvegarde `.carnet`, avec confirmation avant de tout remplacer. */
@Composable
private fun DataSection(data: DataActions) {
    val c = CarnetTheme.colors
    var confirming by rememberSaveable { mutableStateOf(data.confirming) }
    Help(R.string.settings_data_help)
    Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuietButton(stringResource(R.string.settings_data_export), data.onExport)
        QuietButton(stringResource(R.string.settings_data_import), { confirming = true })
    }
    if (confirming) {
        Text(stringResource(R.string.settings_data_import_confirm), style = CarnetTheme.type.bodySmall, color = c.text, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton(stringResource(R.string.settings_data_import_choose), { confirming = false; data.onImport() })
            QuietButton(stringResource(R.string.settings_data_cancel), { confirming = false })
        }
    }
    data.message?.let { m ->
        val text = when (m) {
            DataMessage.EXPORTED -> stringResource(R.string.settings_data_exported)
            DataMessage.EXPORT_FAILED -> stringResource(R.string.settings_data_export_failed)
            DataMessage.IMPORTED -> if (data.skipped > 0) pluralStringResource(R.plurals.settings_data_imported_skipped, data.skipped, data.skipped)
            else stringResource(R.string.settings_data_imported)
            DataMessage.IMPORTED_PARTIALLY -> stringResource(R.string.settings_data_imported_partially)
            DataMessage.IMPORT_NOT_CARNET -> stringResource(R.string.settings_data_not_carnet)
            DataMessage.IMPORT_TOO_NEW -> stringResource(R.string.settings_data_too_new)
            DataMessage.IMPORT_TOO_LARGE -> stringResource(R.string.settings_data_too_large)
            DataMessage.IMPORT_FAILED -> stringResource(R.string.settings_data_import_failed)
        }
        val ok = m == DataMessage.EXPORTED || m == DataMessage.IMPORTED
        Text(text, style = CarnetTheme.type.bodySmall, color = if (ok) c.text2 else c.vermilion,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp).semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
private fun ToleranceChoice(value: Tolerance, onChange: (Tolerance) -> Unit) {
    SegmentedChoice(Tolerance.entries.map { stringResource(it.labelRes) }, value.ordinal, { onChange(Tolerance.entries[it]) }, Modifier.padding(horizontal = 18.dp))
}

/** Titre d'un réglage, avec sa valeur courante à droite (déjà lue par TalkBack sur la commande elle-même). */
@Composable
private fun Label(res: Int, value: String? = null) {
    // Même hiérarchie que les titres d'interrupteur : un réglage, un titre en Garamond.
    Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(res), style = CarnetTheme.type.bodySmall, color = CarnetTheme.colors.text, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = CarnetTheme.type.dataStrong, color = CarnetTheme.accent, softWrap = false, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun Help(res: Int) {
    Text(stringResource(res), style = CarnetTheme.type.dataSmall, color = CarnetTheme.colors.text2, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
}
