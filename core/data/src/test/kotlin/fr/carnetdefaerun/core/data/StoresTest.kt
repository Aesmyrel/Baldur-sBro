package fr.carnetdefaerun.core.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
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
}
