package fr.carnetdefaerun.core.ui

import androidx.annotation.StringRes
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.AreaShape
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.CanonNature
import fr.carnetdefaerun.core.model.content.DamageType
import fr.carnetdefaerun.core.model.content.Edition
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatureKind
import fr.carnetdefaerun.core.model.content.Handedness
import fr.carnetdefaerun.core.model.content.ItemCategory
import fr.carnetdefaerun.core.model.content.LoreTheme
import fr.carnetdefaerun.core.model.content.RangeKind
import fr.carnetdefaerun.core.model.content.Rarity
import fr.carnetdefaerun.core.model.content.Recharge
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.Rest
import fr.carnetdefaerun.core.model.content.SaveEffect
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.content.SpellSchool
import fr.carnetdefaerun.core.model.content.WeaponProperty

// Libellés français du vocabulaire de règles (D-075). Un `when` exhaustif : toute valeur ajoutée au modèle
// doit recevoir son libellé, sinon la compilation échoue.

/** Nom de la catégorie, au pluriel (listes, regroupement des résultats). */
@get:StringRes
val EntryType.pluralRes: Int
    get() = when (this) {
        EntryType.SPELL -> R.string.type_spell
        EntryType.ITEM -> R.string.type_item
        EntryType.CONDITION -> R.string.type_condition
        EntryType.FEATURE -> R.string.type_feature
        EntryType.PLACE -> R.string.type_place
        EntryType.CLASS -> R.string.type_class
        EntryType.SUBCLASS -> R.string.type_subclass
        EntryType.RACE -> R.string.type_race
        EntryType.SUBRACE -> R.string.type_subrace
        EntryType.BACKGROUND -> R.string.type_background
        EntryType.FEAT -> R.string.type_feat
        EntryType.COMPANION -> R.string.type_companion
        EntryType.QUEST -> R.string.type_quest
        EntryType.ACHIEVEMENT -> R.string.type_achievement
        EntryType.LORE -> R.string.type_lore
    }

/** Nom du type d'une entrée, au singulier. */
@get:StringRes
val EntryType.singularRes: Int
    get() = when (this) {
        EntryType.SPELL -> R.string.type1_spell
        EntryType.ITEM -> R.string.type1_item
        EntryType.CONDITION -> R.string.type1_condition
        EntryType.FEATURE -> R.string.type1_feature
        EntryType.PLACE -> R.string.type1_place
        EntryType.CLASS -> R.string.type1_class
        EntryType.SUBCLASS -> R.string.type1_subclass
        EntryType.RACE -> R.string.type1_race
        EntryType.SUBRACE -> R.string.type1_subrace
        EntryType.BACKGROUND -> R.string.type1_background
        EntryType.FEAT -> R.string.type1_feat
        EntryType.COMPANION -> R.string.type1_companion
        EntryType.QUEST -> R.string.type1_quest
        EntryType.ACHIEVEMENT -> R.string.type1_achievement
        EntryType.LORE -> R.string.type1_lore
    }

@get:StringRes
val Ability.labelRes: Int
    get() = when (this) {
        Ability.STRENGTH -> R.string.ability_str
        Ability.DEXTERITY -> R.string.ability_dex
        Ability.CONSTITUTION -> R.string.ability_con
        Ability.INTELLIGENCE -> R.string.ability_int
        Ability.WISDOM -> R.string.ability_wis
        Ability.CHARISMA -> R.string.ability_cha
    }

@get:StringRes
val Skill.labelRes: Int
    get() = when (this) {
        Skill.ATHLETICS -> R.string.skill_athletics
        Skill.ACROBATICS -> R.string.skill_acrobatics
        Skill.SLEIGHT_OF_HAND -> R.string.skill_sleight_of_hand
        Skill.STEALTH -> R.string.skill_stealth
        Skill.ARCANA -> R.string.skill_arcana
        Skill.HISTORY -> R.string.skill_history
        Skill.INVESTIGATION -> R.string.skill_investigation
        Skill.NATURE -> R.string.skill_nature
        Skill.RELIGION -> R.string.skill_religion
        Skill.ANIMAL_HANDLING -> R.string.skill_animal_handling
        Skill.INSIGHT -> R.string.skill_insight
        Skill.MEDICINE -> R.string.skill_medicine
        Skill.PERCEPTION -> R.string.skill_perception
        Skill.SURVIVAL -> R.string.skill_survival
        Skill.DECEPTION -> R.string.skill_deception
        Skill.INTIMIDATION -> R.string.skill_intimidation
        Skill.PERFORMANCE -> R.string.skill_performance
        Skill.PERSUASION -> R.string.skill_persuasion
    }

/** Complément de « dégâts » : « de feu », « contondants ». */
@get:StringRes
val DamageType.labelRes: Int
    get() = when (this) {
        DamageType.ACID -> R.string.damage_acid
        DamageType.BLUDGEONING -> R.string.damage_bludgeoning
        DamageType.COLD -> R.string.damage_cold
        DamageType.FIRE -> R.string.damage_fire
        DamageType.FORCE -> R.string.damage_force
        DamageType.LIGHTNING -> R.string.damage_lightning
        DamageType.NECROTIC -> R.string.damage_necrotic
        DamageType.PIERCING -> R.string.damage_piercing
        DamageType.POISON -> R.string.damage_poison
        DamageType.PSYCHIC -> R.string.damage_psychic
        DamageType.RADIANT -> R.string.damage_radiant
        DamageType.SLASHING -> R.string.damage_slashing
        DamageType.THUNDER -> R.string.damage_thunder
    }

@get:StringRes
val SpellSchool.labelRes: Int
    get() = when (this) {
        SpellSchool.ABJURATION -> R.string.school_abjuration
        SpellSchool.CONJURATION -> R.string.school_conjuration
        SpellSchool.DIVINATION -> R.string.school_divination
        SpellSchool.ENCHANTMENT -> R.string.school_enchantment
        SpellSchool.EVOCATION -> R.string.school_evocation
        SpellSchool.ILLUSION -> R.string.school_illusion
        SpellSchool.NECROMANCY -> R.string.school_necromancy
        SpellSchool.TRANSMUTATION -> R.string.school_transmutation
    }

@get:StringRes
val Rarity.labelRes: Int
    get() = when (this) {
        Rarity.COMMON -> R.string.rarity_common
        Rarity.UNCOMMON -> R.string.rarity_uncommon
        Rarity.RARE -> R.string.rarity_rare
        Rarity.VERY_RARE -> R.string.rarity_very_rare
        Rarity.LEGENDARY -> R.string.rarity_legendary
        Rarity.STORY -> R.string.rarity_story
    }

@get:StringRes
val Resource.labelRes: Int
    get() = when (this) {
        Resource.ACTION -> R.string.resource_action
        Resource.BONUS_ACTION -> R.string.resource_bonus_action
        Resource.REACTION -> R.string.resource_reaction
        Resource.MOVEMENT -> R.string.resource_movement
        Resource.HALF_MOVEMENT -> R.string.resource_half_movement
        Resource.SPELL_SLOT -> R.string.resource_spell_slot
        Resource.SHADOW_SPELL_SLOT -> R.string.resource_shadow_spell_slot
        Resource.PACT_SLOT -> R.string.resource_pact_slot
        Resource.ARCANE_ARROW -> R.string.resource_arcane_arrow
        Resource.ARCANE_RECOVERY -> R.string.resource_arcane_recovery
        Resource.BARDIC_INSPIRATION -> R.string.resource_bardic_inspiration
        Resource.BLADESONG_POWER -> R.string.resource_bladesong
        Resource.CHANNEL_DIVINITY -> R.string.resource_channel_divinity
        Resource.CHANNEL_OATH -> R.string.resource_channel_oath
        Resource.COSMIC_OMEN -> R.string.resource_cosmic_omen
        Resource.EYESTALK_ACTION -> R.string.resource_eyestalk
        Resource.FUNGAL_INFESTATION -> R.string.resource_fungal_infestation
        Resource.KI_POINT -> R.string.resource_ki
        Resource.LAY_ON_HANDS -> R.string.resource_lay_on_hands
        Resource.LUCK_POINT -> R.string.resource_luck
        Resource.NATURAL_RECOVERY -> R.string.resource_natural_recovery
        Resource.RAGE -> R.string.resource_rage
        Resource.SORCERY_POINT -> R.string.resource_sorcery
        Resource.STAR_MAP -> R.string.resource_star_map
        Resource.SUPERIORITY_DIE -> R.string.resource_superiority
        Resource.TIDES_OF_CHAOS -> R.string.resource_tides_of_chaos
        Resource.WRITHING_CURRENT -> R.string.resource_writhing_current
        Resource.WILD_SHAPE -> R.string.resource_wild_shape
        Resource.WAR_PRIEST -> R.string.resource_war_priest
    }

@get:StringRes
val Recharge.labelRes: Int
    get() = when (this) {
        Recharge.PER_TURN -> R.string.recharge_per_turn
        Recharge.PER_COMBAT -> R.string.recharge_per_combat
        Recharge.SHORT_REST -> R.string.recharge_short_rest
        Recharge.LONG_REST -> R.string.recharge_long_rest
    }

/** Fin d'une durée : « jusqu'au prochain repos long ». */
@get:StringRes
val Rest.untilRes: Int
    get() = when (this) {
        Rest.SHORT -> R.string.rest_short
        Rest.LONG -> R.string.rest_long
    }

@get:StringRes
val RangeKind.labelRes: Int
    get() = when (this) {
        RangeKind.SELF -> R.string.range_self
        RangeKind.MELEE -> R.string.range_melee
        RangeKind.RANGED -> R.string.range_ranged
        RangeKind.WEAPON -> R.string.range_weapon
        RangeKind.DISTANCE -> R.string.range_distance
    }

@get:StringRes
val AreaShape.labelRes: Int
    get() = when (this) {
        AreaShape.RADIUS -> R.string.area_radius
        AreaShape.CONE -> R.string.area_cone
        AreaShape.LINE -> R.string.area_line
        AreaShape.SPHERE -> R.string.area_sphere
        AreaShape.CIRCLE -> R.string.area_circle
        AreaShape.CUBE -> R.string.area_cube
        AreaShape.WALL -> R.string.area_wall
    }

@get:StringRes
val SaveEffect.labelRes: Int
    get() = when (this) {
        SaveEffect.HALF -> R.string.save_half
        SaveEffect.NEGATES -> R.string.save_negates
    }

@get:StringRes
val Handedness.labelRes: Int
    get() = when (this) {
        Handedness.ONE_HANDED -> R.string.hand_one
        Handedness.TWO_HANDED -> R.string.hand_two
        Handedness.VERSATILE -> R.string.hand_versatile
    }

@get:StringRes
val WeaponProperty.labelRes: Int
    get() = when (this) {
        WeaponProperty.FINESSE -> R.string.prop_finesse
        WeaponProperty.HEAVY -> R.string.prop_heavy
        WeaponProperty.LIGHT -> R.string.prop_light
        WeaponProperty.REACH -> R.string.prop_reach
        WeaponProperty.THROWN -> R.string.prop_thrown
        WeaponProperty.DIPPABLE -> R.string.prop_dippable
    }

@get:StringRes
val ArmourProficiency.labelRes: Int
    get() = when (this) {
        ArmourProficiency.LIGHT -> R.string.armour_light
        ArmourProficiency.MEDIUM -> R.string.armour_medium
        ArmourProficiency.HEAVY -> R.string.armour_heavy
        ArmourProficiency.SHIELD -> R.string.armour_shield
        ArmourProficiency.INSTRUMENT -> R.string.armour_instrument
    }

@get:StringRes
val ItemCategory.labelRes: Int
    get() = when (this) {
        ItemCategory.WEAPON -> R.string.cat_weapon
        ItemCategory.HELMET -> R.string.cat_helmet
        ItemCategory.CLOAK -> R.string.cat_cloak
        ItemCategory.CLOTHING -> R.string.cat_clothing
        ItemCategory.LIGHT_ARMOUR -> R.string.cat_light_armour
        ItemCategory.MEDIUM_ARMOUR -> R.string.cat_medium_armour
        ItemCategory.HEAVY_ARMOUR -> R.string.cat_heavy_armour
        ItemCategory.SHIELD -> R.string.cat_shield
        ItemCategory.GLOVES -> R.string.cat_gloves
        ItemCategory.BOOTS -> R.string.cat_boots
        ItemCategory.AMULET -> R.string.cat_amulet
        ItemCategory.RING -> R.string.cat_ring
        ItemCategory.INSTRUMENT -> R.string.cat_instrument
        ItemCategory.CAMP_CLOTHING -> R.string.cat_camp_clothing
        ItemCategory.CAMP_SHOES -> R.string.cat_camp_shoes
        ItemCategory.UNDERWEAR -> R.string.cat_underwear
    }

@get:StringRes
val FeatureKind.labelRes: Int
    get() = when (this) {
        FeatureKind.PASSIVE -> R.string.kind_passive
        FeatureKind.ACTION -> R.string.kind_action
        FeatureKind.WEAPON_ACTION -> R.string.kind_weapon_action
    }

@get:StringRes
val LoreTheme.labelRes: Int
    get() = when (this) {
        LoreTheme.PANTHEON -> R.string.theme_pantheon
        LoreTheme.COSMOLOGY -> R.string.theme_cosmology
        LoreTheme.PEOPLES -> R.string.theme_peoples
        LoreTheme.BESTIARY -> R.string.theme_bestiary
        LoreTheme.FACTIONS -> R.string.theme_factions
        LoreTheme.PLACES -> R.string.theme_places
        LoreTheme.CHARACTERS -> R.string.theme_characters
        LoreTheme.LEGENDARY_ITEMS -> R.string.theme_legendary_items
        LoreTheme.TIMELINE -> R.string.theme_timeline
        LoreTheme.SAGA -> R.string.theme_saga
        LoreTheme.GAME_BOOKS -> R.string.theme_game_books
        LoreTheme.FURTHER_READING -> R.string.theme_further_reading
    }

@get:StringRes
val Edition.labelRes: Int
    get() = when (this) {
        Edition.ADND_1 -> R.string.edition_add1
        Edition.ADND_2 -> R.string.edition_add2
        Edition.THIRD -> R.string.edition_3e
        Edition.THIRD_POINT_FIVE -> R.string.edition_35
        Edition.FOURTH -> R.string.edition_4e
        Edition.FIFTH_2014 -> R.string.edition_5e_2014
        Edition.FIFTH_2024 -> R.string.edition_5e_2024
        Edition.MULTI -> R.string.edition_multi
    }

@get:StringRes
val CanonNature.labelRes: Int
    get() = when (this) {
        CanonNature.CANON_BG3 -> R.string.nature_canon_bg3
        CanonNature.CANON_DND -> R.string.nature_canon_dnd
        CanonNature.INTERPRETATION -> R.string.nature_interpretation
        CanonNature.REAL_WORLD -> R.string.nature_real_world
    }
