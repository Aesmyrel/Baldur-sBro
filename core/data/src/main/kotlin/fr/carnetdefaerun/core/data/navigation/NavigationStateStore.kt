package fr.carnetdefaerun.core.data.navigation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.carnetdefaerun.core.model.navigation.NavigationState
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Mémorise la pile de navigation pour rouvrir l'appli exactement là où on l'a quittée. */
interface NavigationStateStore {
    suspend fun load(): NavigationState
    suspend fun save(state: NavigationState)
}

@Singleton
class DataStoreNavigationStateStore @Inject constructor(
    @Named("navigation") private val store: DataStore<Preferences>,
) : NavigationStateStore {

    override suspend fun load(): NavigationState {
        val raw = try {
            store.data.first()[KEY]
        } catch (e: IOException) {
            null // fichier illisible : on repart de l'écran d'accueil
        }
        return raw?.let { runCatching { JSON.decodeFromString(NavigationState.serializer(), it) }.getOrNull() } ?: NavigationState()
    }

    override suspend fun save(state: NavigationState) {
        try {
            store.edit { it[KEY] = JSON.encodeToString(NavigationState.serializer(), state) }
        } catch (e: IOException) {
            // Stockage plein ou indisponible : la navigation continue, seule la reprise est perdue.
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("navigation")
        // Une destination retirée d'une version future ne doit pas empêcher l'appli de démarrer.
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
