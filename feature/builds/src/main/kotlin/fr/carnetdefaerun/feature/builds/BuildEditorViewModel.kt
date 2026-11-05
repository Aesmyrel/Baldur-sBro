package fr.carnetdefaerun.feature.builds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.builds.BuildRepository
import fr.carnetdefaerun.core.data.builds.RulesRepository
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.rules.CharacterSheet
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import fr.carnetdefaerun.core.rules.RulesData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EditorUiState {
    data object Loading : EditorUiState
    data object Missing : EditorUiState
    data class Ready(
        val id: Long,
        val build: Build,
        val sheet: CharacterSheet,
        val rows: List<LevelRow>,
        val catalog: Catalog,
        val rules: RulesData,
        /** Place du build dans la liste (1 à [total]), pour le fil d'Ariane. */
        val position: Int,
        val total: Int,
    ) : EditorUiState {
        val shareCode: String by lazy { BuildCode.encode(build) }
    }
}

/** État de l'éditeur, en fonction pure. */
fun editorState(id: Long, build: Build, rules: RulesData, catalog: Catalog, ids: List<Long>): EditorUiState.Ready =
    EditorUiState.Ready(
        id = id,
        build = build,
        sheet = CharacterSheetCalculator.compute(build, rules),
        rows = levelRows(build, rules),
        catalog = catalog,
        rules = rules,
        position = ids.indexOf(id).let { if (it < 0) 1 else it + 1 },
        total = ids.size.coerceAtLeast(1),
    )

/**
 * Éditeur d'un build. Le build en cours vit ici ([local]) : chaque édition s'applique aussitôt à l'écran, puis
 * s'enregistre dans la base du joueur. La base n'est relue qu'à l'ouverture.
 */
@HiltViewModel(assistedFactory = BuildEditorViewModel.Factory::class)
class BuildEditorViewModel @AssistedInject constructor(
    @Assisted private val id: Long,
    private val repository: BuildRepository,
    rulesRepository: RulesRepository,
    gateway: ContentGateway,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(id: Long): BuildEditorViewModel }

    private val local = MutableStateFlow<Build?>(null)
    private val missing = MutableStateFlow(false)
    private val rules = flow { emit(rulesRepository.data()) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val catalog = gateway.catalog().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val saved = repository.build(id).first()
            if (saved == null) missing.value = true else local.value = saved.build
        }
    }

    val state: StateFlow<EditorUiState> = combine(local, missing, rules, catalog, repository.builds.map { list -> list.map { it.id } }) { build, gone, data, names, ids ->
        when {
            gone -> EditorUiState.Missing
            build == null || data == null || names == null -> EditorUiState.Loading
            else -> editorState(id, build, data, names, ids)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState.Loading)

    fun edit(edit: BuildEdit) {
        val build = local.value ?: return
        val data = rules.value ?: return
        val names = catalog.value ?: return
        val updated = edit.applyTo(build, data, names)
        if (updated == build) return
        local.value = updated
        viewModelScope.launch { repository.save(id, updated) }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        repository.delete(id)
        onDeleted()
    }
}
