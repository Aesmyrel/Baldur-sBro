package fr.carnetdefaerun.core.model.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Maîtrises accordées : armures, armes (catégories entières ou types), compétences, instruments.
 * Pour une classe, [skills] est la liste où choisir [skillChoices] compétences.
 */
@Serializable
data class Proficiencies(
    val armour: List<ArmourProficiency> = emptyList(),
    val simpleWeapons: Boolean = false,
    val martialWeapons: Boolean = false,
    val weapons: List<WeaponType> = emptyList(),
    val skillChoices: Int = 0,
    val skills: List<Skill> = emptyList(),
    val instruments: Int = 0,
)

/**
 * Classe jouable. Points de vie : [hitPointsAtFirstLevel] au premier niveau de personnage, puis
 * [hitPointsPerLevel] à chaque niveau, chacun plus le modificateur de Constitution (pages de classe de bg3.wiki).
 */
@Serializable
@SerialName("class")
data class ClassFacts(
    val hitPointsAtFirstLevel: Int,
    val hitPointsPerLevel: Int,
    val savingThrows: List<Ability>,
    /** Caractéristique d'incantation ; pour une classe sans sorts, celle des objets, parchemins et équipements. */
    val spellcastingAbility: Ability? = null,
    val proficiencies: Proficiencies,
    /** Maîtrises obtenues en prenant cette classe en multiclasse (jamais de jet de sauvegarde). */
    val multiclassProficiencies: Proficiencies,
    val subclassLevel: Int,
    /** Magie de pacte : les emplacements d'un niveau sont tous du plus haut niveau atteint, et se rechargent au repos court. */
    val pactMagic: Boolean = false,
    val levels: List<ClassLevel>,
) : Facts

/** Un niveau de classe : ce qu'il apporte, tel que le tableau de progression de la page le donne. */
@Serializable
data class ClassLevel(
    val level: Int,
    val proficiencyBonus: Int,
    val features: List<LevelFeature> = emptyList(),
    /** Don (ou amélioration de caractéristiques) à ce niveau. */
    val feat: Boolean = false,
    /** Choix de la sous-classe à ce niveau. */
    val subclassChoice: Boolean = false,
    val cantripsKnown: Int? = null,
    /** Sorts connus ou appris (selon la classe). */
    val spellsKnown: Int? = null,
    /** Emplacements par niveau de sort : indice 0 pour le niveau 1. */
    val spellSlots: List<Int> = emptyList(),
    val resources: List<ResourceCount> = emptyList(),
    /** Autres colonnes du tableau (dés d'attaque sournoise, dégâts de rage…), sous une clé stable. */
    val values: List<LevelValue> = emptyList(),
)

/** Capacité obtenue à un niveau, avec son nom ; [id] renvoie à sa fiche quand elle existe (sinon signalée). */
@Serializable
data class LevelFeature(val name: Name, val id: ContentId? = null)

@Serializable
data class ResourceCount(val resource: Resource, val amount: Int)

/** Valeur d'une colonne propre à la classe ; [key] est stable (ex. `sneak_attack_damage`), [value] tel quel (`1d6`, `+2`). */
@Serializable
data class LevelValue(val key: String, val value: String)
