package fr.carnetdefaerun.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.navigation.NavigationStateStore
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.core.model.navigation.NavigationState
import fr.carnetdefaerun.core.model.navigation.TopLevel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Propriétaire de la pile de navigation (une pile par onglet). Chaque changement est enregistré :
 * à la relance, l'appli rouvre exactement le même écran (docs/PLAN.md §1.4).
 */
@HiltViewModel
class AppNavigator @Inject constructor(private val store: NavigationStateStore) : ViewModel() {

    private val _state = MutableStateFlow<NavigationState?>(null)

    /** null tant que l'état enregistré n'est pas relu (le premier écran attend, sans clignoter). */
    val state: StateFlow<NavigationState?> = _state.asStateFlow()

    private var pendingDeepLink: Destination? = null

    init {
        viewModelScope.launch {
            val restored = store.load()
            _state.value = pendingDeepLink?.let { restored.push(it) } ?: restored
            pendingDeepLink = null
        }
    }

    fun navigate(destination: Destination) = mutate { it.push(destination) }

    fun select(tab: TopLevel) = mutate { it.select(tab) }

    /** Retour : renvoie false s'il faut laisser le système fermer l'appli. */
    fun back(): Boolean {
        val current = _state.value ?: return false
        val next = current.pop() ?: return false
        set(next)
        return true
    }

    fun openDeepLink(destination: Destination) {
        if (_state.value == null) pendingDeepLink = destination else navigate(destination)
    }

    private fun mutate(transform: (NavigationState) -> NavigationState) {
        _state.value?.let { set(transform(it)) }
    }

    private fun set(next: NavigationState) {
        _state.value = next
        viewModelScope.launch { store.save(next) }
    }
}
