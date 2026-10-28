package fr.carnetdefaerun.core.model.content

import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Grande famille d'une entrée ; sert au regroupement des listes et des résultats de recherche. */
@Serializable
enum class EntryType {
    @SerialName("spell") SPELL,
    @SerialName("item") ITEM,
    @SerialName("condition") CONDITION,
    @SerialName("feature") FEATURE,
    @SerialName("place") PLACE,
    @SerialName("class") CLASS,
    @SerialName("subclass") SUBCLASS,
    @SerialName("race") RACE,
    @SerialName("subrace") SUBRACE,
    @SerialName("background") BACKGROUND,
    @SerialName("feat") FEAT,
    @SerialName("companion") COMPANION,
    @SerialName("quest") QUEST,
    @SerialName("achievement") ACHIEVEMENT,
    @SerialName("lore") LORE,
}

/** Nom affiché : français d'abord, anglais en secondaire, avec la fiabilité de la traduction. */
@Serializable
data class Name(
    val fr: String,
    val en: String,
    val status: NameStatus,
    /** Identifiant de la source du nom français (registre `data/sources.yaml`), absent pour une proposition du projet. */
    val source: String? = null,
)

/** Attribution d'une donnée dérivée : page source et révision exacte. */
@Serializable
data class Attribution(
    val source: String,
    val title: String,
    val revision: Long,
    val url: String,
)

/**
 * Bloc de texte affichable, rédigé par le projet en français, avec sa propre étiquette.
 * Un écran ne reçoit jamais un bloc verrouillé : le dépôt le remplace par un voile.
 */
@Serializable
data class Block(
    val id: String,
    val kind: BlockKind,
    val text: String,
    val tag: SpoilerTag,
    val references: List<String> = emptyList(),
)

@Serializable
enum class BlockKind {
    /** Effet mécanique, en style télégraphique (jamais le texte du jeu). */
    @SerialName("effect") EFFECT,

    /** Précision utile (interaction, piège, limite). */
    @SerialName("note") NOTE,

    /** Où et comment obtenir. */
    @SerialName("source") SOURCE,
}

/**
 * Lien typé vers une autre entrée. Son étiquette protège au moins autant que ses deux extrémités ;
 * le pipeline la calcule une fois toutes les entrées connues ([SpoilerTag.LOCKED] en attendant).
 */
@Serializable
data class Link(
    val target: ContentId,
    val relation: Relation,
    val tag: SpoilerTag = SpoilerTag.LOCKED,
)

@Serializable
enum class Relation {
    @SerialName("grants") GRANTS,
    @SerialName("granted_by") GRANTED_BY,
    @SerialName("applies") APPLIES,
    @SerialName("learned_by") LEARNED_BY,
    @SerialName("located_in") LOCATED_IN,
    @SerialName("variant_of") VARIANT_OF,
    @SerialName("see_also") SEE_ALSO,

    // Relations du lore (base de la future « Toile du lore »).
    @SerialName("worships") WORSHIPS,
    @SerialName("ally_of") ALLY_OF,
    @SerialName("rival_of") RIVAL_OF,
    @SerialName("member_of") MEMBER_OF,
    @SerialName("leads") LEADS,
    @SerialName("parent_of") PARENT_OF,
    @SerialName("created_by") CREATED_BY,
    @SerialName("appears_in") APPEARS_IN,
    @SerialName("mentioned_in") MENTIONED_IN,
}

/**
 * Entrée de la base de contenu. Le format JSON de `data/game/` est exactement ce type :
 * le pipeline l'écrit, la base embarquée le stocke, l'appli le relit.
 */
@Serializable
data class Entry(
    val id: ContentId,
    val type: EntryType,
    val name: Name,
    val aliases: List<String> = emptyList(),
    /** Étiquette de la fiche entière (ce qu'on apprend en l'ouvrant). */
    val tag: SpoilerTag,
    /** Étiquette du seul nom (ce qu'on apprend en le voyant dans une liste). Jamais plus stricte que [tag]. */
    val titleTag: SpoilerTag,
    val licence: String,
    val attribution: List<Attribution> = emptyList(),
    val facts: Facts,
    val blocks: List<Block> = emptyList(),
    val links: List<Link> = emptyList(),
    /** Champs qu'on n'a pas pu établir de façon fiable : vides dans [facts], signalés ici et à l'écran. */
    val unknown: List<String> = emptyList(),
)
