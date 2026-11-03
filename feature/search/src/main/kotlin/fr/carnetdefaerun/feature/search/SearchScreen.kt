package fr.carnetdefaerun.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import fr.carnetdefaerun.core.data.search.SearchHit
import fr.carnetdefaerun.core.designsystem.component.CarnetSwitchRow
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.SearchField
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SpoilerVeil
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.MAX_QUERY_LENGTH
import fr.carnetdefaerun.core.model.search.NameKind
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.ui.destinationOf
import fr.carnetdefaerun.core.ui.pluralRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Résultats d'un même type, dans l'ordre de leur meilleur classement. */
data class SearchGroup(val type: EntryType, val hits: ImmutableList<SearchHit>)

data class SearchUiState(
    val query: String = "",
    val includeHidden: Boolean = false,
    val groups: ImmutableList<SearchGroup> = persistentListOf(),
    /** Voiles des résultats masqués : seulement si le joueur a demandé à chercher aussi dans le contenu masqué. */
    val hidden: ImmutableList<RevealKey> = persistentListOf(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = SearchViewModel.Factory::class)
class SearchViewModel @AssistedInject constructor(@Assisted initial: String, private val gateway: ContentGateway) : ViewModel() {
    @AssistedFactory interface Factory { fun create(initial: String): SearchViewModel }

    private val query = MutableStateFlow(initial.take(MAX_QUERY_LENGTH))
    private val includeHidden = MutableStateFlow(false)

    init {
        viewModelScope.launch { gateway.warmUpSearch() }
    }

    val state: StateFlow<SearchUiState> = combine(query, includeHidden) { q, hidden -> q to hidden }
        .flatMapLatest { (q, hidden) ->
            if (q.isBlank()) flowOf(SearchUiState(q, hidden))
            else gateway.search(q, hidden).map { results ->
                SearchUiState(
                    query = q,
                    includeHidden = hidden,
                    groups = results.hits.groupBy { it.entry.type }.map { (type, hits) -> SearchGroup(type, hits.toImmutableList()) }.toImmutableList(),
                    hidden = results.hidden.toImmutableList(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState(initial))

    fun setQuery(text: String) { query.value = text.take(MAX_QUERY_LENGTH) }

    fun setIncludeHidden(include: Boolean) { includeHidden.value = include }

    fun reveal(key: RevealKey) = viewModelScope.launch { gateway.reveal(key) }
}

@Composable
fun SearchRoute(initialQuery: String, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory>(creationCallback = { it.create(initialQuery) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchScreen(state, viewModel::setQuery, viewModel::setIncludeHidden, viewModel::reveal, { id, type -> onNavigate(destinationOf(id, type)) }, onBack)
}

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQuery: (String) -> Unit,
    onIncludeHidden: (Boolean) -> Unit,
    onReveal: (RevealKey) -> Unit,
    onOpen: (ContentId, EntryType) -> Unit,
    onBack: () -> Unit,
    autoFocus: Boolean = true,
) {
    val c = CarnetTheme.colors
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { if (state.query.isEmpty()) focus.requestFocus() }
    Column(Modifier.fillMaxSize()) {
        CarnetTopBar(stringResource(R.string.search_crumb), onBack = onBack)
        SearchField(state.query, onQuery, stringResource(R.string.search_placeholder), Modifier.padding(horizontal = 18.dp, vertical = 6.dp).focusRequester(focus))
        LazyColumn(Modifier.fillMaxSize()) {
            if (state.query.isBlank()) {
                item(key = "hint") {
                    Text(stringResource(R.string.search_hint), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 12.dp))
                }
                return@LazyColumn
            }
            for (group in state.groups) {
                item(key = "type-${group.type.name}") { SectionHeader(stringResource(group.type.pluralRes), Modifier.padding(top = 4.dp)) }
                items(group.hits, key = { it.entry.id.value }) { hit -> HitRow(hit, onOpen) }
            }
            if (state.groups.isEmpty()) item(key = "empty") {
                Text(stringResource(R.string.search_empty), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 12.dp))
            }
            // Ligne fixe, quel que soit le résultat : elle ne dit pas s'il existe un résultat masqué.
            item(key = "include-hidden") {
                CarnetSwitchRow(
                    stringResource(R.string.search_include_hidden), stringResource(R.string.search_include_hidden_meta),
                    state.includeHidden, onIncludeHidden, Modifier.padding(top = 12.dp),
                )
            }
            items(state.hidden, key = { it.token }) { key ->
                SpoilerVeil({ onReveal(key) }, Modifier.padding(horizontal = 18.dp, vertical = 6.dp), minHeight = 72.dp)
            }
            item(key = "end") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HitRow(hit: SearchHit, onOpen: (ContentId, EntryType) -> Unit) {
    val entry = hit.entry
    val subtitle = when {
        hit.excerpt != null -> hit.excerpt
        hit.matchedKind != NameKind.FR && entry.name.en != entry.name.fr -> stringResource(R.string.search_vo, entry.name.en)
        else -> null
    }
    ListRow(entry.name.fr, { onOpen(entry.id, entry.type) }, subtitle = subtitle)
}
