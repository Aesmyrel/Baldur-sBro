package fr.carnetdefaerun.feature.tools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.carnetdefaerun.core.designsystem.component.CarnetSwitchRow
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ChoiceTag
import fr.carnetdefaerun.core.designsystem.component.OrnateCard
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SegmentedChoice
import fr.carnetdefaerun.core.designsystem.component.Stepper
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.rules.BonusDie
import fr.carnetdefaerun.core.rules.MAX_CHARACTER_LEVEL
import fr.carnetdefaerun.core.rules.RollKind
import fr.carnetdefaerun.core.rules.RollMode

/** L'entrée se garde à la rotation et au retour dans l'onglet. */
private val InputSaver = Saver<ProbabilityInput, List<Any>>(
    save = { listOf(it.kind.name, it.modifier, it.target, it.mode.name, it.bonusDice.map(BonusDie::name), it.bardLevel, it.halflingLuck, it.improvedCriticals, it.autoSaves, it.inspirationRerolls) },
    restore = { v ->
        @Suppress("UNCHECKED_CAST")
        ProbabilityInput(
            RollKind.valueOf(v[0] as String), v[1] as Int, v[2] as Int, RollMode.valueOf(v[3] as String),
            (v[4] as List<String>).map(BonusDie::valueOf).toSet(), v[5] as Int, v[6] as Boolean, v[7] as Int, v[8] as Boolean, v[9] as Int,
        )
    },
)

@Composable
fun ProbabilityRoute(onBack: () -> Unit) {
    var input by rememberSaveable(stateSaver = InputSaver) { mutableStateOf(ProbabilityInput()) }
    ProbabilityScreen(input, { input = it }, onBack)
}

@Composable
fun ProbabilityScreen(input: ProbabilityInput, onChange: (ProbabilityInput) -> Unit, onBack: () -> Unit) {
    val c = CarnetTheme.colors
    val gutter = CarnetTheme.dimens.gutter
    val result = input.result()
    CarnetSectionScope(CarnetSection.OUTILS) {
        Column(Modifier.fillMaxSize()) {
            CarnetTopBar(stringResource(R.string.tools_crumb), onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier.fillMaxHeight().widthIn(max = CarnetTheme.dimens.readingMaxWidth).fillMaxWidth().wrapContentWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(R.string.proba_title), style = CarnetTheme.type.headline, color = c.text,
                        modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp).semantics { heading() })

                    ResultCard(input, result, Modifier.padding(horizontal = gutter, vertical = 10.dp))

                    val kinds = listOf(RollKind.ATTACK, RollKind.ABILITY_CHECK, RollKind.SAVING_THROW)
                    SegmentedChoice(
                        listOf(stringResource(R.string.proba_kind_attack), stringResource(R.string.proba_kind_check), stringResource(R.string.proba_kind_save)),
                        kinds.indexOf(input.kind), { onChange(input.copy(kind = kinds[it])) }, Modifier.padding(horizontal = gutter, vertical = 8.dp),
                    )
                    Column(Modifier.padding(horizontal = gutter)) {
                        val modLabel = stringResource(R.string.proba_modifier)
                        Stepper(modLabel, input.modifier, { onChange(input.copy(modifier = it)) }, -5..20,
                            stringResource(R.string.proba_decrease, modLabel), stringResource(R.string.proba_increase, modLabel), valueText = signed(input.modifier))
                        val targetLabel = stringResource(if (input.kind == RollKind.ATTACK) R.string.proba_target_ac else R.string.proba_target_dc)
                        Stepper(targetLabel, input.target, { onChange(input.copy(target = it)) }, 1..35,
                            stringResource(R.string.proba_decrease, targetLabel), stringResource(R.string.proba_increase, targetLabel))
                    }
                    val modes = RollMode.entries
                    SegmentedChoice(
                        listOf(stringResource(R.string.proba_mode_normal), stringResource(R.string.proba_mode_advantage), stringResource(R.string.proba_mode_disadvantage)),
                        modes.indexOf(input.mode), { onChange(input.copy(mode = modes[it])) }, Modifier.padding(horizontal = gutter, vertical = 8.dp),
                    )

                    SectionHeader(stringResource(R.string.proba_dice))
                    TagRow(Modifier.padding(horizontal = gutter, vertical = 6.dp)) {
                        for (die in BonusDie.entries.filter { input.kind in it.kinds }) {
                            val label = when (die) {
                                BonusDie.GUIDANCE -> stringResource(R.string.proba_guidance)
                                BonusDie.BLESS -> stringResource(R.string.proba_bless)
                                BonusDie.BARDIC_INSPIRATION -> stringResource(R.string.proba_bardic, die.sides(input.bardLevel))
                            }
                            ChoiceTag(label, die in input.bonusDice, { on -> onChange(input.copy(bonusDice = if (on) input.bonusDice + die else input.bonusDice - die)) })
                        }
                    }
                    if (BonusDie.BARDIC_INSPIRATION in input.appliedDice) {
                        val bardLabel = stringResource(R.string.proba_bard_level)
                        Stepper(bardLabel, input.bardLevel, { onChange(input.copy(bardLevel = it)) }, 1..MAX_CHARACTER_LEVEL,
                            stringResource(R.string.proba_decrease, bardLabel), stringResource(R.string.proba_increase, bardLabel), Modifier.padding(horizontal = gutter))
                    }

                    SectionHeader(stringResource(R.string.proba_options))
                    // Les interrupteurs ont leur propre marge : pas de second retrait.
                    CarnetSwitchRow(stringResource(R.string.proba_halfling), stringResource(R.string.proba_halfling_sub), input.halflingLuck, { onChange(input.copy(halflingLuck = it)) })
                    if (input.kind == RollKind.SAVING_THROW) {
                        CarnetSwitchRow(stringResource(R.string.proba_auto_saves), stringResource(R.string.proba_auto_saves_sub), input.autoSaves, { onChange(input.copy(autoSaves = it)) })
                    }
                    Column(Modifier.padding(horizontal = gutter)) {
                        if (input.kind == RollKind.ATTACK) {
                            val critLabel = stringResource(R.string.proba_crit)
                            Stepper(critLabel, input.improvedCriticals, { onChange(input.copy(improvedCriticals = it)) }, 0..3,
                                stringResource(R.string.proba_decrease, critLabel), stringResource(R.string.proba_increase, critLabel))
                        }
                        val rerollLabel = stringResource(R.string.proba_rerolls)
                        Stepper(rerollLabel, input.inspirationRerolls, { onChange(input.copy(inspirationRerolls = it)) }, 0..4,
                            stringResource(R.string.proba_decrease, rerollLabel), stringResource(R.string.proba_increase, rerollLabel))
                    }
                    Text(stringResource(R.string.proba_karmic), style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 10.dp))
                    Text(stringResource(R.string.proba_sources), style = CarnetTheme.type.readingItalic.copy(fontSize = CarnetTheme.type.bodySmall.fontSize), color = c.text2,
                        modifier = Modifier.padding(horizontal = gutter))
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Le résultat en tête : grand pourcentage d'or, coup critique, ce qu'il faut au dé. Annoncé à chaque changement. */
@Composable
private fun ResultCard(input: ProbabilityInput, result: ProbabilityResult, modifier: Modifier = Modifier) {
    val c = CarnetTheme.colors
    val value = percentText(percent(result.success, result.failure))
    val a11y = stringResource(R.string.proba_result_a11y, value)
    OrnateCard(modifier, borderColor = c.goldDeep) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.proba_result_label), style = CarnetTheme.type.label, color = c.text2, modifier = Modifier.weight(1f).padding(bottom = 10.dp))
            Text(
                stringResource(R.string.proba_percent, value), style = CarnetTheme.type.numericLarge, color = c.gold,
                modifier = Modifier.clearAndSetSemantics { contentDescription = a11y; liveRegion = LiveRegionMode.Polite },
            )
        }
        result.critical?.let { Text(stringResource(R.string.proba_critical, percentText(percent(it))), style = CarnetTheme.type.data, color = c.text) }
        val needed = when (val n = result.needed) {
            is Needed.Face -> stringResource(R.string.proba_needed_face, n.face)
            Needed.AllButOne -> stringResource(R.string.proba_needed_all_but_one)
            is Needed.OnlyNatural -> if (n.face == 20) stringResource(R.string.proba_needed_natural, 20) else stringResource(R.string.proba_needed_natural_or_more, n.face)
            Needed.Always -> stringResource(R.string.proba_needed_always)
            Needed.Never -> stringResource(R.string.proba_needed_never)
        }
        Text(needed, style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(top = 4.dp))
        if (input.appliedDice.isNotEmpty()) Text(stringResource(R.string.proba_needed_dice_note), style = CarnetTheme.type.bodySmall, color = c.text2)
    }
}

/** Pourcentage avec sa borne éventuelle (« moins de 0,1 »), sans le signe %. */
@Composable
private fun percentText(p: Percent): String = when (p.bound) {
    Percent.Bound.EXACT -> p.value
    Percent.Bound.BELOW -> stringResource(R.string.proba_below, p.value)
    Percent.Bound.ABOVE -> stringResource(R.string.proba_above, p.value)
}

/** Nombre signé avec le vrai signe moins. */
private fun signed(value: Int): String = if (value >= 0) "+$value" else "\u2212${-value}"
