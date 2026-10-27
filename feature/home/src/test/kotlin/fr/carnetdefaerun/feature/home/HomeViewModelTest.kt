package fr.carnetdefaerun.feature.home

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.testing.FakeUserPreferencesRepository
import fr.carnetdefaerun.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `changer d'acte met à jour la progression déclarée`() = runTest {
        val repo = FakeUserPreferencesRepository()
        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        vm.selectAct(Act.ACT_2)
        assertThat(repo.current.currentAct).isEqualTo(Act.ACT_2)
        assertThat(vm.state.first { it.act == Act.ACT_2 }.act).isEqualTo(Act.ACT_2)
    }

    @Test
    fun `le mode session bascule`() = runTest {
        val repo = FakeUserPreferencesRepository()
        val vm = HomeViewModel(repo)
        vm.toggleSession()
        assertThat(repo.current.sessionActive).isTrue()
        vm.toggleSession()
        assertThat(repo.current.sessionActive).isFalse()
    }
}
