package fr.carnetdefaerun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.model.settings.UserPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Préférences globales (thème, mouvement, session) ; null tant qu'elles ne sont pas relues (l'écran de démarrage reste affiché). */
@HiltViewModel
class AppStateViewModel @Inject constructor(repository: UserPreferencesRepository) : ViewModel() {
    val preferences: StateFlow<UserPreferences?> =
        repository.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
