package fr.carnetdefaerun.feature.codex

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.data.content.EntryList
import fr.carnetdefaerun.core.data.content.EntrySummary
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.SearchPrompt
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.LoreTheme
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.ui.labelRes
import fr.carnetdefaerun.core.ui.slug
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Thèmes du codex, par groupe, dans l'ordre d'affichage (docs/PLAN.md §7). */
internal val CODEX_GROUPS: List<Pair<Int, List<LoreTheme>>> = listOf(
    R.string.codex_group_world to listOf(
        LoreTheme.PANTHEON, LoreTheme.COSMOLOGY, LoreTheme.PEOPLES, LoreTheme.BESTIARY,
        LoreTheme.FACTIONS, LoreTheme.PLACES, LoreTheme.CHARACTERS, LoreTheme.LEGENDARY_ITEMS,
    ),
    R.string.codex_group_history to listOf(LoreTheme.TIMELINE, LoreTheme.SAGA, LoreTheme.GAME_BOOKS, LoreTheme.FURTHER_READING),
)

@get:StringRes
private val LoreTheme.metaRes: Int
    get() = when (this) {
        LoreTheme.PANTHEON -> R.string.codex_meta_pantheon
        LoreTheme.COSMOLOGY -> R.string.codex_meta_cosmology
        LoreTheme.PEOPLES -> R.string.codex_meta_peoples
        LoreTheme.BESTIARY -> R.string.codex_meta_bestiary
        LoreTheme.FACTIONS -> R.string.codex_meta_factions
        LoreTheme.PLACES -> R.string.codex_meta_places
        LoreTheme.CHARACTERS -> R.string.codex_meta_characters
        LoreTheme.LEGENDARY_ITEMS -> R.string.codex_meta_legendary_items
        LoreTheme.TIMELINE -> R.string.codex_meta_timeline
        LoreTheme.SAGA -> R.string.codex_meta_saga
        LoreTheme.GAME_BOOKS -> R.string.codex_meta_game_books
        LoreTheme.FURTHER_READING -> R.string.codex_meta_further_reading
    }

/** Nombre de fiches visibles par thème : seulement ce que le verrou laisse voir. */
data class CodexUiState(val counts: ImmutableMap<LoreTheme, Int> = persistentMapOf())

@HiltViewModel
class CodexViewModel @Inject constructor(gateway: ContentGateway) : ViewModel() {
    val state: StateFlow<CodexUiState?> = gateway.list(EntryType.LORE)
        .map { list -> CodexUiState(LoreTheme.entries.associateWith { t -> list.visible.count { it.subtype == t.name } }.toImmutableMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun CodexRoute(onNavigate: (Destination) -> Unit, viewModel: CodexViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CodexScreen(state ?: CodexUiState(), onNavigate)
}

@Composable
fun CodexScreen(state: CodexUiState, onNavigate: (Destination) -> Unit) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    CarnetSectionScope(CarnetSection.CODEX) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ReadingColumn(Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.codex_crumb), style = CarnetTheme.type.label, color = CarnetTheme.accent, modifier = Modifier.padding(horizontal = gutter))
                Text(stringResource(R.string.codex_title), style = CarnetTheme.type.display, color = c.text, modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp))
                Text(stringResource(R.string.codex_intro), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = gutter))
                Spacer(Modifier.height(16.dp))
                SearchPrompt(stringResource(R.string.codex_search), { onNavigate(Destination.Search()) }, Modifier.padding(horizontal = 18.dp))
                for ((group, themes) in CODEX_GROUPS) {
                    SectionHeader(stringResource(group), Modifier.padding(top = 10.dp))
                    for (theme in themes) {
                        val count = state.counts[theme] ?: 0
                        ListRow(stringResource(theme.labelRes), { onNavigate(Destination.CodexTheme(theme.slug)) }, subtitle = stringResource(theme.metaRes)) {
                            if (count > 0) Text(pluralStringResource(R.plurals.codex_count, count, count), style = CarnetTheme.type.dataSmall, color = c.gold)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Fiches visibles d'un thème, dans l'ordre alphabétique du nom replié (déjà trié par la porte du contenu). */
data class ThemeUiState(val theme: LoreTheme, val entries: ImmutableList<EntrySummary> = persistentListOf(), val someHidden: Boolean = false)

internal fun themeState(theme: LoreTheme, list: EntryList): ThemeUiState {
    val inTheme = list.visible.filter { it.subtype == theme.name }
    return ThemeUiState(theme, inTheme.toImmutableList(), someHidden = list.someHidden)
}

@HiltViewModel(assistedFactory = ThemeViewModel.Factory::class)
class ThemeViewModel @AssistedInject constructor(@Assisted private val theme: LoreTheme, gateway: ContentGateway) : ViewModel() {
    @AssistedFactory interface Factory { fun create(theme: LoreTheme): ThemeViewModel }

    val state: StateFlow<ThemeUiState?> = gateway.list(EntryType.LORE).map { themeState(theme, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ThemeRoute(theme: LoreTheme, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<ThemeViewModel, ThemeViewModel.Factory>(key = theme.name, creationCallback = { it.create(theme) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThemeScreen(state ?: ThemeUiState(theme), { onNavigate(Destination.LoreEntry(it.id.value)) }, onBack)
}

@Composable
fun ThemeScreen(state: ThemeUiState, onOpen: (EntrySummary) -> Unit, onBack: () -> Unit) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    CarnetSectionScope(CarnetSection.CODEX) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.codex_crumb), onBack = onBack)
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "title") {
                    Text(stringResource(state.theme.labelRes), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp))
                    Text(stringResource(state.theme.metaRes), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp))
                }
                items(state.entries, key = { it.id.value }) { entry ->
                    val vo = if (entry.name.en != entry.name.fr) stringResource(R.string.lore_vo, entry.name.en) else null
                    ListRow(entry.name.fr, { onOpen(entry) }, subtitle = vo)
                }
                if (state.entries.isEmpty()) item(key = "empty") {
                    Text(stringResource(R.string.codex_empty_theme), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(gutter))
                }
                if (state.someHidden) item(key = "hidden") {
                    Text(stringResource(R.string.codex_some_hidden), style = CarnetTheme.type.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = gutter, vertical = 16.dp))
                }
                item(key = "end") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
