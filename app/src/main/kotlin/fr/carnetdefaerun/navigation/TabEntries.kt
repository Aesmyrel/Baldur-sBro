package fr.carnetdefaerun.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Entrées décorées de l'onglet [current], toutes les piles restant en composition : changer d'onglet ne retire
 * rien de la pile quittée, donc ni l'état sauvegardable ni les ViewModels de l'onglet quitté ne sont détruits.
 */
@Composable
fun <K : Any, T : Any> rememberTabEntries(
    tabs: List<K>,
    current: K,
    stack: (K) -> List<T>,
    provider: (K) -> (T) -> NavEntry<T>,
): List<NavEntry<T>> {
    val decorators = listOf(rememberSaveableStateHolderNavEntryDecorator<T>(), rememberViewModelStoreNavEntryDecorator())
    val byTab = tabs.associateWith { tab -> key(tab) { rememberDecoratedNavEntries(stack(tab), decorators, provider(tab)) } }
    return byTab.getValue(current)
}
