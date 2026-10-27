package fr.carnetdefaerun.core.testing

import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.model.settings.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Double de test des préférences, sans DataStore. */
class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val preferences: StateFlow<UserPreferences> = state
    val current: UserPreferences get() = state.value
    override suspend fun update(transform: (UserPreferences) -> UserPreferences) { state.value = transform(state.value) }
}
