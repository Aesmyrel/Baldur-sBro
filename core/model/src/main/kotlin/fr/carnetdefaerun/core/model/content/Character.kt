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

/**
 * Capacité (ou option d'un choix) obtenue à un niveau, avec son nom ; [id] renvoie à sa fiche quand elle existe
 * (capacité ou sort). [grants] : ce que l'option apporte avec elle (sorts d'un terrain, actions d'un compagnon).
 */
@Serializable
data class LevelFeature(val name: Name, val id: ContentId? = null, val grants: List<ContentId> = emptyList())

@Serializable
data class ResourceCount(val resource: Resource, val amount: Int)

/** Valeur d'une colonne propre à la classe ; [key] est stable (ex. `sneak_attack_damage`), [value] tel quel (`1d6`, `+2`). */
@Serializable
data class LevelValue(val key: String, val value: String)

/** Sous-classe : capacités par niveau (niveaux de la classe parente), maîtrises et sorts qu'elle ajoute. */
@Serializable
@SerialName("subclass")
data class SubclassFacts(
    val parentClass: ContentId,
    /** Maîtrises accordées par la sous-classe, y compris en multiclasse. */
    val proficiencies: Proficiencies = Proficiencies(),
    val levels: List<SubclassLevel>,
) : Facts

@Serializable
data class SubclassLevel(
    val level: Int,
    val features: List<LevelFeature> = emptyList(),
    /** Choix à faire à ce niveau (un cœur bestial, un terrain…), avec ses options. */
    val choices: List<LevelChoice> = emptyList(),
    val spells: List<GrantedSpell> = emptyList(),
    val resources: List<ResourceCount> = emptyList(),
    /** Tiers de lanceur (chevalier occulte, escroc arcanique) : emplacements atteints à ce niveau. */
    val spellSlots: List<Int> = emptyList(),
)

/** Choix d'un niveau : [count] options à prendre parmi [options]. */
@Serializable
data class LevelChoice(val name: Name, val options: List<LevelFeature>, val count: Int = 1)

@Serializable
data class GrantedSpell(val spell: ContentId, val kind: GrantKind)

@Serializable
enum class GrantKind {
    /** Toujours préparé (sorts de domaine, de serment, de cercle, sort mineur offert). */
    @SerialName("always_prepared") ALWAYS_PREPARED,
    /** Ajouté à la liste où l'on choisit (liste étendue de l'occultiste). */
    @SerialName("expanded_list") EXPANDED_LIST,
}

/** Race jouable : vitesse, maîtrises et capacités communes à ses sous-races (page `Races` de bg3.wiki). */
@Serializable
@SerialName("race")
data class RaceFacts(
    val speedMetres: Double? = null,
    val proficiencies: Proficiencies = Proficiencies(),
    val features: List<LevelFeature> = emptyList(),
) : Facts

/** Sous-race : ce qu'elle ajoute à sa race (ou remplace, pour la vitesse). */
@Serializable
@SerialName("subrace")
data class SubraceFacts(
    val parentRace: ContentId,
    val speedMetres: Double? = null,
    val proficiencies: Proficiencies = Proficiencies(),
    val features: List<LevelFeature> = emptyList(),
) : Facts

/** Historique : les deux compétences qu'il rend maîtrisées. */
@Serializable
@SerialName("background")
data class BackgroundFacts(val skills: List<Skill>) : Facts

/** Don : capacités qu'il accorde et augmentation de caractéristique éventuelle. */
@Serializable
@SerialName("feat")
data class FeatFacts(
    val features: List<LevelFeature> = emptyList(),
    val abilityIncrease: AbilityIncrease? = null,
) : Facts

/**
 * Augmentation de caractéristique d'un don : [amount] à l'une des [choices] (toutes si la liste est vide),
 * sans dépasser [cap] ; [splittable] : peut se répartir en +1 sur deux caractéristiques (« Amélioration de caractéristique »).
 */
@Serializable
data class AbilityIncrease(val amount: Int, val choices: List<Ability> = emptyList(), val cap: Int = 20, val splittable: Boolean = false)
