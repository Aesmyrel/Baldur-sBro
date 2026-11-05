package fr.carnetdefaerun.feature.encyclopedia

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import fr.carnetdefaerun.core.data.content.EntryView
import fr.carnetdefaerun.core.data.content.LinkedEntry
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.BookmarkGlyph
import fr.carnetdefaerun.core.designsystem.component.NoteGlyph
import fr.carnetdefaerun.core.designsystem.component.CanonBadge
import fr.carnetdefaerun.core.designsystem.component.CanonKind
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.IconSlot
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.SearchField
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SpoilerVeil
import fr.carnetdefaerun.core.designsystem.component.Tag
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.component.TextAction
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.Block
import fr.carnetdefaerun.core.model.content.BlockKind
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.NameStatus
import fr.carnetdefaerun.core.model.content.Relation
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.ui.destinationOf
import fr.carnetdefaerun.core.ui.labelRes
import fr.carnetdefaerun.core.ui.pluralRes
import fr.carnetdefaerun.core.ui.singularRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EntryUiState {
    data object Loading : EntryUiState
    data object Missing : EntryUiState

    /** Fiche verrouillée : seulement la poignée de révélation, et l'acte si le joueur l'a demandé. */
    data class Hidden(val key: RevealKey, val act: Act?) : EntryUiState
    data class Visible(val view: EntryView, val favorite: Boolean) : EntryUiState
}

@HiltViewModel(assistedFactory = EntryViewModel.Factory::class)
class EntryViewModel @AssistedInject constructor(@Assisted rawId: String, private val gateway: ContentGateway) : ViewModel() {
    /** L'identifiant arrive en texte : une fabrique assistée n'accepte pas une classe de valeur. */
    @AssistedFactory interface Factory { fun create(rawId: String): EntryViewModel }

    private val id = ContentId(rawId)

    val state: StateFlow<EntryUiState> = combine(gateway.entry(id), gateway.favorites) { gated, favorites ->
        when (gated) {
            null -> EntryUiState.Missing
            is Gated.Hidden -> EntryUiState.Hidden(gated.key, gated.act)
            is Gated.Visible -> EntryUiState.Visible(gated.value, id in favorites)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryUiState.Loading)

    fun reveal(key: RevealKey) = viewModelScope.launch { gateway.reveal(key) }

    fun setFavorite(favorite: Boolean) = viewModelScope.launch { gateway.setFavorite(id, favorite) }

    fun rename(fr: String) = viewModelScope.launch { gateway.correctName(id, fr) }

    fun resetName() = viewModelScope.launch { gateway.resetName(id) }
}

@Composable
fun EntryRoute(id: ContentId, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<EntryViewModel, EntryViewModel.Factory>(key = id.value, creationCallback = { it.create(id.value) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    EntryScreen(
        state,
        EntryActions(
            onReveal = viewModel::reveal,
            onFavorite = viewModel::setFavorite,
            onRename = viewModel::rename,
            onResetName = viewModel::resetName,
            onOpen = { target, type -> onNavigate(destinationOf(target, type)) },
            onBack = onBack,
            onNote = { onNavigate(Destination.NoteEditor(targetId = id.value)) },
        ),
    )
}

/** Ce que la fiche peut demander : révéler, marquer, renommer, ouvrir une fiche liée, revenir. */
class EntryActions(
    val onReveal: (RevealKey) -> Unit = {},
    val onFavorite: (Boolean) -> Unit = {},
    val onRename: (String) -> Unit = {},
    val onResetName: () -> Unit = {},
    val onOpen: (ContentId, EntryType) -> Unit = { _, _ -> },
    val onBack: () -> Unit = {},
    /** Ajouter une note rattachée à cette fiche ; null : pas d'action. */
    val onNote: (() -> Unit)? = null,
)

@Composable
fun EntryScreen(state: EntryUiState, actions: EntryActions) {
    CarnetSectionScope(CarnetSection.GRIMOIRE) {
        Column(Modifier.fillMaxSize()) {
            val crumb = (state as? EntryUiState.Visible)?.let {
                stringResource(R.string.subtitle_join, stringResource(R.string.encyclopedia_crumb), stringResource(it.view.type.pluralRes))
            } ?: stringResource(R.string.encyclopedia_crumb)
            CarnetTopBar(crumb, onBack = actions.onBack) {
                if (state is EntryUiState.Visible) {
                    val label = stringResource(if (state.favorite) R.string.entry_favorite_remove else R.string.entry_favorite_add)
                    actions.onNote?.let { note -> IconSlot(note, stringResource(R.string.entry_note_add)) { NoteGlyph(CarnetTheme.colors.gold) } }
                    IconSlot({ actions.onFavorite(!state.favorite) }, label) { BookmarkGlyph(state.favorite, CarnetTheme.colors.gold) }
                }
            }
            when (state) {
                EntryUiState.Loading -> Unit
                EntryUiState.Missing -> Notice(stringResource(R.string.entry_missing))
                is EntryUiState.Hidden -> HiddenEntry(state, actions)
                is EntryUiState.Visible -> VisibleEntry(state.view, actions)
            }
        }
    }
}

@Composable
private fun HiddenEntry(state: EntryUiState.Hidden, actions: EntryActions) {
    val c = CarnetTheme.colors
    ReadingColumn(Modifier.padding(top = 8.dp)) {
        Text(stringResource(R.string.entry_hidden_title), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter))
        Text(stringResource(R.string.entry_hidden_body), style = CarnetTheme.type.reading, color = c.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 12.dp))
        SpoilerVeil({ actions.onReveal(state.key) }, Modifier.padding(horizontal = 18.dp), actLabel = state.act?.let { stringResource(it.labelRes) }, minHeight = 140.dp)
    }
}

@Composable
private fun VisibleEntry(view: EntryView, actions: EntryActions) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ReadingColumn {
            Text(
                view.name.fr, style = CarnetTheme.type.display, color = c.text,
                modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp).semantics { heading() },
            )
            val vo = if (view.name.en != view.name.fr) stringResource(R.string.entry_vo, view.name.en) else null
            joinedSubtitle(listOfNotNull(view.name.precision, vo))?.let {
                Text(it, style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = gutter))
            }
            TagRow(Modifier.padding(horizontal = gutter, vertical = 12.dp)) {
                Tag(stringResource(view.type.singularRes), color = CarnetTheme.accent)
                when (view.name.status) {
                    NameStatus.VF_DND -> CanonBadge(stringResource(R.string.entry_status_vf_dnd), CanonKind.DND)
                    NameStatus.UNOFFICIAL -> CanonBadge(stringResource(R.string.entry_status_unofficial), CanonKind.NEUTRE)
                    NameStatus.CORRECTED -> CanonBadge(stringResource(R.string.entry_status_corrected), CanonKind.INTERPRETATION)
                    NameStatus.VF_BG3 -> Unit
                }
            }
            RenameRow(view, actions)

            val linkNames = view.links.associate { it.id to it.name.fr }
            EntryFacts(view, linkNames, actions.onOpen)

            val structured = (view.facts as? ItemFacts)?.let { itemEffectLines(it.effects, linkNames) }.orEmpty()
            BlockSection(stringResource(R.string.entry_effects), view.blocks, BlockKind.EFFECT, actions, structured)
            if (view.someEffectsHidden) {
                Text(stringResource(R.string.entry_effects_hidden), style = CarnetTheme.type.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp))
            }
            BlockSection(stringResource(R.string.entry_notes), view.blocks, BlockKind.NOTE, actions)
            ItemSources(view, linkNames, actions.onReveal)

            if (view.links.any { link -> structured.none { it.target == link.id } }) {
                SectionHeader(stringResource(R.string.entry_links), Modifier.padding(top = 10.dp))
                // Ce que les effets citent déjà n'est pas répété dans les liens.
                val cited = structured.mapNotNull { it.target }.toSet()
                for ((relation, links) in view.links.filter { it.id !in cited }.groupBy { it.relation }) {
                    Text(stringResource(relation.labelRes), style = CarnetTheme.type.label, color = c.text2, modifier = Modifier.padding(start = gutter, end = gutter, top = 10.dp))
                    for (link in links) LinkRow(link, actions)
                }
            }

            if (view.unknown.isNotEmpty()) {
                Text(stringResource(R.string.entry_incomplete), style = CarnetTheme.type.bodySmall, color = c.muted, modifier = Modifier.padding(horizontal = gutter, vertical = 12.dp))
            }
            Attribution(view)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RenameRow(view: EntryView, actions: EntryActions) {
    var editing by rememberSaveable(view.id.value) { mutableStateOf(false) }
    var draft by rememberSaveable(view.id.value) { mutableStateOf(view.name.fr) }
    if (!editing) {
        // Le lien d'action a 4 dp de marge propre : la rangée s'aligne ainsi sur la colonne de texte.
        Row(Modifier.padding(horizontal = CarnetTheme.dimens.gutter - 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextAction(stringResource(R.string.entry_rename), { draft = view.name.fr; editing = true })
            if (view.name.status == NameStatus.CORRECTED) TextAction(stringResource(R.string.entry_rename_reset), actions.onResetName)
        }
    } else {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(draft, { draft = it.take(120) }, stringResource(R.string.entry_rename_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton(stringResource(R.string.entry_rename_save), { actions.onRename(draft); editing = false }, enabled = draft.isNotBlank())
                QuietButton(stringResource(R.string.entry_rename_cancel), { editing = false })
            }
        }
    }
}

/** Blocs d'un genre : texte visible, ou voile à la place d'un bloc masqué (sans rien dire de son contenu). */
@Composable
private fun BlockSection(title: String, blocks: List<Gated<Block>>, kind: BlockKind, actions: EntryActions, structured: List<EffectLine> = emptyList()) {
    val visible = blocks.filterIsInstance<Gated.Visible<Block>>().filter { it.value.kind == kind }
    // Un voile n'appartient à aucun genre visible : il est rangé sous les effets, la section la plus générale.
    val veils = if (kind == BlockKind.EFFECT) blocks.filterIsInstance<Gated.Hidden>() else emptyList()
    if (visible.isEmpty() && veils.isEmpty() && structured.isEmpty()) return
    SectionHeader(title, Modifier.padding(top = 10.dp))
    for (line in structured) {
        val target = line.target
        if (target != null) ListRow(line.text, { actions.onOpen(target, EntryType.entries.first { it.name.lowercase() == target.kind }) })
        else Text(line.text, style = CarnetTheme.type.reading, color = CarnetTheme.colors.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp))
    }
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in visible) {
            Text(block.value.text, style = CarnetTheme.type.reading, color = CarnetTheme.colors.text)
        }
    }
    for (veil in veils) {
        SpoilerVeil({ actions.onReveal(veil.key) }, Modifier.padding(horizontal = 18.dp, vertical = 6.dp), actLabel = veil.act?.let { stringResource(it.labelRes) })
    }
}

@Composable
private fun LinkRow(link: LinkedEntry, actions: EntryActions) {
    ListRow(link.name.fr, { actions.onOpen(link.id, link.type) }, subtitle = joinedSubtitle(listOfNotNull(stringResource(link.type.singularRes), link.name.precision)))
}

@Composable
private fun Attribution(view: EntryView) {
    val c = CarnetTheme.colors
    if (view.attribution.isEmpty()) return
    SectionHeader(stringResource(R.string.entry_source), Modifier.padding(top = 10.dp))
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (a in view.attribution) {
            Text(stringResource(R.string.entry_attribution, a.source, a.title, a.revision.toInt()), style = CarnetTheme.type.bodySmall, color = c.text2)
        }
        Text(stringResource(R.string.entry_licence, view.licence), style = CarnetTheme.type.bodySmall, color = c.muted)
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = CarnetTheme.type.reading, color = CarnetTheme.colors.text2, modifier = Modifier.padding(CarnetTheme.dimens.gutter))
}

internal val Relation.labelRes: Int
    get() = when (this) {
        Relation.GRANTS -> R.string.relation_grants
        Relation.GRANTED_BY -> R.string.relation_granted_by
        Relation.APPLIES -> R.string.relation_applies
        Relation.LEARNED_BY -> R.string.relation_learned_by
        Relation.LOCATED_IN -> R.string.relation_located_in
        Relation.VARIANT_OF -> R.string.relation_variant_of
        Relation.SEE_ALSO -> R.string.relation_see_also
        Relation.SUBCLASS_OF -> R.string.relation_subclass_of
        else -> R.string.relation_other
    }
