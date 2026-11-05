package fr.carnetdefaerun.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.data.notes.Note
import fr.carnetdefaerun.core.data.notes.NoteRepository
import fr.carnetdefaerun.core.data.notes.NoteTarget
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ChoiceTag
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.ui.labelRes
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Nom d'une fiche rattachée, tel que le verrou le laisse voir. */
sealed interface EntryLabel {
    data class Named(val name: String, val type: fr.carnetdefaerun.core.model.content.EntryType? = null) : EntryLabel
    data object Hidden : EntryLabel
    data object Missing : EntryLabel
}

/** Nom affichable de chaque fiche rattachée : passe par la porte de contenu, donc par le verrou et les noms corrigés. */
@OptIn(ExperimentalCoroutinesApi::class)
fun ContentGateway.entryLabels(ids: Flow<Set<ContentId>>): Flow<Map<ContentId, EntryLabel>> = ids.flatMapLatest { set ->
    if (set.isEmpty()) flowOf(emptyMap())
    else combine(set.map { id -> entry(id).map { gated -> id to labelOf(gated) } }) { pairs -> pairs.toMap() }
}

internal fun labelOf(gated: Gated<fr.carnetdefaerun.core.data.content.EntryView>?): EntryLabel = when (gated) {
    is Gated.Visible -> EntryLabel.Named(gated.value.name.fr, gated.value.type)
    is Gated.Hidden -> EntryLabel.Hidden
    null -> EntryLabel.Missing
}

data class NotesUiState(val notes: List<Note>, val labels: Map<ContentId, EntryLabel>)

private val FilterSaver = androidx.compose.runtime.saveable.Saver<NoteFilter, String>(
    save = { f -> when (f) { NoteFilter.All -> "all"; NoteFilter.Free -> "free"; NoteFilter.OnEntries -> "entries"; is NoteFilter.OnAct -> "act:${f.act.name}" } },
    restore = { v ->
        when {
            v == "free" -> NoteFilter.Free
            v == "entries" -> NoteFilter.OnEntries
            v.startsWith("act:") -> fr.carnetdefaerun.core.model.spoiler.Act.entries.firstOrNull { it.name == v.removePrefix("act:") }?.let(NoteFilter::OnAct) ?: NoteFilter.All
            else -> NoteFilter.All
        }
    },
)

/** Filtre de la liste : toutes, sans rattachement, un acte, ou les notes rattachées à une fiche. */
sealed interface NoteFilter {
    data object All : NoteFilter
    data object Free : NoteFilter
    data class OnAct(val act: fr.carnetdefaerun.core.model.spoiler.Act) : NoteFilter
    data object OnEntries : NoteFilter

    fun accepts(note: Note): Boolean = when (this) {
        All -> true
        Free -> note.target == null
        is OnAct -> (note.target as? NoteTarget.OnAct)?.act == act
        OnEntries -> note.target is NoteTarget.OnEntry
    }
}

@HiltViewModel
class NotesViewModel @Inject constructor(repository: NoteRepository, gateway: ContentGateway) : ViewModel() {
    private val targets = repository.notes.map { list -> list.mapNotNull { (it.target as? NoteTarget.OnEntry)?.id }.toSet() }
    val state: StateFlow<NotesUiState?> = combine(repository.notes, gateway.entryLabels(targets)) { notes, labels -> NotesUiState(notes, labels) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun NotesRoute(onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<NotesViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotesScreen(state, onOpen = { onNavigate(Destination.NoteEditor(noteId = it)) }, onNew = { onNavigate(Destination.NoteEditor()) }, onBack = onBack)
}

@Composable
fun NotesScreen(state: NotesUiState?, onOpen: (Long) -> Unit, onNew: () -> Unit, onBack: () -> Unit, initialFilter: NoteFilter = NoteFilter.All) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    // Filtre gardé au retour d'une note et à la rotation.
    var filter by rememberSaveable(stateSaver = FilterSaver) { mutableStateOf(initialFilter) }
    CarnetSectionScope(CarnetSection.OUTILS) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.notes_crumb), onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(Modifier.fillMaxHeight().widthIn(max = CarnetTheme.dimens.readingMaxWidth).fillMaxWidth().wrapContentWidth()) {
                    item(key = "title") {
                        Text(stringResource(R.string.notes_title), style = CarnetTheme.type.headline, color = c.text,
                            modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp).semantics { heading() })
                    }
                    item(key = "intro") {
                        Text(stringResource(R.string.notes_intro), style = CarnetTheme.type.reading, color = c.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 8.dp))
                    }
                    item(key = "new") {
                        Column(Modifier.padding(horizontal = gutter, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldButton(stringResource(R.string.notes_new), onNew)
                        }
                    }
                    if (state != null && state.notes.isNotEmpty()) item(key = "filter") {
                        // Filtres présents seulement s'ils trouvent quelque chose : jamais une puce vide.
                        val filters = buildList {
                            add(NoteFilter.All)
                            if (state.notes.any { it.target == null }) add(NoteFilter.Free)
                            fr.carnetdefaerun.core.model.spoiler.Act.entries.forEach { act -> if (state.notes.any { NoteFilter.OnAct(act).accepts(it) }) add(NoteFilter.OnAct(act)) }
                            if (state.notes.any { it.target is NoteTarget.OnEntry }) add(NoteFilter.OnEntries)
                        }
                        if (filters.size > 2) TagRow(Modifier.padding(horizontal = gutter, vertical = 6.dp)) {
                            for (f in filters) {
                                val label = when (f) {
                                    NoteFilter.All -> stringResource(R.string.notes_filter_all)
                                    NoteFilter.Free -> stringResource(R.string.notes_free)
                                    is NoteFilter.OnAct -> stringResource(f.act.labelRes)
                                    NoteFilter.OnEntries -> stringResource(R.string.notes_filter_entries)
                                }
                                ChoiceTag(label, filter == f, { filter = f })
                            }
                        }
                    }
                    if (state != null) {
                        items(state.notes.filter(filter::accepts), key = { it.id }) { note ->
                            ListRow(titleOf(note), { onOpen(note.id) }, subtitle = targetLabel(note.target, state.labels))
                        }
                        if (state.notes.isEmpty()) item(key = "empty") {
                            Text(stringResource(R.string.notes_empty), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(gutter))
                        }
                    }
                    item(key = "end") { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

/** Première ligne de la note, comme titre. */
@Composable
private fun titleOf(note: Note): String = note.text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(80) ?: stringResource(R.string.notes_untitled)

/** Libellé du rattachement ; une fiche masquée n'est jamais nommée. */
@Composable
fun targetLabel(target: NoteTarget?, labels: Map<ContentId, EntryLabel>): String = when (target) {
    null -> stringResource(R.string.notes_free)
    is NoteTarget.OnAct -> stringResource(target.act.labelRes)
    is NoteTarget.OnEntry -> when (val label = labels[target.id]) {
        is EntryLabel.Named -> label.name
        EntryLabel.Hidden -> stringResource(R.string.notes_hidden_entry)
        EntryLabel.Missing, null -> stringResource(R.string.notes_missing_entry)
    }
}
