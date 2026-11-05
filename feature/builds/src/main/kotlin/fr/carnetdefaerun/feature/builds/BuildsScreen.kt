package fr.carnetdefaerun.feature.builds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.builds.BuildRepository
import fr.carnetdefaerun.core.data.builds.RulesRepository
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.TextInput
import fr.carnetdefaerun.core.designsystem.illustration.drawClassEmblem
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.rules.CharacterSheet
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Un build de la liste, avec sa fiche calculée pour le résumé. */
data class BuildItem(val id: Long, val build: Build, val sheet: CharacterSheet)

data class BuildsUiState(val items: ImmutableList<BuildItem>, val catalog: Catalog)

@HiltViewModel
class BuildsViewModel @Inject constructor(
    private val repository: BuildRepository,
    rules: RulesRepository,
    gateway: ContentGateway,
) : ViewModel() {
    val state: StateFlow<BuildsUiState?> = combine(repository.builds, gateway.catalog(), flow { emit(rules.data()) }) { builds, catalog, data ->
        BuildsUiState(builds.map { BuildItem(it.id, it.build, CharacterSheetCalculator.compute(it.build, data)) }.toImmutableList(), catalog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun create(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch { onCreated(repository.create(Build(name = name))) }

    /** Importe un code de partage ; false s'il est illisible. */
    fun import(code: String, onCreated: (Long) -> Unit): Boolean {
        val build = BuildCode.decode(code) ?: return false
        viewModelScope.launch { onCreated(repository.create(build)) }
        return true
    }
}

@Composable
fun BuildsRoute(onNavigate: (Destination) -> Unit) {
    val viewModel = hiltViewModel<BuildsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val defaultName = stringResource(R.string.builds_default_name)
    val open = { id: Long -> onNavigate(Destination.BuildEditor(id)) }
    BuildsScreen(
        state = state,
        onOpen = open,
        onCreate = { viewModel.create(defaultName, open) },
        onImport = { code -> viewModel.import(code, open) },
    )
}

@Composable
fun BuildsScreen(state: BuildsUiState?, onOpen: (Long) -> Unit, onCreate: () -> Unit, onImport: (String) -> Boolean) {
    val c = CarnetTheme.colors
    var importing by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    CarnetSectionScope(CarnetSection.BUILDS) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.builds_crumb))
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "title") {
                    Text(stringResource(R.string.builds_title), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp))
                }
                item(key = "intro") {
                    Text(stringResource(R.string.builds_intro), style = CarnetTheme.type.reading, color = c.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp))
                }
                item(key = "actions") {
                    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GoldButton(stringResource(R.string.builds_new), onCreate)
                            if (!importing) QuietButton(stringResource(R.string.builds_import), { importing = true })
                        }
                        if (importing) {
                            TextInput(code, { code = it.take(BUILD_CODE_INPUT_LIMIT); invalid = false }, stringResource(R.string.builds_import_hint))
                            if (invalid) Text(stringResource(R.string.builds_import_invalid), style = CarnetTheme.type.bodySmall, color = c.vermilion)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GoldButton(stringResource(R.string.builds_import_action), {
                                    if (onImport(code)) { importing = false; code = "" } else invalid = true
                                }, enabled = code.isNotBlank())
                                QuietButton(stringResource(R.string.builds_cancel), { importing = false; code = ""; invalid = false })
                            }
                        }
                    }
                }
                if (state != null && state.items.isNotEmpty()) {
                    item(key = "header") { SectionHeader(stringResource(R.string.builds_saved)) }
                    items(state.items, key = { it.id }) { item ->
                        val main = item.sheet.classLevels.maxByOrNull { it.value }?.key
                        ListRow(item.build.name, { onOpen(item.id) }, subtitle = summaryLine(item.build, item.sheet, state.catalog)) {
                            val emblem = main?.let(::emblemOf)
                            if (emblem != null) {
                                val pigment = pigmentOf(main, c)
                                Canvas(Modifier.size(28.dp)) { drawClassEmblem(emblem, pigment, strokeWidth = 1.4f) }
                            }
                        }
                    }
                } else if (state != null) {
                    item(key = "empty") {
                        Text(stringResource(R.string.builds_empty), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(CarnetTheme.dimens.gutter))
                    }
                }
                item(key = "end") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/** Un code de partage tient en moins de 400 caractères ; on laisse de la marge sans accepter un roman. */
private const val BUILD_CODE_INPUT_LIMIT = 4_096

