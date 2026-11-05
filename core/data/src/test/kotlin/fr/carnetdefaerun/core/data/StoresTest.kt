package fr.carnetdefaerun.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.data.builds.DataStorePartyRepository
import fr.carnetdefaerun.core.data.di.resilientStore
import fr.carnetdefaerun.core.data.navigation.DataStoreNavigationStateStore
import fr.carnetdefaerun.core.data.preferences.DataStoreUserPreferencesRepository
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.NavigationState
import fr.carnetdefaerun.core.model.settings.UserPreferences
import fr.carnetdefaerun.core.model.spoiler.Act
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Les magasins ne plantent jamais au démarrage : fichier corrompu ou contenu illisible → valeurs par défaut. */
@OptIn(ExperimentalCoroutinesApi::class)
class StoresTest {
    private val dir = createTempDirectory("stores").toFile()

    private fun TestScope.store(name: String, bytes: ByteArray? = null) = run {
        val file = File(dir, "$name.preferences_pb")
        if (bytes != null) file.writeBytes(bytes)
        resilientStore(backgroundScope) { file }
    }

    @Test
    fun `préférences, fichier corrompu → valeurs par défaut, puis écriture possible`() = runTest(UnconfinedTestDispatcher()) {
        val repo = DataStoreUserPreferencesRepository(store("p", byteArrayOf(0x7f, 0x00, 0x13, 0x42, 0x01)))
        assertThat(repo.preferences.first()).isEqualTo(UserPreferences())
        repo.update { it.copy(currentAct = Act.ACT_2) }
        assertThat(repo.preferences.first().currentAct).isEqualTo(Act.ACT_2)
    }

    @Test
    fun `préférences, JSON illisible → valeurs par défaut`() = runTest(UnconfinedTestDispatcher()) {
        val ds = store("pj")
        ds.edit { it[stringPreferencesKey("preferences")] = "{pas du json" }
        assertThat(DataStoreUserPreferencesRepository(ds).preferences.first()).isEqualTo(UserPreferences())
    }

    @Test
    fun `navigation, fichier corrompu → accueil, puis enregistrement possible`() = runTest(UnconfinedTestDispatcher()) {
        val nav = DataStoreNavigationStateStore(store("n", ByteArray(64) { 0xff.toByte() }))
        assertThat(nav.load()).isEqualTo(NavigationState())
        val next = NavigationState().push(Destination.Settings)
        nav.save(next)
        assertThat(nav.load()).isEqualTo(next)
    }

    @Test
    fun `groupe de quatre, emplacements bornés, un build à une seule place, contenu illisible ignoré`() = runTest(UnconfinedTestDispatcher()) {
        val ds = store("party")
        ds.edit { it[stringPreferencesKey("party_slots")] = "x,-3,,99999999999999999999" }
        val party = DataStorePartyRepository(ds)
        assertThat(party.slots.first()).containsExactly(null, null, null, null).inOrder()
        party.set(0, 7)
        party.set(2, 9)
        party.set(3, 7) // le build 7 quitte l'emplacement 0
        party.set(9, 1) // hors bornes : ignoré
        assertThat(party.slots.first()).containsExactly(null, null, 9L, 7L).inOrder()
        party.set(2, null)
        assertThat(party.slots.first()).containsExactly(null, null, null, 7L).inOrder()
    }
}
