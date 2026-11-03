package fr.carnetdefaerun.feature.encyclopedia

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import fr.carnetdefaerun.core.designsystem.component.ChoiceTag
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.SearchField
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatureKind
import fr.carnetdefaerun.core.model.content.ItemCategory
import fr.carnetdefaerun.core.model.content.SpellSchool
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.ui.destinationOf
import fr.carnetdefaerun.core.ui.labelRes
import fr.carnetdefaerun.core.ui.pluralRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Groupe de la liste : sous-classes par classe, sous-races par race ; un seul groupe sans titre ailleurs. */
data class EntryGroup(val title: String?, val entries: ImmutableList<EntrySummary>)

data class CategoryUiState(
    val type: EntryType,
    val filter: String = "",
    /** Sous-types présents parmi les fiches visibles (école, catégorie d'objet, nature, acte). */
    val subtypes: ImmutableList<String> = persistentListOf(),
    val selected: String? = null,
    val groups: ImmutableList<EntryGroup> = persistentListOf(),
    val someHidden: Boolean = false,
)

@HiltViewModel(assistedFactory = CategoryViewModel.Factory::class)
class CategoryViewModel @AssistedInject constructor(@Assisted private val type: EntryType, gateway: ContentGateway) : ViewModel() {
    @AssistedFactory interface Factory { fun create(type: EntryType): CategoryViewModel }

    private val filter = MutableStateFlow("")
    private val selected = MutableStateFlow<String?>(null)

    /** Les parents (classes, races) nomment les groupes ; un parent masqué laisse son groupe sans titre. */
    private val parents = when (type) {
        EntryType.SUBCLASS -> gateway.list(EntryType.CLASS)
        EntryType.SUBRACE -> gateway.list(EntryType.RACE)
        else -> flowOf(null)
    }

    val state: StateFlow<CategoryUiState?> = combine(gateway.list(type), parents, filter, selected) { list, parentList, query, subtype ->
        categoryState(type, list, parentList, query, subtype)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setFilter(query: String) { filter.value = query.take(80) }

    fun select(subtype: String?) { selected.value = subtype }

}

/**
 * État de la liste, en fonction pure : filtre par nom (français ou anglais, accents ignorés), par sous-type, et
 * groupes par parent pour les sous-classes et sous-races ([parents] : la liste visible des classes ou des races).
 */
internal fun categoryState(type: EntryType, list: EntryList, parents: EntryList?, query: String = "", subtype: String? = null): CategoryUiState {
    val folded = Folding.fold(query)
    val subtypes = list.visible.mapNotNull { it.subtype }.distinct().sortedBy { subtypeOrder(type, it) }
    val shown = list.visible.filter { e ->
        (subtype == null || e.subtype == subtype) &&
            (folded.isEmpty() || Folding.fold(e.name.fr).contains(folded) || Folding.fold(e.name.en).contains(folded))
    }
    val groups = if (parents == null) listOf(EntryGroup(null, shown.toImmutableList()))
    else {
        val names = parents.visible.associate { it.id.value to it.name.fr }
        shown.groupBy { it.subtype }.map { (parent, entries) -> EntryGroup(parent?.let(names::get), entries.toImmutableList()) }
            .sortedWith(compareBy<EntryGroup> { it.title == null }.thenBy { it.title?.let(Folding::fold) })
    }
    return CategoryUiState(
        type = type,
        filter = query,
        subtypes = (if (parents == null) subtypes else emptyList()).toImmutableList(),
        selected = subtype,
        groups = groups.filter { it.entries.isNotEmpty() }.toImmutableList(),
        someHidden = list.someHidden,
    )
}

private fun subtypeOrder(type: EntryType, subtype: String): Int = when (type) {
    EntryType.SPELL -> SpellSchool.entries.indexOfFirst { it.name == subtype }
    EntryType.ITEM -> ItemCategory.entries.indexOfFirst { it.name == subtype }
    EntryType.FEATURE -> FeatureKind.entries.indexOfFirst { it.name == subtype }
    EntryType.PLACE -> Act.entries.indexOfFirst { it.name == subtype }
    else -> 0
}

@Composable
fun CategoryRoute(type: EntryType, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<CategoryViewModel, CategoryViewModel.Factory>(key = type.name, creationCallback = { it.create(type) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoryScreen(state ?: CategoryUiState(type), viewModel::setFilter, viewModel::select, { id, t -> onNavigate(destinationOf(id, t)) }, onBack)
}

@Composable
fun CategoryScreen(
    state: CategoryUiState,
    onFilter: (String) -> Unit,
    onSelect: (String?) -> Unit,
    onOpen: (ContentId, EntryType) -> Unit,
    onBack: () -> Unit,
) {
    val c = CarnetTheme.colors
    CarnetSectionScope(CarnetSection.GRIMOIRE) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.encyclopedia_crumb), onBack = onBack)
            LazyColumn(Modifier.fillMaxSize()) {
                item(key = "title") {
                    Text(stringResource(state.type.pluralRes), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp))
                }
                item(key = "filter") {
                    SearchField(state.filter, onFilter, stringResource(R.string.category_filter), Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                }
                if (state.subtypes.size > 1) item(key = "subtypes") {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceTag(stringResource(R.string.category_all), state.selected == null, { onSelect(null) })
                        for (subtype in state.subtypes) {
                            ChoiceTag(subtypeLabel(state.type, subtype), state.selected == subtype, { on -> onSelect(if (on) subtype else null) })
                        }
                    }
                }
                for ((index, group) in state.groups.withIndex()) {
                    if (state.groups.size > 1 || group.title != null) item(key = "group-$index") {
                        SectionHeader(group.title ?: stringResource(R.string.category_unnamed_group), Modifier.padding(top = 6.dp))
                    }
                    items(group.entries, key = { it.id.value }) { entry ->
                        ListRow(entry.name.fr, { onOpen(entry.id, entry.type) }, subtitle = subtitleOf(state.type, entry))
                    }
                }
                if (state.groups.isEmpty()) item(key = "empty") {
                    Text(stringResource(R.string.category_empty), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(CarnetTheme.dimens.gutter))
                }
                if (state.someHidden) item(key = "hidden") {
                    Text(stringResource(R.string.category_some_hidden), style = CarnetTheme.type.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 16.dp))
                }
                item(key = "end") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/** Le nom anglais en secondaire, et le sous-type quand la liste mélange plusieurs sous-types. */
@Composable
private fun subtitleOf(type: EntryType, entry: EntrySummary): String? {
    val vo = if (entry.name.en != entry.name.fr) stringResource(R.string.entry_vo, entry.name.en) else null
    val subtype = entry.subtype?.takeIf { type == EntryType.SPELL || type == EntryType.ITEM }?.let { subtypeLabel(type, it) }
    return joinedSubtitle(listOfNotNull(entry.name.precision, subtype, vo))
}

@Composable
internal fun subtypeLabel(type: EntryType, subtype: String): String = when (type) {
    EntryType.SPELL -> SpellSchool.entries.firstOrNull { it.name == subtype }?.let { stringResource(it.labelRes) }
    EntryType.ITEM -> ItemCategory.entries.firstOrNull { it.name == subtype }?.let { stringResource(it.labelRes) }
    EntryType.FEATURE -> FeatureKind.entries.firstOrNull { it.name == subtype }?.let { stringResource(it.labelRes) }
    EntryType.PLACE -> Act.entries.firstOrNull { it.name == subtype }?.let { stringResource(it.labelRes) }
    else -> null
} ?: subtype

/** Précisions d'une ligne, séparées par un point médian ; null s'il n'y en a aucune. */
@Composable
internal fun joinedSubtitle(parts: List<String>): String? =
    parts.reduceOrNull { acc, part -> stringResource(R.string.subtitle_join, acc, part) }
