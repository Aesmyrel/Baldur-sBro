package fr.carnetdefaerun.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.spoiler.Act
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val act: Act = Act.PROLOGUE,
    val difficulty: Difficulty = Difficulty.BALANCED,
    val sessionActive: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val preferences: UserPreferencesRepository) : ViewModel() {

    /** `null` tant que les préférences ne sont pas relues : l'en-tête ne montre jamais un acte provisoire. */
    val state: StateFlow<HomeUiState?> = preferences.preferences
        .map { HomeUiState(it.currentAct, it.difficulty, it.sessionActive) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Changer d'acte en un appui : tout le contenu se déverrouille ou se voile aussitôt. */
    fun selectAct(act: Act) = viewModelScope.launch { preferences.update { it.copy(currentAct = act) } }

    fun toggleSession() = viewModelScope.launch { preferences.update { it.copy(sessionActive = !it.sessionActive) } }
}
