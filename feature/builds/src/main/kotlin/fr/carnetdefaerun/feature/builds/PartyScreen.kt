package fr.carnetdefaerun.feature.builds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.carnetdefaerun.core.data.builds.BuildRepository
import fr.carnetdefaerun.core.data.builds.PartyRepository
import fr.carnetdefaerun.core.data.builds.RulesRepository
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.data.preferences.UserPreferencesRepository
import fr.carnetdefaerun.core.designsystem.component.CarnetSheet
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ChoiceTag
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.component.TextAction
import fr.carnetdefaerun.core.designsystem.illustration.drawClassEmblem
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.rules.CharacterSheet
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import fr.carnetdefaerun.core.rules.DifficultyRules
import fr.carnetdefaerun.core.ui.labelRes
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Meilleur bonus du groupe pour un jet : qui (place dans le groupe), combien, s'il le maîtrise lui-même, et si au
 * moins un membre le maîtrise ([anyProficient]) : un membre non maîtrisé peut avoir le meilleur bonus.
 */
data class Best(val slot: Int, val bonus: Int, val proficient: Boolean, val anyProficient: Boolean = proficient)

/** Ce que le groupe couvre : pour chaque sauvegarde et compétence, le meilleur membre ; null sans membre. */
data class PartyCoverage(val saves: Map<Ability, Best?>, val skills: Map<Skill, Best?>)

/**
 * Couverture du groupe, en fonction pure. Le meilleur est le plus haut bonus ; à égalité, celui qui maîtrise le jet,
 * puis le premier emplacement. Aucun rôle n'est attribué : BG3 n'en définit pas, on montre les faits.
 */
fun partyCoverage(slots: List<CharacterSheet?>): PartyCoverage {
    fun <K> best(keys: List<K>, bonus: (CharacterSheet, K) -> Int, mastered: (CharacterSheet, K) -> Boolean): Map<K, Best?> =
        keys.associateWith { key ->
            val candidates = slots.mapIndexedNotNull { i, sheet -> sheet?.let { Best(i, bonus(it, key), mastered(it, key)) } }
            val anyone = candidates.any { it.proficient }
            candidates.sortedWith(compareByDescending<Best> { it.bonus }.thenByDescending { it.proficient }.thenBy { it.slot })
                .firstOrNull()?.copy(anyProficient = anyone)
        }
    return PartyCoverage(
        saves = best(Ability.entries, { s, a -> s.savingThrowBonuses.getValue(a) }, { s, a -> a in s.savingThrows }),
        skills = best(Skill.entries, { s, k -> s.skillBonuses.getValue(k) }, { s, k -> k in s.skillProficiencies }),
    )
}

data class PartyUiState(val slots: List<BuildItem?>, val builds: List<BuildItem>, val catalog: Catalog) {
    val coverage: PartyCoverage by lazy { partyCoverage(slots.map { it?.sheet }) }
}

@HiltViewModel
class PartyViewModel @Inject constructor(
    private val party: PartyRepository,
    repository: BuildRepository,
    rules: RulesRepository,
    gateway: ContentGateway,
    preferences: UserPreferencesRepository,
) : ViewModel() {
    val state: StateFlow<PartyUiState?> = combine(
        party.slots, repository.builds, gateway.catalog(), flow { emit(rules.data()) }, preferences.preferences.map { DifficultyRules.of(it.difficulty) },
    ) { slots, builds, catalog, data, difficulty ->
        val items = builds.filterNot { it.damaged }.map { BuildItem(it.id, it.build, CharacterSheetCalculator.compute(it.build, data, difficulty)) }
        val byId = items.associateBy { it.id }
        PartyUiState(slots.map { id -> id?.let(byId::get) }, items, catalog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun set(slot: Int, buildId: Long?) = viewModelScope.launch { party.set(slot, buildId) }
}

@Composable
fun PartyRoute(onBack: () -> Unit) {
    val viewModel = hiltViewModel<PartyViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    PartyScreen(state, onSet = viewModel::set, onBack = onBack)
}

@Composable
fun PartyScreen(state: PartyUiState?, onSet: (Int, Long?) -> Unit, onBack: () -> Unit, initialSlot: Int? = null) {
    val c = CarnetTheme.colors
    var choosing by remember { mutableStateOf(initialSlot) }
    CarnetSectionScope(CarnetSection.BUILDS) {
        Box(Modifier.fillMaxSize()) {
            // Feuille ouverte : le contenu dessous sort de l'arbre d'accessibilité.
            Column(Modifier.fillMaxSize().then(if (choosing != null) Modifier.clearAndSetSemantics {} else Modifier)) {
                CarnetTopBar(stringResource(R.string.builds_crumb), onBack = onBack)
                if (state != null) LazyColumn(Modifier.capped()) {
                    item(key = "title") {
                        Text(stringResource(R.string.party_title), style = CarnetTheme.type.headline, color = c.text,
                            modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp).semantics { heading() })
                    }
                    item(key = "intro") {
                        Text(stringResource(R.string.party_intro), style = CarnetTheme.type.reading, color = c.text2, modifier = Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp))
                    }
                    items(state.slots.indices.toList(), key = { "slot-$it" }) { i -> SlotRow(i, state.slots[i], state.catalog) { choosing = i } }
                    if (state.slots.any { it != null }) {
                        item(key = "saves-header") { SectionHeader(stringResource(R.string.party_saves)) }
                        item(key = "saves") {
                            CoverageRows(Ability.entries.map { stringResource(it.labelRes) to state.coverage.saves[it] }, state.slots)
                        }
                        item(key = "skills-header") { SectionHeader(stringResource(R.string.section_skills)) }
                        item(key = "skills") {
                            val rows = Skill.entries.map { stringResource(it.labelRes) to state.coverage.skills[it] }.sortedBy { Folding.fold(it.first) }
                            CoverageRows(rows, state.slots)
                        }
                        item(key = "casting-header") { SectionHeader(stringResource(R.string.party_casting)) }
                        item(key = "casting") { Casting(state) }
                    }
                    item(key = "end") { Spacer(Modifier.height(32.dp)) }
                }
            }
            val slot = choosing
            if (state != null) CarnetSheet(
                visible = slot != null,
                onDismiss = { choosing = null },
                title = stringResource(R.string.party_slot, (slot ?: 0) + 1),
                closeLabel = stringResource(R.string.sheet_close),
            ) {
                if (slot != null) {
                    Text(stringResource(R.string.party_slot, slot + 1), style = CarnetTheme.type.headline, color = c.text, modifier = Modifier.padding(vertical = 6.dp).semantics { heading() })
                    if (state.builds.isEmpty()) Text(stringResource(R.string.party_no_builds), style = CarnetTheme.type.bodySmall, color = c.text2)
                    TagRow(Modifier.padding(vertical = 6.dp)) {
                        for (item in state.builds) ChoiceTag(item.build.name, state.slots[slot]?.id == item.id, { on -> onSet(slot, if (on) item.id else null); choosing = null })
                    }
                    if (state.slots[slot] != null) TextAction(stringResource(R.string.party_free), { onSet(slot, null); choosing = null })
                    TextAction(stringResource(R.string.sheet_close), { choosing = null })
                }
            }
        }
    }
}

@Composable
private fun SlotRow(index: Int, item: BuildItem?, catalog: Catalog, onClick: () -> Unit) {
    val c = CarnetTheme.colors
    if (item == null) {
        ListRow(stringResource(R.string.party_slot_free, index + 1), onClick, subtitle = stringResource(R.string.party_slot_choose))
        return
    }
    val main = item.sheet.classLevels.maxByOrNull { it.value }?.key
    ListRow(stringResource(R.string.party_slot_named, index + 1, item.build.name), onClick, subtitle = summaryLine(item.build, item.sheet, catalog)) {
        val emblem = main?.let(::emblemOf)
        if (emblem != null) {
            val pigment = pigmentOf(main, c)
            Canvas(Modifier.size(28.dp)) { drawClassEmblem(emblem, pigment, strokeWidth = 1.4f) }
        }
    }
}

/** Une ligne par jet : le meilleur membre et son bonus, ou l'avertissement que personne ne le maîtrise. */
@Composable
private fun CoverageRows(rows: List<Pair<String, Best?>>, slots: List<BuildItem?>) {
    val c = CarnetTheme.colors
    val nobody = stringResource(R.string.party_nobody)
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 6.dp)) {
        for ((label, best) in rows) {
            if (best == null) continue
            val who = slots[best.slot]?.build?.name.orEmpty()
            val bonus = signed(best.bonus)
            val description = if (best.anyProficient) stringResource(R.string.party_best_a11y, label, bonus, who)
            else stringResource(R.string.party_unmastered_a11y, label, bonus, who)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 36.dp).clearAndSetSemantics { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = CarnetTheme.type.bodySmall, color = c.text)
                    Text(if (best.anyProficient) who else nobody, style = CarnetTheme.type.dataSmall, color = if (best.anyProficient) c.text2 else c.vermilion,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(bonus, style = CarnetTheme.type.dataStrong, color = if (best.proficient) c.gold else c.text2)
            }
        }
    }
}

@Composable
private fun Casting(state: PartyUiState) {
    val c = CarnetTheme.colors
    val hidden = stringResource(R.string.hidden_choice)
    val lines = state.slots.filterNotNull().flatMap { item ->
        item.sheet.spellcasting.map { s ->
            stringResource(R.string.party_casting_line, item.build.name, state.catalog.name(s.classId) ?: hidden, stringResource(s.ability.labelRes), s.saveDc)
        }
    }
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (lines.isEmpty()) Text(stringResource(R.string.party_no_casting), style = CarnetTheme.type.bodySmall, color = c.vermilion)
        for (line in lines) Text(line, style = CarnetTheme.type.bodySmall, color = c.text)
    }
}
