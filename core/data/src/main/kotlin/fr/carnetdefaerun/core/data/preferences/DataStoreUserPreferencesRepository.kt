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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.serialization.json.Json

/**
 * Stockage des préférences dans DataStore, sous forme d'un document JSON versionné par le schéma
 * de [UserPreferences] (les champs inconnus sont ignorés, les champs absents prennent leur valeur par défaut).
 */
@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : UserPreferencesRepository {

    /** Une lecture qui échoue donne les valeurs par défaut, puis le flux se réabonne : les écritures suivantes restent visibles. */
    override val preferences: Flow<UserPreferences> = store.data
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            if (attempt == 0L) emit(emptyPreferences())
            delay(minOf(30_000L, 500L shl attempt.coerceAtMost(6L).toInt()))
            true
        }
        .map { decode(it[KEY]) }
        .distinctUntilChanged()

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
