package fr.carnetdefaerun.core.model.content

import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Faits structurés d'une entrée, propres à son type. Ils ne contiennent aucun texte du jeu :
 * l'appli compose l'affichage français à partir de ces valeurs et des blocs rédigés par le projet.
 */
@Serializable
sealed interface Facts

/** Sort (tour de magie : niveau 0). */
@Serializable
@SerialName("spell")
data class SpellFacts(
    val level: Int,
    val school: SpellSchool? = null,
    val costs: List<Cost> = emptyList(),
    val range: Range? = null,
    val area: Area? = null,
    val attackRoll: Boolean = false,
    val save: Ability? = null,
    val saveEffect: SaveEffect? = null,
    val damage: List<Damage> = emptyList(),
    /** Le sort inflige les dégâts de l'arme tenue. */
    val weaponDamage: Boolean = false,
    /** Dés ajoutés par niveau d'emplacement au-delà du niveau de base, quand c'est la seule amélioration. */
    val upcastPerLevel: List<Damage> = emptyList(),
    val upcastable: Boolean = false,
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val durationTurns: Int? = null,
    val durationUntil: Rest? = null,
    val recharge: Recharge? = null,
    val learnedBy: List<LearnedAt> = emptyList(),
    val scribable: Boolean? = null,
    val gameUid: String? = null,
) : Facts

/**
 * Qui apprend un sort, à quel niveau dans la classe (ou de personnage pour une race).
 * [variant] précise une condition propre à la source (terrain d'un cercle, ascendance draconique),
 * en anglais tel que bg3.wiki l'écrit ; l'appli la traduit par un lexique, ou ne l'affiche pas.
 */
@Serializable
data class LearnedAt(
    val source: ContentId,
    val level: Int,
    val via: LearnVia = LearnVia.SPELL_LIST,
    val variant: String? = null,
)

@Serializable
enum class LearnVia {
    /** Sort de la liste de la classe, à choisir. */
    @SerialName("spell_list") SPELL_LIST,

    /** Sort de domaine, de serment, de cercle… : toujours préparé, accordé par la sous-classe. */
    @SerialName("subclass_spell") SUBCLASS_SPELL,

    /** Secrets magiques (barde). */
    @SerialName("magical_secrets") MAGICAL_SECRETS,

    /** Accordé par la race ou la sous-race. */
    @SerialName("racial") RACIAL,

    /** Autre voie, précisée par [LearnedAt.variant]. */
    @SerialName("other") OTHER,
}

@Serializable
enum class Recharge {
    @SerialName("per_turn") PER_TURN,
    @SerialName("per_combat") PER_COMBAT,
    @SerialName("short_rest") SHORT_REST,
    @SerialName("long_rest") LONG_REST,
}

/** Catégorie d'objet ; [wikiName] est le type employé par bg3.wiki. */
@Serializable
enum class ItemCategory(val wikiName: String) {
    @SerialName("weapon") WEAPON("Weapons"),
    @SerialName("helmet") HELMET("Helmets"),
    @SerialName("cloak") CLOAK("Cloaks"),
    @SerialName("clothing") CLOTHING("Clothing"),
    @SerialName("light_armour") LIGHT_ARMOUR("Light Armour"),
    @SerialName("medium_armour") MEDIUM_ARMOUR("Medium Armour"),
    @SerialName("heavy_armour") HEAVY_ARMOUR("Heavy Armour"),
    @SerialName("shield") SHIELD("Shields"),
    @SerialName("gloves") GLOVES("Gloves"),
    @SerialName("boots") BOOTS("Boots"),
    @SerialName("amulet") AMULET("Amulets"),
    @SerialName("ring") RING("Rings"),
    @SerialName("instrument") INSTRUMENT("Musical Instruments"),
    @SerialName("camp_clothing") CAMP_CLOTHING("Camp Clothing"),
    @SerialName("camp_shoes") CAMP_SHOES("Camp Shoes"),
    @SerialName("underwear") UNDERWEAR("Underwear"),
    ;

    companion object {
        fun byWikiName(name: String): ItemCategory? = entries.firstOrNull { it.wikiName.equals(name.trim(), ignoreCase = true) }
    }
}

/** Type d'arme ; [wikiName] est le nom de la page de type sur bg3.wiki. */
@Serializable
enum class WeaponType(val wikiName: String) {
    @SerialName("battleaxe") BATTLEAXE("Battleaxes"),
    @SerialName("club") CLUB("Clubs"),
    @SerialName("dagger") DAGGER("Daggers"),
    @SerialName("dart") DART("Darts"),
    @SerialName("flail") FLAIL("Flails"),
    @SerialName("glaive") GLAIVE("Glaives"),
    @SerialName("greataxe") GREATAXE("Greataxes"),
    @SerialName("greatclub") GREATCLUB("Greatclubs"),
    @SerialName("greatsword") GREATSWORD("Greatswords"),
    @SerialName("halberd") HALBERD("Halberds"),
    @SerialName("hand_crossbow") HAND_CROSSBOW("Hand Crossbows"),
    @SerialName("handaxe") HANDAXE("Handaxes"),
    @SerialName("heavy_crossbow") HEAVY_CROSSBOW("Heavy Crossbows"),
    @SerialName("javelin") JAVELIN("Javelins"),
    @SerialName("light_crossbow") LIGHT_CROSSBOW("Light Crossbows"),
    @SerialName("light_hammer") LIGHT_HAMMER("Light Hammers"),
    @SerialName("longbow") LONGBOW("Longbows"),
    @SerialName("longsword") LONGSWORD("Longswords"),
    @SerialName("mace") MACE("Maces"),
    @SerialName("maul") MAUL("Mauls"),
    @SerialName("morningstar") MORNINGSTAR("Morningstars"),
    @SerialName("pike") PIKE("Pikes"),
    @SerialName("quarterstaff") QUARTERSTAFF("Quarterstaves"),
    @SerialName("rapier") RAPIER("Rapiers"),
    @SerialName("scimitar") SCIMITAR("Scimitars"),
    @SerialName("shortbow") SHORTBOW("Shortbows"),
    @SerialName("shortsword") SHORTSWORD("Shortswords"),
    @SerialName("sickle") SICKLE("Sickles"),
    @SerialName("sling") SLING("Slings"),
    @SerialName("spear") SPEAR("Spears"),
    @SerialName("trident") TRIDENT("Tridents"),
    @SerialName("war_pick") WAR_PICK("War Picks"),
    @SerialName("warhammer") WARHAMMER("Warhammers"),
    ;

    companion object {
        fun byWikiName(name: String): WeaponType? = entries.firstOrNull { it.wikiName.equals(name.trim(), ignoreCase = true) }
    }
}

@Serializable
data class WeaponFacts(
    val type: WeaponType,
    val martial: Boolean,
    val ranged: Boolean,
    val handedness: Handedness,
    val damage: List<Damage>,
    val versatileDamage: Damage? = null,
    val properties: Set<WeaponProperty> = emptySet(),
    val rangeMetres: Double? = null,
    val weaponActions: List<ContentId> = emptyList(),
)

@Serializable
data class ArmourFacts(
    /** Classe d'armure de base (armures) ou bonus à la CA (boucliers). */
    val armourClass: Int? = null,
    val armourClassBonus: Int? = null,
    val proficiency: ArmourProficiency? = null,
    val stealthDisadvantage: Boolean = false,
)

/** Objet : équipement, arme. Les sources d'obtention portent chacune leur étiquette. */
@Serializable
@SerialName("item")
data class ItemFacts(
    val category: ItemCategory,
    val rarity: Rarity? = null,
    val enchantment: Int? = null,
    val weightKg: Double? = null,
    val priceGold: Int? = null,
    val weapon: WeaponFacts? = null,
    val armour: ArmourFacts? = null,
    val effects: List<Effect> = emptyList(),
    val sources: List<ItemSource> = emptyList(),
    val gameUid: String? = null,
) : Facts

/** Où et comment obtenir un objet. Le détail (personnage, quête) n'est jamais stocké en clair hors du [tag]. */
@Serializable
data class ItemSource(
    val act: Act? = null,
    val place: ContentId? = null,
    val method: SourceMethod = SourceMethod.UNKNOWN,
    /** Nom propre du personnage marchand, s'il y a lieu (affiché seulement si [tag] le permet). */
    val vendor: String? = null,
    val tag: SpoilerTag = SpoilerTag.SAFE,
)

@Serializable
enum class SourceMethod {
    @SerialName("vendor") VENDOR,
    @SerialName("container") CONTAINER,
    @SerialName("ground") GROUND,
    @SerialName("carried") CARRIED,
    @SerialName("reward") REWARD,
    @SerialName("unknown") UNKNOWN,
}

/**
 * Effet mécanique structuré. Ce qui ne se laisse pas structurer de façon sûre est un bloc
 * [BlockKind.EFFECT] rédigé par le projet, jamais une reprise du texte du jeu.
 */
@Serializable
sealed interface Effect

@Serializable
@SerialName("ability")
data class AbilityBonus(val ability: Ability, val delta: Int? = null, val setTo: Int? = null, val cap: Int? = null) : Effect

@Serializable
@SerialName("skill")
data class SkillBonus(val skill: Skill, val delta: Int) : Effect

@Serializable
@SerialName("armour_class")
data class ArmourClassBonus(val delta: Int) : Effect

@Serializable
@SerialName("saving_throws")
data class SavingThrowBonus(val delta: Int, val ability: Ability? = null) : Effect

@Serializable
@SerialName("save_advantage")
data class SaveAdvantage(val ability: Ability) : Effect

@Serializable
@SerialName("check_advantage")
data class CheckAdvantage(val skill: Skill? = null, val ability: Ability? = null) : Effect

@Serializable
@SerialName("resistance")
data class DamageResistance(val type: DamageType) : Effect

@Serializable
@SerialName("critical_immunity")
data object CriticalHitImmunity : Effect

@Serializable
@SerialName("grants")
data class Grants(val target: ContentId, val recharge: Recharge? = null) : Effect

/** Le porteur est sous l'effet d'un état tant qu'il porte l'objet. */
@Serializable
@SerialName("wearer_condition")
data class WearerCondition(val condition: ContentId) : Effect

/** État : effets rédigés par le projet dans les blocs, groupes techniques du jeu ici. */
@Serializable
@SerialName("condition")
data class ConditionFacts(
    val groups: List<String> = emptyList(),
    val gameUid: String? = null,
) : Facts

/** Capacité (passive, action, action d'arme). */
@Serializable
@SerialName("feature")
data class FeatureFacts(
    val kind: FeatureKind,
    val costs: List<Cost> = emptyList(),
    val recharge: Recharge? = null,
    val applies: List<ContentId> = emptyList(),
    val gameUid: String? = null,
) : Facts

@Serializable
enum class FeatureKind {
    @SerialName("passive") PASSIVE,
    @SerialName("action") ACTION,
    @SerialName("weapon_action") WEAPON_ACTION,
}

/** Lieu du jeu ; [act] vient de la page du lieu. */
@Serializable
@SerialName("place")
data class PlaceFacts(
    val act: Act? = null,
    val region: ContentId? = null,
) : Facts
