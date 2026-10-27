package fr.carnetdefaerun.core.data.preferences

import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Préférences et progression déclarée du joueur. Toute modification se fait par une transformation atomique. */
interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun update(transform: (UserPreferences) -> UserPreferences)
}

/** Profil de spoiler courant : tous les dépôts de contenu filtrent à partir de ce flux. */
val UserPreferencesRepository.spoilerProfile: Flow<SpoilerProfile>
    get() = preferences.map { it.spoilerProfile }.distinctUntilChanged()
