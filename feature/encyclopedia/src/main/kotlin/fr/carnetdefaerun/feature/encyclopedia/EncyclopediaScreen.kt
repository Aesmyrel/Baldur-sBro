package fr.carnetdefaerun.feature.encyclopedia

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.SearchPrompt
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.ui.pluralRes
import fr.carnetdefaerun.core.ui.slug
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Catégories du grimoire, par groupe, dans l'ordre d'affichage. */
internal val GRIMOIRE_GROUPS: List<Pair<Int, List<EntryType>>> = listOf(
    R.string.encyclopedia_group_magic to listOf(EntryType.SPELL, EntryType.FEATURE, EntryType.CONDITION),
    R.string.encyclopedia_group_gear to listOf(EntryType.ITEM),
    R.string.encyclopedia_group_character to listOf(EntryType.CLASS, EntryType.SUBCLASS, EntryType.RACE, EntryType.SUBRACE, EntryType.BACKGROUND, EntryType.FEAT),
    R.string.encyclopedia_group_world to listOf(EntryType.PLACE),
)

@get:StringRes
private val EntryType.metaRes: Int
    get() = when (this) {
        EntryType.SPELL -> R.string.encyclopedia_meta_spell
        EntryType.FEATURE -> R.string.encyclopedia_meta_feature
        EntryType.CONDITION -> R.string.encyclopedia_meta_condition
        EntryType.ITEM -> R.string.encyclopedia_meta_item
        EntryType.CLASS -> R.string.encyclopedia_meta_class
        EntryType.SUBCLASS -> R.string.encyclopedia_meta_subclass
        EntryType.RACE -> R.string.encyclopedia_meta_race
        EntryType.SUBRACE -> R.string.encyclopedia_meta_subrace
        EntryType.BACKGROUND -> R.string.encyclopedia_meta_background
        EntryType.FEAT -> R.string.encyclopedia_meta_feat
        else -> R.string.encyclopedia_meta_place
    }

/** Nombre de fiches visibles par catégorie : seulement ce que le verrou laisse voir. */
data class EncyclopediaUiState(val counts: ImmutableMap<EntryType, Int> = persistentMapOf())

@HiltViewModel
class EncyclopediaViewModel @Inject constructor(gateway: ContentGateway) : ViewModel() {
    private val types = GRIMOIRE_GROUPS.flatMap { it.second }

    val state: StateFlow<EncyclopediaUiState?> = combine(types.map(gateway::list)) { lists ->
        EncyclopediaUiState(types.zip(lists) { type, list -> type to list.visible.size }.toMap().toImmutableMap())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun EncyclopediaRoute(onNavigate: (Destination) -> Unit, viewModel: EncyclopediaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EncyclopediaScreen(state ?: EncyclopediaUiState(), onNavigate)
}

@Composable
fun EncyclopediaScreen(state: EncyclopediaUiState, onNavigate: (Destination) -> Unit) {
    val c = CarnetTheme.colors
    CarnetSectionScope(CarnetSection.GRIMOIRE) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ReadingColumn(Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.encyclopedia_crumb), style = CarnetTheme.type.label, color = CarnetTheme.accent, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter))
                Text(stringResource(R.string.encyclopedia_title), style = CarnetTheme.type.display, color = c.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp))
                Text(stringResource(R.string.encyclopedia_intro), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter))
                Spacer(Modifier.height(16.dp))
                SearchPrompt(stringResource(R.string.encyclopedia_search), { onNavigate(Destination.Search()) }, Modifier.padding(horizontal = 18.dp))
                for ((group, types) in GRIMOIRE_GROUPS) {
                    SectionHeader(stringResource(group), Modifier.padding(top = 10.dp))
                    Column {
                        for (type in types) {
                            val count = state.counts[type]
                            ListRow(
                                title = stringResource(type.pluralRes),
                                subtitle = stringResource(type.metaRes),
                                onClick = { onNavigate(Destination.EncyclopediaCategory(type.slug)) },
                            ) {
                                if (count != null) Text(pluralStringResource(R.plurals.encyclopedia_count, count, count), style = CarnetTheme.type.dataSmall, color = c.gold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

