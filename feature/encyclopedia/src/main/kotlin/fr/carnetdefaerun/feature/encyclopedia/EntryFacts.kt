package fr.carnetdefaerun.feature.encyclopedia

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.data.content.EntryView
import fr.carnetdefaerun.core.designsystem.component.InfoLine
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SpoilerVeil
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.AbilityBonus
import fr.carnetdefaerun.core.model.content.ArmourClassBonus
import fr.carnetdefaerun.core.model.content.CheckAdvantage
import fr.carnetdefaerun.core.model.content.CriticalHitImmunity
import fr.carnetdefaerun.core.model.content.DamageResistance
import fr.carnetdefaerun.core.model.content.Effect
import fr.carnetdefaerun.core.model.content.Grants
import fr.carnetdefaerun.core.model.content.SaveAdvantage
import fr.carnetdefaerun.core.model.content.SavingThrowBonus
import fr.carnetdefaerun.core.model.content.SkillBonus
import fr.carnetdefaerun.core.model.content.WearerCondition
import fr.carnetdefaerun.core.model.content.WieldHand
import fr.carnetdefaerun.core.model.content.Area
import fr.carnetdefaerun.core.model.content.BackgroundFacts
import fr.carnetdefaerun.core.model.content.Block
import fr.carnetdefaerun.core.model.content.BlockKind
import fr.carnetdefaerun.core.model.content.ClassFacts
import fr.carnetdefaerun.core.model.content.ConditionFacts
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Cost
import fr.carnetdefaerun.core.model.content.Damage
import fr.carnetdefaerun.core.model.content.DamageModifier
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.FeatFacts
import fr.carnetdefaerun.core.model.content.FeatureFacts
import fr.carnetdefaerun.core.model.content.ItemFacts
import fr.carnetdefaerun.core.model.content.LevelFeature
import fr.carnetdefaerun.core.model.content.LoreFacts
import fr.carnetdefaerun.core.model.content.PlaceFacts
import fr.carnetdefaerun.core.model.content.Proficiencies
import fr.carnetdefaerun.core.model.content.RaceFacts
import fr.carnetdefaerun.core.model.content.Range
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.Rest
import fr.carnetdefaerun.core.model.content.SaveEffect
import fr.carnetdefaerun.core.model.content.SourceMethod
import fr.carnetdefaerun.core.model.content.SpellFacts
import fr.carnetdefaerun.core.model.content.SubclassFacts
import fr.carnetdefaerun.core.model.content.SubraceFacts
import fr.carnetdefaerun.core.model.spoiler.Gated
import fr.carnetdefaerun.core.model.spoiler.RevealKey
import fr.carnetdefaerun.core.ui.labelRes
import fr.carnetdefaerun.core.ui.untilRes
import fr.carnetdefaerun.core.ui.R as UiR
import java.text.NumberFormat
import java.util.Locale

/**
 * Faits structurés d'une fiche, en lignes « libellé — valeur ». Tout vient des faits déjà filtrés par le verrou ;
 * les noms des fiches citées viennent des liens visibles ([linkNames]) ou des noms portés par les faits.
 */
@Composable
internal fun EntryFacts(view: EntryView, linkNames: Map<ContentId, String>, onOpen: (ContentId, EntryType) -> Unit) {
    val lines: List<Pair<String, String>> = when (val f = view.facts) {
        is SpellFacts -> spellLines(f)
        is FeatureFacts -> featureLines(f)
        is ItemFacts -> itemLines(f)
        is ClassFacts -> classLines(f)
        is SubclassFacts -> proficiencyLines(f.proficiencies)
        is RaceFacts -> speedLine(f.speedMetres) + proficiencyLines(f.proficiencies)
        is SubraceFacts -> speedLine(f.speedMetres) + proficiencyLines(f.proficiencies)
        is BackgroundFacts -> listOfNotNull(joined(f.skills.map { stringResource(it.labelRes) })?.let { stringResource(R.string.fact_skills) to it })
        is FeatFacts -> featLines(f)
        is PlaceFacts -> listOfNotNull(f.act?.let { stringResource(R.string.fact_act) to stringResource(it.labelRes) })
        is ConditionFacts, is LoreFacts -> emptyList()
    }
    if (lines.isNotEmpty()) {
        SectionHeader(stringResource(R.string.entry_facts), Modifier.padding(top = 6.dp))
        Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter)) {
            for ((label, value) in lines) InfoLine(label, value)
        }
    }
    when (val f = view.facts) {
        is ClassFacts -> Progression(f.levels.map { it.level to it.features }, onOpen)
        is SubclassFacts -> Progression(f.levels.map { level -> level.level to (level.features + level.choices.flatMap { it.options }) }, onOpen)
        is RaceFacts -> Features(f.features, onOpen)
        is SubraceFacts -> Features(f.features, onOpen)
        is FeatFacts -> Features(f.features, onOpen)
        else -> Unit
    }
}

@Composable
private fun spellLines(f: SpellFacts): List<Pair<String, String>> = buildList {
    add(stringResource(R.string.fact_level) to if (f.level == 0) stringResource(R.string.fact_cantrip) else stringResource(R.string.fact_level_n, f.level))
    f.school?.let { add(stringResource(R.string.fact_school) to stringResource(it.labelRes)) }
    addAll(actionLines(f.costs, f.range, f.area, f.attackRoll, f.save?.let { s -> s to f.saveEffect }, f.damage, f.weaponDamage, f.durationTurns, f.durationUntil))
    if (f.upcastPerLevel.isNotEmpty()) add(stringResource(R.string.fact_upcast) to damages(f.upcastPerLevel))
    if (f.concentration) add(stringResource(R.string.fact_concentration) to stringResource(R.string.fact_yes))
    if (f.ritual) add(stringResource(R.string.fact_ritual) to stringResource(R.string.fact_yes))
    f.recharge?.let { add(stringResource(R.string.fact_recharge) to stringResource(it.labelRes)) }
}

@Composable
private fun featureLines(f: FeatureFacts): List<Pair<String, String>> = buildList {
    add(stringResource(R.string.fact_kind) to stringResource(f.kind.labelRes))
    addAll(actionLines(f.costs, f.range, f.area, f.attackRoll, f.save?.let { s -> s to f.saveEffect }, f.damage, f.weaponDamage, f.durationTurns, f.durationUntil))
    f.recharge?.let { add(stringResource(R.string.fact_recharge) to stringResource(it.labelRes)) }
}

@Composable
private fun actionLines(
    costs: List<Cost>,
    range: Range?,
    area: Area?,
    attackRoll: Boolean,
    save: Pair<Ability, SaveEffect?>?,
    damage: List<Damage>,
    weaponDamage: Boolean,
    durationTurns: Int?,
    durationUntil: Rest?,
): List<Pair<String, String>> = buildList {
    joined(costs.map { costLabel(it) })?.let { add(stringResource(R.string.fact_cost) to it) }
    range?.let { r -> add(stringResource(R.string.fact_range) to (r.metres?.let { stringResource(UiR.string.metres, number(it)) } ?: stringResource(r.kind.labelRes))) }
    area?.let { a -> add(stringResource(R.string.fact_area) to (a.metres?.let { m -> stringResource(R.string.fact_area_value, stringResource(a.shape.labelRes), stringResource(UiR.string.metres, number(m))) } ?: stringResource(a.shape.labelRes))) }
    if (attackRoll) add(stringResource(R.string.fact_attack) to stringResource(R.string.fact_yes))
    save?.let { (ability, effect) ->
        val name = stringResource(ability.labelRes)
        add(stringResource(R.string.fact_save) to (effect?.let { stringResource(R.string.fact_save_value, name, stringResource(it.labelRes)) } ?: name))
    }
    if (damage.isNotEmpty()) add(stringResource(R.string.fact_damage) to damages(damage))
    if (weaponDamage) add(stringResource(R.string.fact_damage) to stringResource(R.string.fact_weapon_damage))
    when {
        durationTurns != null -> add(stringResource(R.string.fact_duration) to pluralStringResource(R.plurals.fact_turns, durationTurns, durationTurns))
        durationUntil != null -> add(stringResource(R.string.fact_duration) to stringResource(durationUntil.untilRes))
    }
}

@Composable
private fun itemLines(f: ItemFacts): List<Pair<String, String>> = buildList {
    add(stringResource(R.string.fact_category) to stringResource(f.category.labelRes))
    f.rarity?.let { add(stringResource(R.string.fact_rarity) to stringResource(it.labelRes)) }
    f.enchantment?.takeIf { it != 0 }?.let { add(stringResource(R.string.fact_enchantment) to signed(it)) }
    f.weapon?.let { w ->
        add(stringResource(R.string.fact_weapon_class) to stringResource(if (w.martial) R.string.fact_martial else R.string.fact_simple))
        add(stringResource(R.string.fact_damage) to damages(w.damage))
        w.versatileDamage?.let { add(stringResource(R.string.fact_versatile) to damages(listOf(it))) }
        add(stringResource(R.string.fact_handedness) to stringResource(w.handedness.labelRes))
        joined(w.properties.map { stringResource(it.labelRes) })?.let { add(stringResource(R.string.fact_properties) to it) }
        w.rangeMetres?.let { add(stringResource(R.string.fact_range) to stringResource(UiR.string.metres, number(it))) }
    }
    f.armour?.let { a ->
        a.armourClass?.let { add(stringResource(R.string.fact_armour_class) to it.toString()) }
        a.armourClassBonus?.let { add(stringResource(R.string.fact_armour_class) to signed(it)) }
        a.proficiency?.let { add(stringResource(R.string.fact_proficiency) to stringResource(it.labelRes)) }
        if (a.stealthDisadvantage) add(stringResource(R.string.fact_stealth) to stringResource(R.string.fact_stealth_disadvantage))
    }
    f.weightKg?.let { add(stringResource(R.string.fact_weight) to stringResource(R.string.fact_weight_value, number(it))) }
    f.priceGold?.let { add(stringResource(R.string.fact_price) to stringResource(R.string.fact_price_value, it)) }
}

@Composable
private fun classLines(f: ClassFacts): List<Pair<String, String>> = buildList {
    add(stringResource(R.string.fact_hp_first) to stringResource(R.string.fact_hp_value, f.hitPointsAtFirstLevel))
    add(stringResource(R.string.fact_hp_level) to stringResource(R.string.fact_hp_value, f.hitPointsPerLevel))
    joined(f.savingThrows.map { stringResource(it.labelRes) })?.let { add(stringResource(R.string.fact_saves) to it) }
    // Une classe sans sorts n’a qu’une caractéristique pour les objets et parchemins (voir ClassFacts.spellcastingAbility).
    val caster = f.pactMagic || f.levels.any { it.spellSlots.isNotEmpty() || it.cantripsKnown != null }
    f.spellcastingAbility?.let { add(stringResource(if (caster) R.string.fact_spellcasting else R.string.fact_item_ability) to stringResource(it.labelRes)) }
    if (f.pactMagic) add(stringResource(R.string.fact_pact_magic) to stringResource(R.string.fact_yes))
    add(stringResource(R.string.fact_subclass_level) to stringResource(R.string.fact_level_n, f.subclassLevel))
    addAll(proficiencyLines(f.proficiencies))
}

@Composable
private fun featLines(f: FeatFacts): List<Pair<String, String>> = buildList {
    f.abilityIncrease?.let { inc ->
        val choices = joined(inc.choices.map { stringResource(it.labelRes) })
        val value = stringResource(R.string.fact_ability_increase_value, inc.amount)
        add(stringResource(R.string.fact_ability_increase) to (choices?.let { stringResource(R.string.fact_save_value, value, it) } ?: value))
    }
}

@Composable
private fun proficiencyLines(p: Proficiencies): List<Pair<String, String>> = buildList {
    joined(p.armour.map { stringResource(it.labelRes) })?.let { add(stringResource(R.string.fact_armour) to it) }
    val weapons = listOfNotNull(
        if (p.simpleWeapons) stringResource(R.string.fact_simple_weapons) else null,
        if (p.martialWeapons) stringResource(R.string.fact_martial_weapons) else null,
    )
    joined(weapons)?.let { add(stringResource(R.string.fact_weapons) to it) }
    joined(p.skills.map { stringResource(it.labelRes) })?.let { skills ->
        add((if (p.skillChoices > 0) pluralStringResource(R.plurals.fact_skill_choices, p.skillChoices, p.skillChoices) else stringResource(R.string.fact_skills)) to skills)
    }
}

@Composable
private fun speedLine(metres: Double?): List<Pair<String, String>> =
    listOfNotNull(metres?.let { stringResource(R.string.fact_speed) to stringResource(UiR.string.metres, number(it)) })

/** Capacités par niveau : chaque nom ouvre sa fiche quand elle existe. */
@Composable
private fun Progression(levels: List<Pair<Int, List<LevelFeature>>>, onOpen: (ContentId, EntryType) -> Unit) {
    val shown = levels.filter { it.second.isNotEmpty() }
    if (shown.isEmpty()) return
    SectionHeader(stringResource(R.string.entry_progression), Modifier.padding(top = 10.dp))
    for ((level, features) in shown) {
        Text(stringResource(R.string.fact_level_n, level), style = CarnetTheme.type.label, color = CarnetTheme.colors.gold, modifier = Modifier.padding(start = CarnetTheme.dimens.gutter, top = 10.dp))
        FeatureRows(features, onOpen)
    }
}

@Composable
private fun Features(features: List<LevelFeature>, onOpen: (ContentId, EntryType) -> Unit) {
    if (features.isEmpty()) return
    SectionHeader(stringResource(R.string.fact_features), Modifier.padding(top = 10.dp))
    FeatureRows(features, onOpen)
}

@Composable
private fun FeatureRows(features: List<LevelFeature>, onOpen: (ContentId, EntryType) -> Unit) {
    for (feature in features) {
        val id = feature.id
        if (id != null) ListRow(feature.name.fr, { onOpen(id, typeOf(id)) })
        else Text(feature.name.fr, style = CarnetTheme.type.reading, color = CarnetTheme.colors.text, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 6.dp))
    }
}

/** Sources d'obtention visibles d'un objet, puis un seul voile pour celles qui restent masquées. */
@Composable
internal fun ItemSources(view: EntryView, linkNames: Map<ContentId, String>, onReveal: (RevealKey) -> Unit) {
    val notes = view.blocks.filterIsInstance<Gated.Visible<Block>>().filter { it.value.kind == BlockKind.SOURCE }
    if (view.sources.isEmpty() && view.hiddenSources == null && notes.isEmpty()) return
    SectionHeader(stringResource(R.string.entry_sources), Modifier.padding(top = 10.dp))
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter)) {
        for (source in view.sources) {
            val where = listOfNotNull(source.act?.let { stringResource(it.labelRes) }, source.place?.let { linkNames[it] }, source.vendor)
            InfoLine(stringResource(source.method.labelRes), joined(where) ?: "")
        }
        for (note in notes) Text(note.value.text, style = CarnetTheme.type.reading, color = CarnetTheme.colors.text, modifier = Modifier.padding(vertical = 4.dp))
    }
    view.hiddenSources?.let { key -> SpoilerVeil({ onReveal(key) }, Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
}

private val SourceMethod.labelRes: Int
    get() = when (this) {
        SourceMethod.VENDOR -> R.string.source_vendor
        SourceMethod.CONTAINER -> R.string.source_container
        SourceMethod.GROUND -> R.string.source_ground
        SourceMethod.CARRIED -> R.string.source_carried
        SourceMethod.REWARD -> R.string.source_reward
        SourceMethod.UNKNOWN -> R.string.source_unknown
    }

@Composable
private fun costLabel(cost: Cost): String {
    val name = if (cost.resource == Resource.SPELL_SLOT && cost.level != null) stringResource(UiR.string.spell_slot_level, cost.level!!)
    else stringResource(cost.resource.labelRes)
    return if (cost.amount > 1) stringResource(R.string.fact_cost_amount, name, cost.amount) else name
}

@Composable
private fun damages(list: List<Damage>): String = joined(list.map { d ->
    val base = when {
        d.healing -> stringResource(UiR.string.healing_dice, d.dice)
        d.type != null -> stringResource(UiR.string.damage_dice, d.dice, stringResource(d.type!!.labelRes))
        else -> d.dice
    }
    when (d.modifier) {
        DamageModifier.SPELLCASTING -> stringResource(R.string.fact_plus_spellcasting, base)
        DamageModifier.FINESSE -> stringResource(R.string.fact_plus_finesse, base)
        null -> base
    }
}) ?: ""

@Composable
private fun joined(parts: List<String>): String? = parts.filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.list_separator))

private val FRENCH = Locale.FRANCE

private fun number(value: Double): String = NumberFormat.getNumberInstance(FRENCH).apply { maximumFractionDigits = 1 }.format(value)

private fun signed(value: Int): String = if (value >= 0) "+$value" else "−${-value}"

private fun typeOf(id: ContentId): EntryType = EntryType.entries.firstOrNull { it.name.lowercase() == id.kind } ?: EntryType.FEATURE

/** Une ligne d'effet structuré ; [target] : la fiche qu'elle cite, ouverte au toucher. */
internal data class EffectLine(val text: String, val target: ContentId? = null)

/** Effets structurés d'un objet, en phrases courtes ; les fiches citées sont nommées par leurs liens visibles. */
@Composable
internal fun itemEffectLines(effects: List<Effect>, linkNames: Map<ContentId, String>): List<EffectLine> = effects.mapNotNull { e ->
    when (e) {
        is AbilityBonus -> {
            val ability = stringResource(e.ability.labelRes)
            when {
                e.setTo != null -> EffectLine(stringResource(R.string.effect_ability_set, ability, e.setTo!!))
                e.delta != null -> EffectLine(stringResource(R.string.effect_bonus, ability, signed(e.delta!!)))
                else -> null
            }
        }
        is SkillBonus -> EffectLine(stringResource(R.string.effect_bonus, stringResource(e.skill.labelRes), signed(e.delta)))
        is ArmourClassBonus -> EffectLine(stringResource(R.string.effect_bonus, stringResource(R.string.fact_armour_class), signed(e.delta)))
        is SavingThrowBonus -> EffectLine(
            e.ability?.let { stringResource(R.string.effect_save_bonus_of, stringResource(it.labelRes), signed(e.delta)) }
                ?: stringResource(R.string.effect_save_bonus, signed(e.delta)),
        )
        is SaveAdvantage -> EffectLine(stringResource(R.string.effect_save_advantage, stringResource(e.ability.labelRes)))
        is CheckAdvantage -> (e.skill?.let { stringResource(it.labelRes) } ?: e.ability?.let { stringResource(it.labelRes) })
            ?.let { EffectLine(stringResource(R.string.effect_check_advantage, it)) }
        is DamageResistance -> EffectLine(stringResource(R.string.effect_resistance, stringResource(e.type.labelRes)))
        CriticalHitImmunity -> EffectLine(stringResource(R.string.effect_critical_immunity))
        is Grants -> linkNames[e.target]?.let { name ->
            val hand = when (e.wielded) {
                WieldHand.MAIN_HAND -> stringResource(R.string.effect_main_hand)
                WieldHand.OFF_HAND -> stringResource(R.string.effect_off_hand)
                null -> null
            }
            val base = stringResource(R.string.effect_grants, name)
            val withHand = hand?.let { stringResource(R.string.subtitle_join, base, it) } ?: base
            EffectLine(e.recharge?.let { stringResource(R.string.subtitle_join, withHand, stringResource(it.labelRes)) } ?: withHand, e.target)
        }
        is WearerCondition -> linkNames[e.condition]?.let { EffectLine(stringResource(R.string.effect_wearer_condition, it), e.condition) }
    }
}

