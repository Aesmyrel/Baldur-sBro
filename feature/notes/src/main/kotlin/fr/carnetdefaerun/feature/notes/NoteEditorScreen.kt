package fr.carnetdefaerun.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
import fr.carnetdefaerun.core.data.notes.NoteRepository
import fr.carnetdefaerun.core.data.notes.NoteTarget
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ChoiceTag
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.component.TextAction
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.ui.labelRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Note telle que l'éditeur la tient : texte, rattachement, et le nom de la fiche rattachée selon le verrou. */
data class NoteEditorUiState(
    val id: Long?,
    val text: String,
    val target: NoteTarget?,
    val labels: Map<ContentId, EntryLabel> = emptyMap(),
    val missing: Boolean = false,
)

@HiltViewModel(assistedFactory = NoteEditorViewModel.Factory::class)
class NoteEditorViewModel @AssistedInject constructor(
    @Assisted private val noteId: Long?,
    @Assisted targetId: String?,
    private val repository: NoteRepository,
    gateway: ContentGateway,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(noteId: Long?, targetId: String?): NoteEditorViewModel }

    private val initialTarget = targetId?.takeIf { ContentId.PATTERN.matches(it) }?.let { NoteTarget.OnEntry(ContentId(it)) }
    private val draft = MutableStateFlow<NoteEditorUiState?>(if (noteId == null) NoteEditorUiState(null, "", initialTarget) else null)

    init {
        if (noteId != null) viewModelScope.launch {
            val note = repository.note(noteId).first()
            draft.value = note?.let { NoteEditorUiState(it.id, it.text, it.target) } ?: NoteEditorUiState(noteId, "", null, missing = true)
        }
    }

    private val labels = gateway.entryLabels(draft.map { d -> setOfNotNull((d?.target as? NoteTarget.OnEntry)?.id) })

    val state: StateFlow<NoteEditorUiState?> = combine(draft, labels) { d, l -> d?.copy(labels = l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setText(text: String) { draft.value = draft.value?.copy(text = text.take(NoteRepository.MAX_LENGTH)) }
    fun setTarget(target: NoteTarget?) { draft.value = draft.value?.copy(target = target) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val d = draft.value ?: return@launch
        if (d.id == null) repository.create(d.text, d.target) else repository.save(d.id, d.text, d.target)
        onDone()
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        draft.value?.id?.let { repository.delete(it) }
        onDone()
    }
}

@Composable
fun NoteEditorRoute(noteId: Long?, targetId: String?, onBack: () -> Unit) {
    val viewModel = hiltViewModel<NoteEditorViewModel, NoteEditorViewModel.Factory>(
        key = "note-$noteId-$targetId", creationCallback = { it.create(noteId, targetId) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    NoteEditorScreen(state, viewModel::setText, viewModel::setTarget, onSave = { viewModel.save(onBack) }, onDelete = { viewModel.delete(onBack) }, onBack = onBack)
}

@Composable
fun NoteEditorScreen(
    state: NoteEditorUiState?,
    onText: (String) -> Unit,
    onTarget: (NoteTarget?) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    var confirming by rememberSaveable { mutableStateOf(false) }
    CarnetSectionScope(CarnetSection.OUTILS) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.note_crumb), onBack = onBack)
            if (state == null) return@Column
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.fillMaxHeight().widthIn(max = CarnetTheme.dimens.readingMaxWidth).fillMaxWidth().wrapContentWidth().verticalScroll(rememberScrollState())) {
                    if (state.missing) {
                        Text(stringResource(R.string.note_missing), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(gutter))
                        return@Column
                    }
                    Text(stringResource(if (state.id == null) R.string.note_title_new else R.string.note_title_edit), style = CarnetTheme.type.headline, color = c.text,
                        modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp).semantics { heading() })
                    NoteField(state.text, onText, Modifier.padding(horizontal = gutter, vertical = 8.dp))
                    val numbers = java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRANCE)
                    Text(stringResource(R.string.note_length, numbers.format(state.text.length), numbers.format(NoteRepository.MAX_LENGTH)), style = CarnetTheme.type.dataSmall, color = c.text2,
                        modifier = Modifier.padding(horizontal = gutter))

                    SectionHeader(stringResource(R.string.note_attach))
                    (state.target as? NoteTarget.OnEntry)?.let { entry ->
                        Column(Modifier.padding(horizontal = gutter, vertical = 6.dp)) {
                            Text(stringResource(R.string.note_entry, targetLabel(entry, state.labels)), style = CarnetTheme.type.reading, color = c.text)
                            TextAction(stringResource(R.string.note_entry_detach), { onTarget(null) })
                        }
                    }
                    TagRow(Modifier.padding(horizontal = gutter, vertical = 6.dp)) {
                        ChoiceTag(stringResource(R.string.note_attach_none), state.target == null, { onTarget(null) })
                        for (act in Act.entries.filter { it != Act.EPILOGUE }) {
                            val on = (state.target as? NoteTarget.OnAct)?.act == act
                            ChoiceTag(stringResource(act.labelRes), on, { selected -> onTarget(if (selected) NoteTarget.OnAct(act) else null) })
                        }
                    }

                    Row(Modifier.padding(horizontal = gutter, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GoldButton(stringResource(R.string.note_save), onSave, enabled = state.text.isNotBlank())
                    }
                    if (state.id != null) {
                        if (!confirming) Box(Modifier.padding(horizontal = gutter)) { TextAction(stringResource(R.string.note_delete), { confirming = true }) }
                        else Column(Modifier.padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.note_delete_confirm), style = CarnetTheme.type.reading, color = c.text)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GoldButton(stringResource(R.string.note_delete_no), { confirming = false })
                                QuietButton(stringResource(R.string.note_delete_yes), onDelete)
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Champ de note : feuillet encadré d'or, texte de lecture, plusieurs lignes ; il grandit avec le texte. */
@Composable
private fun NoteField(text: String, onText: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val shape = RoundedCornerShape(CarnetTheme.dimens.radius * 2)
    val hint = stringResource(R.string.note_hint)
    BasicTextField(
        value = text,
        onValueChange = onText,
        textStyle = CarnetTheme.type.reading.copy(color = c.text),
        cursorBrush = SolidColor(c.gold),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = hint },
        decorationBox = { field ->
            Box(Modifier.fillMaxWidth().heightIn(min = 200.dp).clip(shape).background(c.surface).border(1.dp, c.goldDeep, shape).padding(16.dp)) {
                if (text.isEmpty()) Text(hint, style = CarnetTheme.type.readingItalic, color = c.muted)
                field()
            }
        },
    )
}
