package fr.carnetdefaerun.core.model.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Vocabulaire de règles partagé par les données, le moteur de règles et l'affichage. */

@Serializable
enum class Ability {
    @SerialName("str") STRENGTH,
    @SerialName("dex") DEXTERITY,
    @SerialName("con") CONSTITUTION,
    @SerialName("int") INTELLIGENCE,
    @SerialName("wis") WISDOM,
    @SerialName("cha") CHARISMA,
}

@Serializable
enum class Skill(val ability: Ability) {
    @SerialName("athletics") ATHLETICS(Ability.STRENGTH),
    @SerialName("acrobatics") ACROBATICS(Ability.DEXTERITY),
    @SerialName("sleight_of_hand") SLEIGHT_OF_HAND(Ability.DEXTERITY),
    @SerialName("stealth") STEALTH(Ability.DEXTERITY),
    @SerialName("arcana") ARCANA(Ability.INTELLIGENCE),
    @SerialName("history") HISTORY(Ability.INTELLIGENCE),
    @SerialName("investigation") INVESTIGATION(Ability.INTELLIGENCE),
    @SerialName("nature") NATURE(Ability.INTELLIGENCE),
    @SerialName("religion") RELIGION(Ability.INTELLIGENCE),
    @SerialName("animal_handling") ANIMAL_HANDLING(Ability.WISDOM),
    @SerialName("insight") INSIGHT(Ability.WISDOM),
    @SerialName("medicine") MEDICINE(Ability.WISDOM),
    @SerialName("perception") PERCEPTION(Ability.WISDOM),
    @SerialName("survival") SURVIVAL(Ability.WISDOM),
    @SerialName("deception") DECEPTION(Ability.CHARISMA),
    @SerialName("intimidation") INTIMIDATION(Ability.CHARISMA),
    @SerialName("performance") PERFORMANCE(Ability.CHARISMA),
    @SerialName("persuasion") PERSUASION(Ability.CHARISMA),
}

@Serializable
enum class DamageType {
    @SerialName("acid") ACID,
    @SerialName("bludgeoning") BLUDGEONING,
    @SerialName("cold") COLD,
    @SerialName("fire") FIRE,
    @SerialName("force") FORCE,
    @SerialName("lightning") LIGHTNING,
    @SerialName("necrotic") NECROTIC,
    @SerialName("piercing") PIERCING,
    @SerialName("poison") POISON,
    @SerialName("psychic") PSYCHIC,
    @SerialName("radiant") RADIANT,
    @SerialName("slashing") SLASHING,
    @SerialName("thunder") THUNDER,
}

@Serializable
enum class SpellSchool {
    @SerialName("abjuration") ABJURATION,
    @SerialName("conjuration") CONJURATION,
    @SerialName("divination") DIVINATION,
    @SerialName("enchantment") ENCHANTMENT,
    @SerialName("evocation") EVOCATION,
    @SerialName("illusion") ILLUSION,
    @SerialName("necromancy") NECROMANCY,
    @SerialName("transmutation") TRANSMUTATION,
}

@Serializable
enum class Rarity {
    @SerialName("common") COMMON,
    @SerialName("uncommon") UNCOMMON,
    @SerialName("rare") RARE,
    @SerialName("very_rare") VERY_RARE,
    @SerialName("legendary") LEGENDARY,
    @SerialName("story") STORY,
}

/**
 * Ressource consommée par une action. [wikiName] est le nom canonique du module `Module:Resource/data`
 * de bg3.wiki (révision 429231), qui fait foi pour les alias employés dans les pages.
 * Les emplacements de sort sont une seule ressource, avec un niveau.
 */
@Serializable
enum class Resource(val wikiName: String) {
    @SerialName("action") ACTION("Action"),
    @SerialName("bonus_action") BONUS_ACTION("Bonus Action"),
    @SerialName("reaction") REACTION("Reaction"),
    @SerialName("movement") MOVEMENT("Movement Speed"),
    @SerialName("half_movement") HALF_MOVEMENT("Movement Speed (Half cost)"),
    @SerialName("spell_slot") SPELL_SLOT("Spell Slot"),
    @SerialName("shadow_spell_slot") SHADOW_SPELL_SLOT("Shadow Spell Slot"),
    @SerialName("pact_slot") PACT_SLOT("Warlock Spell Slot"),
    @SerialName("arcane_arrow") ARCANE_ARROW("Arcane Arrow"),
    @SerialName("arcane_recovery") ARCANE_RECOVERY("Arcane Recovery Charge"),
    @SerialName("bardic_inspiration") BARDIC_INSPIRATION("Bardic Inspiration"),
    @SerialName("bladesong") BLADESONG_POWER("Bladesong Power"),
    @SerialName("channel_divinity") CHANNEL_DIVINITY("Channel Divinity Charge"),
    @SerialName("channel_oath") CHANNEL_OATH("Channel Oath Charge"),
    @SerialName("cosmic_omen") COSMIC_OMEN("Cosmic Omen"),
    @SerialName("eyestalk") EYESTALK_ACTION("Eyestalk action"),
    @SerialName("fungal_infestation") FUNGAL_INFESTATION("Fungal Infestation Charge"),
    @SerialName("ki") KI_POINT("Ki Point"),
    @SerialName("lay_on_hands") LAY_ON_HANDS("Lay on Hands Charge"),
    @SerialName("luck") LUCK_POINT("Luck Point"),
    @SerialName("natural_recovery") NATURAL_RECOVERY("Natural Recovery Charge"),
    @SerialName("rage") RAGE("Rage Charge"),
    @SerialName("sorcery") SORCERY_POINT("Sorcery Point"),
    @SerialName("star_map") STAR_MAP("Star Map"),
    @SerialName("superiority") SUPERIORITY_DIE("Superiority Die"),
    @SerialName("tides_of_chaos") TIDES_OF_CHAOS("Tides of Chaos Charge"),
    @SerialName("writhing_current") WRITHING_CURRENT("Writhing Current"),
    @SerialName("wild_shape") WILD_SHAPE("Wild Shape Charge"),
    @SerialName("war_priest") WAR_PRIEST("War Priest Charge"),
    ;

    companion object {
        fun byWikiName(name: String): Resource? = entries.firstOrNull { it.wikiName.equals(name, ignoreCase = true) }
    }
}

/** Coût d'une action : ressource et, pour un emplacement de sort, son niveau. */
@Serializable
data class Cost(val resource: Resource, val amount: Int = 1, val level: Int? = null)

@Serializable
enum class Rest {
    @SerialName("short") SHORT,
    @SerialName("long") LONG,
}

@Serializable
enum class RangeKind {
    @SerialName("self") SELF,
    @SerialName("melee") MELEE,
    @SerialName("ranged") RANGED,
    @SerialName("weapon") WEAPON,
    @SerialName("distance") DISTANCE,
}

/** Portée ; [metres] est renseigné pour une portée chiffrée. */
@Serializable
data class Range(val kind: RangeKind, val metres: Double? = null)

@Serializable
enum class AreaShape {
    @SerialName("radius") RADIUS,
    @SerialName("cone") CONE,
    @SerialName("line") LINE,
    @SerialName("sphere") SPHERE,
    @SerialName("circle") CIRCLE,
    @SerialName("cube") CUBE,
    @SerialName("wall") WALL,
}

@Serializable
data class Area(val shape: AreaShape, val metres: Double? = null)

/** Modificateur ajouté à un jet de dégâts ou de soins. */
@Serializable
enum class DamageModifier {
    /** Modificateur de la caractéristique d'incantation. */
    @SerialName("spellcasting") SPELLCASTING,

    /** Modificateur de Force ou de Dextérité, au choix (arme de finesse). */
    @SerialName("finesse") FINESSE,
}

/** Jet de dés de dégâts ou de soins, en notation de dés (`8d6`, `1d8+1`). */
@Serializable
data class Damage(
    val dice: String,
    val type: DamageType? = null,
    val healing: Boolean = false,
    val modifier: DamageModifier? = null,
) {
    init {
        require(DICE.matches(dice)) { "Notation de dés invalide : « $dice »" }
    }

    companion object {
        val DICE = Regex("^(\\d+d\\d+|\\d+)([+-](\\d+d\\d+|\\d+))*$")
    }
}

@Serializable
enum class SaveEffect {
    /** La moitié des dégâts en cas de réussite. */
    @SerialName("half") HALF,

    /** Aucun effet en cas de réussite. */
    @SerialName("negates") NEGATES,
}

@Serializable
enum class Handedness {
    @SerialName("one_handed") ONE_HANDED,
    @SerialName("two_handed") TWO_HANDED,
    @SerialName("versatile") VERSATILE,
}

@Serializable
enum class WeaponProperty {
    @SerialName("finesse") FINESSE,
    @SerialName("heavy") HEAVY,
    @SerialName("light") LIGHT,
    @SerialName("reach") REACH,
    @SerialName("thrown") THROWN,
    @SerialName("dippable") DIPPABLE,
}

/** Groupe de maîtrise d'une armure ou d'un objet tenu. */
@Serializable
enum class ArmourProficiency {
    @SerialName("light") LIGHT,
    @SerialName("medium") MEDIUM,
    @SerialName("heavy") HEAVY,
    @SerialName("shield") SHIELD,
    @SerialName("instrument") INSTRUMENT,
}
