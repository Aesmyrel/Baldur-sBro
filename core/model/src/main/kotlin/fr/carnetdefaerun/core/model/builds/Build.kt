package fr.carnetdefaerun.core.model.builds

import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import kotlinx.serialization.Serializable

/**
 * Build d'un personnage, tel que le joueur le compose : origine, achat de points, puis un choix par niveau.
 * Les identifiants renvoient aux fiches de jeu (classes, sous-classes, races, historiques, dons) ; tout le reste
 * (points de vie, classe d'armure, emplacements…) est calculé par le moteur de règles, jamais stocké.
 */
@Serializable
data class Build(
    val name: String,
    val race: ContentId? = null,
    val subrace: ContentId? = null,
    val background: ContentId? = null,
    /** Valeurs achetées, de 8 à 15, avant les bonus. */
    val baseScores: Map<Ability, Int> = Ability.entries.associateWith { 8 },
    /** Bonus de création : +2 et +1, à deux caractéristiques différentes. */
    val majorBonus: Ability? = null,
    val minorBonus: Ability? = null,
    /** Un niveau par entrée, dans l'ordre où ils sont pris ; le premier est la classe de départ. */
    val levels: List<BuildLevel> = emptyList(),
)

/** Un niveau du personnage : la classe prise, la sous-classe si ce niveau la choisit, le don s'il en donne un. */
@Serializable
data class BuildLevel(val classId: ContentId, val subclassId: ContentId? = null, val feat: FeatChoice? = null)

/** Don choisi ; pour une amélioration de caractéristique, la répartition des points. */
@Serializable
data class FeatChoice(val featId: ContentId, val abilityIncreases: Map<Ability, Int> = emptyMap())
