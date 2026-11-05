package fr.carnetdefaerun.core.data.builds

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Groupe de quatre composé de builds enregistrés : un identifiant par emplacement, ou null s'il est libre. */
interface PartyRepository {
    val slots: Flow<List<Long?>>
    suspend fun set(slot: Int, buildId: Long?)

    companion object {
        const val SIZE = 4
    }
}

/** Groupe gardé dans un petit DataStore (inclus dans la sauvegarde Android). Un build supprimé y laisse un emplacement libre. */
@Singleton
class DataStorePartyRepository @Inject constructor(@Named("party") private val store: DataStore<Preferences>) : PartyRepository {
    override val slots: Flow<List<Long?>> = store.data.map { prefs -> parse(prefs[KEY]) }.distinctUntilChanged()

    override suspend fun set(slot: Int, buildId: Long?) {
        if (slot !in 0 until PartyRepository.SIZE) return
        store.edit { prefs ->
            val slots = parse(prefs[KEY]).toMutableList()
            // Un même build n'occupe qu'un emplacement : il quitte l'ancien.
            if (buildId != null) for (i in slots.indices) if (slots[i] == buildId) slots[i] = null
            slots[slot] = buildId
            prefs[KEY] = slots.joinToString(",") { it?.toString().orEmpty() }
        }
    }

    private fun parse(raw: String?): List<Long?> {
        val parts = raw?.split(',').orEmpty()
        return List(PartyRepository.SIZE) { i -> parts.getOrNull(i)?.toLongOrNull()?.takeIf { it > 0 } }
    }

    private companion object {
        val KEY = stringPreferencesKey("party_slots")
    }
}
