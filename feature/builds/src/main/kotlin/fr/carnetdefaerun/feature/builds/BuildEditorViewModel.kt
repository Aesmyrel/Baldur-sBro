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
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.rules.CharacterSheet
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import fr.carnetdefaerun.core.rules.DifficultyRules
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface EditorUiState {
    data object Loading : EditorUiState
    data object Missing : EditorUiState

    /** Enregistrement illisible : seul son nom est connu ; on peut le supprimer. */
    data class Damaged(val id: Long, val name: String) : EditorUiState
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
        /** Mode de difficulté réglé par le joueur : Explorateur change la fiche. */
        val difficulty: Difficulty = Difficulty.BALANCED,
    ) : EditorUiState {
        val shareCode: String by lazy { BuildCode.encode(build) }
    }
}

/** État de l'éditeur, en fonction pure. */
fun editorState(id: Long, build: Build, rules: RulesData, catalog: Catalog, ids: List<Long>, difficulty: Difficulty = Difficulty.BALANCED): EditorUiState.Ready =
    EditorUiState.Ready(
        id = id,
        build = build,
        sheet = CharacterSheetCalculator.compute(build, rules, DifficultyRules.of(difficulty)),
        rows = levelRows(build, rules),
        catalog = catalog,
        rules = rules,
        position = ids.indexOf(id).let { if (it < 0) 1 else it + 1 },
        total = ids.size.coerceAtLeast(1),
        difficulty = difficulty,
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
    preferences: UserPreferencesRepository,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(id: Long): BuildEditorViewModel }

    private val local = MutableStateFlow<Build?>(null)
    private val missing = MutableStateFlow(false)
    private val damaged = MutableStateFlow<String?>(null)

    /** Une écriture à la fois, toujours celle du build le plus récent : deux éditions rapides ne peuvent pas s'inverser. */
    private val writer = Mutex()
    private val rules = flow { emit(rulesRepository.data()) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val catalog = gateway.catalog().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val saved = repository.build(id).first()
            when {
                saved == null -> missing.value = true
                saved.damaged -> damaged.value = saved.build.name
                else -> local.value = saved.build
            }
        }
    }

    private val context = combine(repository.builds.map { list -> list.map { it.id } }, preferences.preferences.map { it.difficulty }) { ids, d -> ids to d }

    private val status = combine(missing, damaged) { gone, broken -> gone to broken }

    val state: StateFlow<EditorUiState> = combine(local, status, rules, catalog, context) { build, (gone, broken), data, names, (ids, difficulty) ->
        when {
            gone -> EditorUiState.Missing
            broken != null -> EditorUiState.Damaged(id, broken)
            build == null || data == null || names == null -> EditorUiState.Loading
            else -> editorState(id, build, data, names, ids, difficulty)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState.Loading)

    fun edit(edit: BuildEdit) {
        val build = local.value ?: return
        val data = rules.value ?: return
        val names = catalog.value ?: return
        val updated = edit.applyTo(build, data, names)
        if (updated == build) return
        local.value = updated
        viewModelScope.launch { writer.withLock { local.value?.let { repository.save(id, it) } } }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        repository.delete(id)
        onDeleted()
    }
}
