package fr.carnetdefaerun.core.model.navigation

import fr.carnetdefaerun.core.model.content.ContentId
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

/** Profondeur maximale d'une pile relue : au-delà, on garde les écrans les plus récents. */
const val MAX_STACK_DEPTH = 30

/** Longueur maximale d'une recherche persistée ou reçue par lien. */
const val MAX_QUERY_LENGTH = 200

/** Ancre de section : son numéro d'ordre (`s3`), jamais un titre, qui pourrait être un spoiler. */
val SECTION_ANCHOR = Regex("^s\\d{1,3}$")

private val SLUG = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

/** Une destination relue (DataStore, lien profond) n'est acceptée que si ses paramètres sont bien formés. */
fun Destination.isWellFormed(): Boolean = when (this) {
    is Destination.GameEntry -> ContentId.PATTERN.matches(id)
    is Destination.CompanionDetail -> ContentId.PATTERN.matches(id)
    is Destination.LoreEntry -> ContentId.PATTERN.matches(id) && (anchor == null || SECTION_ANCHOR.matches(anchor))
    is Destination.CodexTheme -> theme.length <= 40 && SLUG.matches(theme)
    is Destination.EncyclopediaCategory -> category.length <= 40 && SLUG.matches(category)
    is Destination.Progress -> act == null || (act.length <= 12 && SLUG.matches(act))
    is Destination.Search -> query.length <= MAX_QUERY_LENGTH
    is Destination.BuildEditor -> buildId > 0
    is Destination.NoteEditor -> (noteId == null || noteId > 0) && (targetId == null || ContentId.PATTERN.matches(targetId))
    else -> true
}

/**
 * État relu, remis d'aplomb : chaque pile commence par la racine de son onglet, ne contient ni
 * destination mal formée ni racine d'un autre onglet, et reste de profondeur bornée.
 */
fun NavigationState.normalized(): NavigationState {
    val roots = TopLevel.entries.map { it.root }.toSet()
    val clean = TopLevel.entries.associateWith { tab ->
        val inner = stacks[tab].orEmpty().filter { it !in roots && it.isWellFormed() }.takeLast(MAX_STACK_DEPTH - 1)
        listOf(tab.root) + inner
    }
    return NavigationState(current, clean)
}
