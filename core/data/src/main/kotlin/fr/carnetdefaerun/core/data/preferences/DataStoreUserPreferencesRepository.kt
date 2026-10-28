package fr.carnetdefaerun.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.carnetdefaerun.core.model.settings.UserPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Stockage des préférences dans DataStore, sous forme d'un document JSON versionné par le schéma
 * de [UserPreferences] (les champs inconnus sont ignorés, les champs absents prennent leur valeur par défaut).
 */
@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { decode(it[KEY]) }

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs -> prefs[KEY] = JSON.encodeToString(UserPreferences.serializer(), transform(decode(prefs[KEY]))) }
    }

    private fun decode(raw: String?): UserPreferences =
        raw?.let { runCatching { JSON.decodeFromString(UserPreferences.serializer(), it) }.getOrNull() } ?: UserPreferences()

    private companion object {
        val KEY = stringPreferencesKey("preferences")
        val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
