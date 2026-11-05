package fr.carnetdefaerun.feature.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import fr.carnetdefaerun.core.designsystem.component.BookmarkGlyph
import fr.carnetdefaerun.core.designsystem.component.NoteGlyph
import fr.carnetdefaerun.core.designsystem.component.CanonBadge
import fr.carnetdefaerun.core.designsystem.component.CanonKind
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.DropCapParagraph
import fr.carnetdefaerun.core.designsystem.component.IconSlot
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.OrnateCard
import fr.carnetdefaerun.core.designsystem.component.ReadingColumn
import fr.carnetdefaerun.core.designsystem.component.ReadingProgress
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SpoilerVeil
import fr.carnetdefaerun.core.designsystem.component.Tag
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.CanonNature
import fr.carnetdefaerun.core.model.content.Citation
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.LoreParagraph
import fr.carnetdefaerun.core.model.content.LoreSection
import fr.carnetdefaerun.core.model.content.LoreSectionType
import fr.carnetdefaerun.core.model.content.Reference
import fr.carnetdefaerun.core.model.content.ReferenceKind
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.ui.destinationOf
import fr.carnetdefaerun.core.ui.labelRes
import fr.carnetdefaerun.core.ui.singularRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LoreUiState {
    data object Loading : LoreUiState
    data object Missing : LoreUiState

    /** Fiche verrouillée : seulement la poignée de révélation, et l'acte si le joueur l'a demandé. */
    data class Hidden(val key: RevealKey, val act: Act?) : LoreUiState
    data class Visible(val view: EntryView, val favorite: Boolean) : LoreUiState
}

@HiltViewModel(assistedFactory = LoreEntryViewModel.Factory::class)
class LoreEntryViewModel @AssistedInject constructor(@Assisted rawId: String, private val gateway: ContentGateway) : ViewModel() {
    /** L'identifiant arrive en texte : une fabrique assistée n'accepte pas une classe de valeur. */
    @AssistedFactory interface Factory { fun create(rawId: String): LoreEntryViewModel }

    private val id = ContentId(rawId)

    val state: StateFlow<LoreUiState> = combine(gateway.entry(id), gateway.favorites) { gated, favorites ->
        when (gated) {
            null -> LoreUiState.Missing
            is Gated.Hidden -> LoreUiState.Hidden(gated.key, gated.act)
            is Gated.Visible -> LoreUiState.Visible(gated.value, id in favorites)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoreUiState.Loading)

    fun reveal(key: RevealKey) = viewModelScope.launch { gateway.reveal(key) }

    fun setFavorite(favorite: Boolean) = viewModelScope.launch { gateway.setFavorite(id, favorite) }
}

@Composable
fun LoreEntryRoute(id: ContentId, anchor: String?, onNavigate: (Destination) -> Unit, onBack: () -> Unit) {
    val viewModel = hiltViewModel<LoreEntryViewModel, LoreEntryViewModel.Factory>(key = id.value, creationCallback = { it.create(id.value) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoreEntryScreen(
        state, anchor, viewModel::reveal, viewModel::setFavorite, { target, type -> onNavigate(destinationOf(target, type)) }, onBack,
        onNote = { onNavigate(Destination.NoteEditor(targetId = id.value)) },
    )
}

@Composable
fun LoreEntryScreen(
    state: LoreUiState,
    anchor: String?,
    onReveal: (RevealKey) -> Unit,
    onFavorite: (Boolean) -> Unit,
    onOpen: (ContentId, EntryType) -> Unit,
    onBack: () -> Unit,
    onNote: (() -> Unit)? = null,
) {
    CarnetSectionScope(CarnetSection.CODEX) {
        Column(Modifier.fillMaxSize()) {
            val theme = ((state as? LoreUiState.Visible)?.view?.facts as? LoreFacts)?.theme
            val crumb = theme?.let { stringResource(R.string.lore_crumb, stringResource(it.labelRes)) } ?: stringResource(R.string.codex_crumb)
            CarnetTopBar(crumb, onBack = onBack) {
                if (state is LoreUiState.Visible) {
                    val label = stringResource(if (state.favorite) R.string.lore_favorite_remove else R.string.lore_favorite_add)
                    onNote?.let { note -> IconSlot(note, stringResource(R.string.lore_note_add)) { NoteGlyph(CarnetTheme.colors.gold) } }
                    IconSlot({ onFavorite(!state.favorite) }, label) { BookmarkGlyph(state.favorite, CarnetTheme.colors.gold) }
                }
            }
            when (state) {
                LoreUiState.Loading -> Unit
                LoreUiState.Missing -> Text(stringResource(R.string.lore_missing), style = CarnetTheme.type.reading, color = CarnetTheme.colors.text2, modifier = Modifier.padding(CarnetTheme.dimens.gutter))
                is LoreUiState.Hidden -> ReadingColumn(Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.lore_hidden_title), style = CarnetTheme.type.headline, color = CarnetTheme.colors.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter))
                    Text(stringResource(R.string.lore_hidden_body), style = CarnetTheme.type.reading, color = CarnetTheme.colors.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 12.dp))
                    SpoilerVeil({ onReveal(state.key) }, Modifier.padding(horizontal = 18.dp), actLabel = state.act?.let { stringResource(it.labelRes) }, minHeight = 140.dp)
                }
                is LoreUiState.Visible -> LoreBody(state.view, anchor, onReveal, onOpen)
            }
        }
    }
}

@Composable
private fun LoreBody(view: EntryView, anchor: String?, onReveal: (RevealKey) -> Unit, onOpen: (ContentId, EntryType) -> Unit) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    val facts = view.facts as? LoreFacts ?: return
    val scroll = rememberScrollState()
    // Numérotation des références dans l'ordre de leur premier appel : celle de la liste en pied de fiche.
    val numbers = remember(view) { citationNumbers(view) }
    val sectionTops = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(anchor, sectionTops[anchor]) { anchor?.let { sectionTops[it] }?.let { scroll.animateScrollTo(it) } }

    ReadingProgress({ if (scroll.maxValue > 0) scroll.value / scroll.maxValue.toFloat() else 0f })
    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
        ReadingColumn {
            Text(view.name.fr, style = CarnetTheme.type.display, color = c.text, modifier = Modifier.padding(start = gutter, end = gutter, top = 12.dp).semantics { heading() })
            val vo = if (view.name.en != view.name.fr) stringResource(R.string.lore_vo, view.name.en) else null
            Text(vo?.let { stringResource(R.string.lore_subtitle, it, facts.summary) } ?: facts.summary, style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp))
            TagRow(Modifier.padding(horizontal = gutter, vertical = 12.dp)) {
                natureBadges(view).forEach { (text, kind) -> CanonBadge(text, kind) }
                if (facts.readingMinutes > 0) Tag(pluralStringResource(R.plurals.lore_reading, facts.readingMinutes, facts.readingMinutes))
            }

            var first = true
            for (gated in view.sections) {
                when (gated) {
                    is Gated.Hidden -> SpoilerVeil({ onReveal(gated.key) }, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), actLabel = gated.act?.let { stringResource(it.labelRes) })
                    is Gated.Visible -> {
                        val section = gated.value
                        Column(Modifier.onGloballyPositioned { sectionTops[section.id] = it.positionInParent().y.toInt() }) {
                            Section(section, numbers, dropCap = first)
                        }
                        first = false
                    }
                }
            }

            if (view.references.isNotEmpty()) {
                SectionHeader(stringResource(R.string.lore_references), Modifier.padding(top = 14.dp))
                Column(Modifier.padding(horizontal = gutter, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val byId = view.references.associateBy { it.id }
                    val citations = remember(view) { citationsByReference(view) }
                    numbers.entries.sortedBy { it.value }.forEach { (id, n) ->
                        byId[id]?.let { ref -> Text("$n. ${referenceLine(ref, byId, citations[id].orEmpty())}", style = CarnetTheme.type.bodySmall, color = c.text2) }
                    }
                    Text(stringResource(R.string.lore_written), style = CarnetTheme.type.bodySmall, color = c.muted, modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (view.links.isNotEmpty()) {
                SectionHeader(stringResource(R.string.lore_relations), Modifier.padding(top = 14.dp))
                for (link in view.links) ListRow(link.name.fr, { onOpen(link.id, link.type) }, subtitle = stringResource(link.type.singularRes))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Une section : son titre, sa nature, ses paragraphes ; les variantes d'un même groupe dans un même cadre. */
@Composable
private fun Section(section: LoreSection, numbers: Map<String, Int>, dropCap: Boolean) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    SectionHeader(section.title, Modifier.padding(top = 10.dp))
    sectionNote(section)?.let { Text(it, style = CarnetTheme.type.labelSmall, color = c.text2, modifier = Modifier.padding(start = gutter, end = gutter, bottom = 4.dp)) }
    var opened = !dropCap
    val groups = section.paragraphs.groupConsecutiveBy { it.variantGroup }
    for ((group, paragraphs) in groups) {
        if (group != null && paragraphs.size > 1) {
            OrnateCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Text(stringResource(R.string.lore_variants), style = CarnetTheme.type.label, color = CarnetTheme.accent)
                Text(stringResource(R.string.lore_variants_hint), style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
                for ((i, p) in paragraphs.withIndex()) {
                    p.edition?.let { Text(stringResource(it.labelRes), style = CarnetTheme.type.labelSmall, color = c.gold, modifier = Modifier.padding(top = if (i == 0) 6.dp else 16.dp, bottom = 2.dp)) }
                    Text(paragraphText(p, numbers), style = CarnetTheme.type.reading, color = c.text)
                }
            }
        } else for (p in paragraphs) {
            val text = paragraphText(p, numbers)
            val modifier = Modifier.padding(horizontal = gutter, vertical = 6.dp)
            if (!opened) { DropCapParagraph(text, modifier); opened = true }
            else Text(text, style = CarnetTheme.type.reading, color = c.text, modifier = modifier)
        }
    }
}

/** Texte d'un paragraphe, suivi de ses appels de référence en exposant (« ¹,² »). */
@Composable
private fun paragraphText(p: LoreParagraph, numbers: Map<String, Int>): AnnotatedString {
    val calls = p.refs.mapNotNull { numbers[it.ref] }.distinct().sorted()
    val italic = p.nature == CanonNature.INTERPRETATION
    return buildAnnotatedString {
        if (italic) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(p.text) } else append(p.text)
        if (calls.isNotEmpty()) withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.62.em, color = CarnetTheme.colors.gold)) {
            append(calls.joinToString(","))
        }
    }
}

/** Ligne sous le titre d'une section : sa nature dominante et, pour le canon de D&D, ses éditions. */
@Composable
private fun sectionNote(section: LoreSection): String? {
    if (section.paragraphs.any { it.variantGroup != null }) return null
    val natures = section.paragraphs.map { it.nature }.distinct()
    val nature = natures.singleOrNull() ?: return null
    val label = stringResource(nature.labelRes)
    val editions = section.paragraphs.mapNotNull { it.edition }.distinct()
    return when {
        nature == CanonNature.CANON_DND && editions.size == 1 -> stringResource(R.string.lore_section_nature, label, stringResource(editions.single().labelRes))
        section.type == LoreSectionType.BG3 || nature != CanonNature.CANON_DND -> label
        else -> null
    }
}

/** Badges de la fiche : les natures présentes, dans un ordre stable. */
@Composable
private fun natureBadges(view: EntryView): List<Pair<String, CanonKind>> {
    val natures = view.sections.filterIsInstance<Gated.Visible<LoreSection>>().flatMap { s -> s.value.paragraphs.map { it.nature } }.distinct().sorted()
    return natures.map { n ->
        stringResource(n.labelRes) to when (n) {
            CanonNature.CANON_BG3 -> CanonKind.BG3
            CanonNature.CANON_DND -> CanonKind.DND
            CanonNature.INTERPRETATION -> CanonKind.INTERPRETATION
            CanonNature.REAL_WORLD -> CanonKind.NEUTRE
        }
    }
}

/** Une référence en une ligne : titre (VF vérifiée entre parenthèses), éditeur, année, édition, ou page de wiki et révision. */
@Composable
private fun referenceLine(ref: Reference, all: Map<String, Reference>, citations: List<Citation>): String {
    val title = ref.titleFrench?.let { stringResource(R.string.lore_ref_vf, ref.titleOriginal, it) } ?: ref.titleOriginal
    val base = if (ref.kind == ReferenceKind.WIKI && ref.revision != null) stringResource(R.string.lore_ref_wiki, title, ref.revision!!.toInt())
    else listOfNotNull(title, ref.publisher, ref.year?.toString(), ref.edition?.let { stringResource(it.labelRes) }).joinToString(", ")
    // Pages et chapitres cités par la fiche, dans l'ordre où ils apparaissent.
    val pages = citations.mapNotNull { it.page }.distinct().map { stringResource(R.string.lore_ref_page, it) }
    val chapters = citations.mapNotNull { it.chapter }.distinct().map { stringResource(R.string.lore_ref_chapter, it) }
    val located = (pages + chapters).fold(base) { acc, loc -> stringResource(R.string.lore_ref_locator, acc, loc) }
    val via = ref.via?.let { all[it] }?.let { stringResource(R.string.lore_ref_via, it.titleOriginal) }
    return via?.let { stringResource(R.string.lore_join, located, it) } ?: located
}

/** Citations de chaque référence par les sections visibles (pour leurs pages et chapitres). */
private fun citationsByReference(view: EntryView): Map<String, List<Citation>> =
    view.sections.filterIsInstance<Gated.Visible<LoreSection>>()
        .flatMap { s -> s.value.paragraphs.flatMap { it.refs } }
        .groupBy { it.ref }

/** Numéro de chaque référence, dans l'ordre de son premier appel par une section visible. */
private fun citationNumbers(view: EntryView): Map<String, Int> {
    val order = LinkedHashSet<String>()
    view.sections.filterIsInstance<Gated.Visible<LoreSection>>().forEach { s -> s.value.paragraphs.forEach { p -> p.refs.forEach { order += it.ref } } }
    return order.withIndex().associate { (i, id) -> id to i + 1 }
}

/** Regroupe les paragraphes consécutifs qui partagent une même clé (les variantes sont contiguës). */
private fun <K> List<LoreParagraph>.groupConsecutiveBy(key: (LoreParagraph) -> K): List<Pair<K, List<LoreParagraph>>> = buildList {
    for (p in this@groupConsecutiveBy) {
        val k = key(p)
        if (isNotEmpty() && last().first == k && k != null) set(lastIndex, k to (last().second + p)) else add(k to listOf(p))
    }
}
