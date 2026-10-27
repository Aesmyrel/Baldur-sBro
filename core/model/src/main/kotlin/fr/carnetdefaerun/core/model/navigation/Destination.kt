package fr.carnetdefaerun.core.model.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations de l'appli. Sérialisables : la pile de navigation est enregistrée à chaque changement
 * pour rouvrir exactement l'écran où l'on était (docs/PLAN.md §1.4). Les modules d'écrans s'y réfèrent
 * sans dépendre les uns des autres.
 */
@Serializable
sealed interface Destination {
    // Onglets principaux
    @Serializable data object Home : Destination
    @Serializable data object Encyclopedia : Destination
    @Serializable data object Codex : Destination
    @Serializable data object Builds : Destination
    @Serializable data object Tools : Destination

    // Jeu
    @Serializable data class EncyclopediaCategory(val category: String) : Destination
    @Serializable data class GameEntry(val id: String) : Destination
    @Serializable data object Companions : Destination
    @Serializable data class CompanionDetail(val id: String) : Destination
    @Serializable data class Progress(val act: String? = null) : Destination

    // Lore
    @Serializable data class CodexTheme(val theme: String) : Destination
    @Serializable data class LoreEntry(val id: String, val anchor: String? = null) : Destination
    @Serializable data object Timeline : Destination
    @Serializable data object Map : Destination
    @Serializable data object Cosmology : Destination

    // Builds et outils
    @Serializable data class BuildEditor(val buildId: Long) : Destination
    @Serializable data object Party : Destination
    @Serializable data object Probability : Destination
    @Serializable data object Dice : Destination

    // Transverses
    @Serializable data class Search(val query: String = "") : Destination
    @Serializable data object Notes : Destination
    @Serializable data class NoteEditor(val noteId: Long? = null, val targetId: String? = null) : Destination
    @Serializable data object Settings : Destination
    @Serializable data object NameCorrections : Destination
    @Serializable data object Credits : Destination
}

/** Les cinq onglets de la barre de navigation, dans l'ordre d'affichage. */
enum class TopLevel(val root: Destination) {
    PARTIE(Destination.Home),
    GRIMOIRE(Destination.Encyclopedia),
    CODEX(Destination.Codex),
    BUILDS(Destination.Builds),
    OUTILS(Destination.Tools),
}

/** État complet de la navigation : une pile par onglet et l'onglet courant. */
@Serializable
data class NavigationState(
    val current: TopLevel = TopLevel.PARTIE,
    val stacks: Map<TopLevel, List<Destination>> = TopLevel.entries.associateWith { listOf(it.root) },
) {
    fun stack(tab: TopLevel): List<Destination> = stacks[tab].orEmpty().ifEmpty { listOf(tab.root) }

    val visible: List<Destination> get() = stack(current)

    fun select(tab: TopLevel): NavigationState =
        // Toucher l'onglet courant ramène à sa racine ; un autre onglet retrouve sa pile.
        if (tab == current) copy(stacks = stacks + (tab to listOf(tab.root))) else copy(current = tab)

    fun push(destination: Destination): NavigationState {
        val tab = TopLevel.entries.firstOrNull { it.root == destination }
        if (tab != null) return select(tab)
        val stack = stack(current)
        if (stack.lastOrNull() == destination) return this
        return copy(stacks = stacks + (current to stack + destination))
    }

    /** Retour : dépile l'onglet courant ; à sa racine, revient à « Ma partie » ; renvoie null s'il faut quitter. */
    fun pop(): NavigationState? {
        val stack = stack(current)
        return when {
            stack.size > 1 -> copy(stacks = stacks + (current to stack.dropLast(1)))
            current != TopLevel.PARTIE -> copy(current = TopLevel.PARTIE)
            else -> null
        }
    }
}
