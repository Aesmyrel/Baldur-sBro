package fr.carnetdefaerun.feature.builds

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.carnetdefaerun.core.designsystem.component.AbilityTile
import fr.carnetdefaerun.core.designsystem.component.CalcRow
import fr.carnetdefaerun.core.designsystem.component.CarnetSheet
import fr.carnetdefaerun.core.designsystem.component.CarnetTopBar
import fr.carnetdefaerun.core.designsystem.component.ChoiceTag
import fr.carnetdefaerun.core.designsystem.component.GoldButton
import fr.carnetdefaerun.core.designsystem.component.IconSlot
import fr.carnetdefaerun.core.designsystem.component.LevelMarker
import fr.carnetdefaerun.core.designsystem.component.LevelPip
import fr.carnetdefaerun.core.designsystem.component.LevelTrack
import fr.carnetdefaerun.core.designsystem.component.ListRow
import fr.carnetdefaerun.core.designsystem.component.QuietButton
import fr.carnetdefaerun.core.designsystem.component.SectionHeader
import fr.carnetdefaerun.core.designsystem.component.SpellSlots
import fr.carnetdefaerun.core.designsystem.component.StatTile
import fr.carnetdefaerun.core.designsystem.component.TagRow
import fr.carnetdefaerun.core.designsystem.component.TextAction
import fr.carnetdefaerun.core.designsystem.component.TextInput
import fr.carnetdefaerun.core.designsystem.draw.sparklePath
import fr.carnetdefaerun.core.designsystem.illustration.ClassEmblem
import fr.carnetdefaerun.core.designsystem.illustration.drawClassEmblem
import fr.carnetdefaerun.core.designsystem.theme.CarnetSection
import fr.carnetdefaerun.core.designsystem.theme.CarnetSectionScope
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.settings.Difficulty
import fr.carnetdefaerun.core.model.search.Folding
import fr.carnetdefaerun.core.rules.CharacterSheet
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import fr.carnetdefaerun.core.rules.MAX_CHARACTER_LEVEL
import fr.carnetdefaerun.core.rules.PointBuy
import fr.carnetdefaerun.core.rules.RuleSources
import fr.carnetdefaerun.core.rules.SheetProblem
import fr.carnetdefaerun.core.rules.Term
import fr.carnetdefaerun.core.rules.UnarmouredFormula
import fr.carnetdefaerun.core.ui.labelRes

/** Ce que la feuille montante affiche. */
sealed interface Panel {
    data class AbilityPanel(val ability: Ability) : Panel
    data object HitPoints : Panel
    data object ArmourClass : Panel
    data object SpellDc : Panel
    data object SpellAttack : Panel
    data class Level(val index: Int) : Panel
    data object AddLevel : Panel
    data object Race : Panel
    data object Subrace : Panel
    data object Background : Panel
    data object Share : Panel
    data object Delete : Panel
    data class Skills(val sourceId: String) : Panel
}

@Composable
fun BuildEditorRoute(id: Long, onBack: () -> Unit) {
    val viewModel = hiltViewModel<BuildEditorViewModel, BuildEditorViewModel.Factory>(key = "build-$id", creationCallback = { it.create(id) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_title)
    BuildEditorScreen(
        state = state,
        onEdit = viewModel::edit,
        onBack = onBack,
        onDelete = { viewModel.delete(onBack) },
        onSend = { code ->
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, code)
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
    )
}

@Composable
fun BuildEditorScreen(
    state: EditorUiState,
    onEdit: (BuildEdit) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit = {},
    onSend: (String) -> Unit = {},
    initialPanel: Panel? = null,
) {
    val c = CarnetTheme.colors
    var panel by remember { mutableStateOf(initialPanel) }
    CarnetSectionScope(CarnetSection.BUILDS) {
        Box(Modifier.fillMaxSize()) {
            when (state) {
                EditorUiState.Loading -> Column(Modifier.fillMaxSize()) { CarnetTopBar(stringResource(R.string.builds_crumb), onBack = onBack) }
                EditorUiState.Missing -> Column(Modifier.fillMaxSize()) {
                    CarnetTopBar(stringResource(R.string.builds_crumb), onBack = onBack)
                    Text(stringResource(R.string.editor_missing), style = CarnetTheme.type.readingItalic, color = c.text2, modifier = Modifier.padding(CarnetTheme.dimens.gutter))
                }
                is EditorUiState.Ready -> {
                    EditorContent(state, onEdit, onBack, open = { panel = it })
                    val current = panel
                    CarnetSheet(
                        visible = current != null,
                        onDismiss = { panel = null },
                        title = current?.let { panelTitle(it, state) }.orEmpty(),
                        closeLabel = stringResource(R.string.sheet_close),
                    ) {
                        if (current != null) PanelContent(current, state, onEdit, close = { panel = null }, onDelete = onDelete, onSend = onSend)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorContent(state: EditorUiState.Ready, onEdit: (BuildEdit) -> Unit, onBack: () -> Unit, open: (Panel) -> Unit) {
    val c = CarnetTheme.colors
    val build = state.build
    val sheet = state.sheet
    val gutter = CarnetTheme.dimens.gutter
    Column(Modifier.fillMaxSize()) {
        CarnetTopBar(stringResource(R.string.builds_crumb_position, state.position, state.total), onBack = onBack) {
            IconSlot({ open(Panel.Share) }, stringResource(R.string.editor_share)) { color -> ShareGlyph(color) }
        }
        LazyColumn(Modifier.capped()) {
            item(key = "title") { TitleBlock(build, sheet, state.catalog, onEdit) }
            if (build.levels.isNotEmpty()) item(key = "track") { Track(state) }
            val problems = sheet.problems - SheetProblem.NO_LEVEL
            if (problems.isNotEmpty()) item(key = "problems") { Problems(problems) }

            item(key = "abilities-header") {
                SectionHeader(
                    stringResource(R.string.section_abilities),
                    actionLabel = stringResource(R.string.points_spent, PointBuy.BUDGET - sheet.pointBuyRemaining, PointBuy.BUDGET),
                )
            }
            item(key = "abilities") { Abilities(build, sheet, open) }

            item(key = "stats-header") { SectionHeader(stringResource(R.string.section_stats)) }
            item(key = "stats") { Stats(sheet, open) }
            if (state.difficulty == Difficulty.EXPLORER || state.difficulty == Difficulty.CUSTOM) item(key = "difficulty") {
                Text(
                    stringResource(if (state.difficulty == Difficulty.EXPLORER) R.string.difficulty_explorer_note else R.string.difficulty_custom_note),
                    style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp),
                )
            }
            item(key = "saves") {
                val names = Ability.entries.filter { it in sheet.savingThrows }.map { stringResource(it.labelRes) }
                Text(
                    if (names.isEmpty()) stringResource(R.string.stat_saves_none) else stringResource(R.string.stat_saves, names.joinToString(stringResource(R.string.list_separator))),
                    style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(horizontal = gutter, vertical = 6.dp),
                )
            }
            item(key = "slots") { Slots(sheet, state.catalog) }

            item(key = "skills-header") { SectionHeader(stringResource(R.string.section_skills)) }
            item(key = "skill-sources") { SkillSources(state, open) }
            item(key = "skills") { SkillsGrid(sheet) }

            item(key = "levels-header") { SectionHeader(stringResource(R.string.section_levels)) }
            items(state.rows, key = { "level-${it.index}" }) { row -> LevelListRow(row, state) { open(Panel.Level(row.index)) } }
            item(key = "levels-actions") {
                Row(Modifier.padding(horizontal = gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (build.levels.size < MAX_CHARACTER_LEVEL) GoldButton(stringResource(R.string.level_add), { open(Panel.AddLevel) })
                    else Text(stringResource(R.string.level_max), style = CarnetTheme.type.bodySmall, color = c.text2)
                    if (build.levels.isNotEmpty()) QuietButton(stringResource(R.string.level_remove_last), { onEdit(BuildEdit.RemoveLastLevel) })
                }
            }

            item(key = "origin-header") { SectionHeader(stringResource(R.string.section_origin)) }
            item(key = "origin") { Origin(state, open) }
            item(key = "delete") {
                Box(Modifier.padding(horizontal = gutter, vertical = 16.dp)) { TextAction(stringResource(R.string.editor_delete), { open(Panel.Delete) }) }
            }
            item(key = "end") { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun TitleBlock(build: Build, sheet: CharacterSheet, catalog: Catalog, onEdit: (BuildEdit) -> Unit) {
    val c = CarnetTheme.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 4.dp)) {
        if (!editing) {
            Text(build.name, style = CarnetTheme.type.display, color = c.text, modifier = Modifier.semantics { heading() })
            Text(summaryLine(build, sheet, catalog), style = CarnetTheme.type.data, color = c.text2, modifier = Modifier.padding(top = 4.dp))
            TextAction(stringResource(R.string.editor_rename), { draft = build.name; editing = true })
        } else {
            TextInput(draft, { draft = it.take(BuildCode.MAX_NAME_LENGTH) }, stringResource(R.string.editor_rename_hint))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldButton(stringResource(R.string.editor_save), { onEdit(BuildEdit.Rename(draft)); editing = false }, enabled = draft.isNotBlank())
                QuietButton(stringResource(R.string.builds_cancel), { editing = false })
            }
        }
    }
}

@Composable
private fun Track(state: EditorUiState.Ready) {
    val c = CarnetTheme.colors
    val hidden = stringResource(R.string.hidden_choice)
    val pips = state.rows.map { row ->
        val classId = row.level.classId.value
        val className = state.catalog.name(classId) ?: hidden
        val base = stringResource(R.string.level_pip, row.index + 1, stringResource(R.string.summary_class_level, className, row.classLevel))
        val marker = when {
            row.subclassHere -> LevelMarker.SOUS_CLASSE
            row.featHere -> LevelMarker.DON
            else -> null
        }
        val description = when (marker) {
            LevelMarker.SOUS_CLASSE -> stringResource(R.string.level_pip_subclass, base)
            LevelMarker.DON -> stringResource(R.string.level_pip_feat, base)
            null -> base
        }
        LevelPip(emblemOf(classId) ?: ClassEmblem.GUERRIER, if (emblemOf(classId) != null) pigmentOf(classId, c) else c.muted, marker, description)
    }
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        LevelTrack(pips, total = MAX_CHARACTER_LEVEL)
        Row(Modifier.padding(top = 2.dp, start = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(LevelMarker.SOUS_CLASSE, stringResource(R.string.legend_subclass))
            Legend(LevelMarker.DON, stringResource(R.string.legend_feat))
        }
    }
}

@Composable
private fun Legend(marker: LevelMarker, label: String) {
    val c = CarnetTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp, 14.dp)) {
            when (marker) {
                LevelMarker.SOUS_CLASSE -> drawPath(sparklePath(size.width / 2, size.height / 2, size.width / 2), c.gold)
                LevelMarker.DON -> drawPath(Path().apply {
                    moveTo(size.width / 2, size.height * 0.15f); lineTo(size.width * 0.8f, size.height / 2)
                    lineTo(size.width / 2, size.height * 0.85f); lineTo(size.width * 0.2f, size.height / 2); close()
                }, c.gold)
            }
        }
        Text(label, style = CarnetTheme.type.dataSmall, color = c.text2)
    }
}

@Composable
private fun Problems(problems: Set<SheetProblem>) {
    val c = CarnetTheme.colors
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (p in problems.sortedBy { !it.blocking }) {
            Text(stringResource(p.labelRes), style = CarnetTheme.type.bodySmall, color = if (p.blocking) c.vermilion else c.text2)
        }
    }
}

private val SheetProblem.labelRes: Int
    get() = when (this) {
        SheetProblem.NO_LEVEL -> R.string.problem_no_level
        SheetProblem.TOO_MANY_LEVELS -> R.string.problem_too_many_levels
        SheetProblem.UNKNOWN_CLASS -> R.string.problem_unknown_class
        SheetProblem.POINT_BUY_OUT_OF_RANGE -> R.string.problem_point_buy_range
        SheetProblem.POINT_BUY_OVER_BUDGET -> R.string.problem_point_buy_budget
        SheetProblem.BONUSES_MISSING -> R.string.problem_bonuses_missing
        SheetProblem.BONUSES_SAME_ABILITY -> R.string.problem_bonuses_same
        SheetProblem.FEAT_MISSING -> R.string.problem_feat_missing
        SheetProblem.FEAT_NOT_ALLOWED -> R.string.problem_feat_not_allowed
        SheetProblem.FEAT_INCREASE_INVALID -> R.string.problem_feat_increase
        SheetProblem.SUBCLASS_MISSING -> R.string.problem_subclass_missing
        SheetProblem.SUBCLASS_INVALID -> R.string.problem_subclass_invalid
        SheetProblem.SKILLS_MISSING -> R.string.problem_skills_missing
        SheetProblem.SKILLS_INVALID -> R.string.problem_skills_invalid
        SheetProblem.MULTICLASS_DISABLED -> R.string.problem_multiclass_disabled
    }

/** Abréviation à trois lettres des tuiles (For, Dex, Con…), tirée du nom de la caractéristique. */
@Composable
private fun abbreviation(ability: Ability): String = stringResource(ability.labelRes).take(3)

@Composable
private fun Abilities(build: Build, sheet: CharacterSheet, open: (Panel) -> Unit) {
    BoxWithConstraints(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        val scale = LocalDensity.current.fontScale.coerceAtMost(1.3f)
        val columns = if (maxWidth / 6 >= 52.dp * scale) 6 else 3
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rowAbilities in Ability.entries.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (a in rowAbilities) {
                        val value = sheet.scores.getValue(a)
                        val mod = signed(sheet.modifiers.getValue(a))
                        AbilityTile(
                            key = abbreviation(a),
                            value = value,
                            modifierText = mod,
                            highlighted = a == build.majorBonus,
                            a11y = stringResource(R.string.ability_a11y, stringResource(a.labelRes), value, mod),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(CarnetTheme.dimens.radius))
                                .clickable(role = Role.Button) { open(Panel.AbilityPanel(a)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stats(sheet: CharacterSheet, open: (Panel) -> Unit) {
    val none = stringResource(R.string.stat_none)
    val dc = sheet.spellcasting.map { it.saveDc }.distinct()
    val attack = sheet.spellcasting.map { it.attackBonus }.distinct()
    val tiles = listOf(
        Triple(stringResource(R.string.stat_hp), if (sheet.level > 0) sheet.hitPoints.total.toString() else none, Panel.HitPoints),
        Triple(stringResource(R.string.stat_ac), sheet.armourClass.total.toString(), Panel.ArmourClass),
        Triple(stringResource(R.string.stat_prof), if (sheet.level > 0) signed(sheet.proficiency) else none, null),
        Triple(stringResource(R.string.stat_dc), if (dc.isEmpty()) none else dc.joinToString(" / "), Panel.SpellDc.takeIf { dc.isNotEmpty() }),
        Triple(stringResource(R.string.stat_attack), if (attack.isEmpty()) none else attack.joinToString(" / ") { signed(it) }, Panel.SpellAttack.takeIf { attack.isNotEmpty() }),
        Triple(stringResource(R.string.stat_init), signed(sheet.initiative), null),
    )
    BoxWithConstraints(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        val scale = LocalDensity.current.fontScale.coerceAtMost(1.5f)
        val columns = if (maxWidth / 3 >= 104.dp * scale) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in tiles.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((label, value, target) in row) {
                        StatTile(label, value, selected = target == Panel.HitPoints, onClick = { target?.let(open) }, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Slots(sheet: CharacterSheet, catalog: Catalog) {
    val c = CarnetTheme.colors
    Column(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val slots = sheet.spellSlots.mapIndexed { i, n -> roman(i + 1) to n }.filter { it.second > 0 }
        if (slots.isNotEmpty()) {
            val description = sheet.spellSlots.mapIndexedNotNull { i, n -> if (n > 0) stringResource(R.string.slots_a11y_item, i + 1, n) else null }
                .joinToString(stringResource(R.string.list_separator))
            SpellSlots(slots, stringResource(R.string.slots_a11y, description))
        }
        sheet.pactSlots?.let { Text(stringResource(R.string.pact_slots, it.count, it.level), style = CarnetTheme.type.bodySmall, color = c.text2) }
        // Sorts de chaque classe qui lance : mineurs, appris, préparés, selon son tableau et la page Spells.
        val separator = stringResource(R.string.summary_separator)
        for (casting in sheet.spellcasting) {
            val parts = listOfNotNull(
                catalog.name(casting.classId),
                casting.cantrips?.let { pluralStringResource(R.plurals.spells_cantrips, it, it) },
                casting.known?.let { pluralStringResource(R.plurals.spells_known, it, it) },
                casting.prepared?.let { pluralStringResource(R.plurals.spells_prepared, it, it) },
            )
            if (parts.size > 1) Text(parts.joinToString(separator), style = CarnetTheme.type.bodySmall, color = c.text2)
        }
        if (slots.isEmpty() && sheet.pactSlots == null && sheet.level > 0) {
            Text(stringResource(R.string.slots_none), style = CarnetTheme.type.bodySmall, color = c.muted)
        }
    }
}

@Composable
private fun LevelListRow(row: LevelRow, state: EditorUiState.Ready, onClick: () -> Unit) {
    val c = CarnetTheme.colors
    val hidden = stringResource(R.string.hidden_choice)
    val classId = row.level.classId.value
    val title = stringResource(R.string.level_title, row.index + 1, state.catalog.name(classId) ?: hidden, row.classLevel)
    val subtitle = when {
        row.subclassHere -> row.level.subclassId?.let { stringResource(R.string.level_subclass, state.catalog.name(it) ?: hidden) }
            ?: stringResource(R.string.level_choose_subclass)
        row.featHere -> row.level.feat?.let { stringResource(R.string.level_feat, featLabel(it, state.catalog)) }
            ?: stringResource(R.string.level_choose_feat)
        else -> null
    }
    ListRow(title, onClick, subtitle = subtitle) {
        val emblem = emblemOf(classId)
        if (emblem != null) {
            val pigment = pigmentOf(classId, c)
            Canvas(Modifier.size(24.dp)) { drawClassEmblem(emblem, pigment, strokeWidth = 1.3f) }
        }
    }
}

@Composable
private fun featLabel(feat: FeatChoice, catalog: Catalog): String {
    val name = catalog.name(feat.featId) ?: stringResource(R.string.hidden_choice)
    if (feat.abilityIncreases.isEmpty()) return name
    val parts = Ability.entries.filter { it in feat.abilityIncreases }.map { a -> "${signed(feat.abilityIncreases.getValue(a))} ${abbreviation(a)}" }
    return stringResource(R.string.level_feat_increase, name, parts.joinToString(stringResource(R.string.list_separator)))
}

@Composable
private fun Origin(state: EditorUiState.Ready, open: (Panel) -> Unit) {
    val build = state.build
    val hidden = stringResource(R.string.hidden_choice)
    val none = stringResource(R.string.origin_none)
    ListRow(stringResource(R.string.origin_race), { open(Panel.Race) }, subtitle = build.race?.let { state.catalog.name(it) ?: hidden } ?: none)
    if (build.race != null && state.catalog.subracesOf(build.race!!.value).isNotEmpty()) {
        ListRow(stringResource(R.string.origin_subrace), { open(Panel.Subrace) }, subtitle = build.subrace?.let { state.catalog.name(it) ?: hidden } ?: none)
    }
    ListRow(stringResource(R.string.origin_background), { open(Panel.Background) }, subtitle = build.background?.let { state.catalog.name(it) ?: hidden } ?: none)
}

@Composable
private fun panelTitle(panel: Panel, state: EditorUiState.Ready): String = when (panel) {
    is Panel.AbilityPanel -> stringResource(panel.ability.labelRes)
    Panel.HitPoints, Panel.ArmourClass, Panel.SpellDc, Panel.SpellAttack -> stringResource(R.string.detail_title)
    is Panel.Level -> stringResource(R.string.sheet_level, panel.index + 1)
    Panel.AddLevel -> stringResource(R.string.sheet_add_level, state.build.levels.size + 1)
    Panel.Race -> stringResource(R.string.origin_race)
    Panel.Subrace -> stringResource(R.string.origin_subrace)
    Panel.Background -> stringResource(R.string.origin_background)
    Panel.Share -> stringResource(R.string.share_title)
    Panel.Delete -> stringResource(R.string.editor_delete)
    is Panel.Skills -> stringResource(R.string.section_skills)
}

@Composable
private fun SheetTitle(text: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = CarnetTheme.type.headline, color = CarnetTheme.colors.text, modifier = Modifier.weight(1f).semantics { heading() })
        trailing()
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(text, style = CarnetTheme.type.label, color = CarnetTheme.colors.gold, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp).semantics { heading() })
}

@Composable
private fun PanelContent(panel: Panel, state: EditorUiState.Ready, onEdit: (BuildEdit) -> Unit, close: () -> Unit, onDelete: () -> Unit, onSend: (String) -> Unit) {
    when (panel) {
        is Panel.AbilityPanel -> AbilityPanel(panel.ability, state, onEdit)
        Panel.HitPoints -> HitPointsDetail(state)
        Panel.ArmourClass -> ArmourDetail(state)
        Panel.SpellDc -> SpellDetail(state, dc = true)
        Panel.SpellAttack -> SpellDetail(state, dc = false)
        is Panel.Level -> LevelPanel(panel.index, state, onEdit)
        Panel.AddLevel -> ChoicePanel(stringResource(R.string.sheet_add_level, state.build.levels.size + 1), state.catalog.classes.map { it.id to it.name.fr }, null) {
            onEdit(BuildEdit.AddLevel(it)); close()
        }
        Panel.Race -> ChoicePanel(stringResource(R.string.origin_race), state.catalog.races.map { it.id to it.name.fr }, state.build.race) { onEdit(BuildEdit.Race(it)); close() }
        Panel.Subrace -> ChoicePanel(
            stringResource(R.string.origin_subrace),
            state.build.race?.let { r -> state.catalog.subracesOf(r.value).map { it.id to it.name.fr } }.orEmpty(),
            state.build.subrace,
        ) { onEdit(BuildEdit.Subrace(it)); close() }
        Panel.Background -> ChoicePanel(stringResource(R.string.origin_background), state.catalog.backgrounds.map { it.id to it.name.fr }, state.build.background) {
            onEdit(BuildEdit.Background(it)); close()
        }
        Panel.Share -> SharePanel(state, onSend)
        Panel.Delete -> DeletePanel(state.build.name, onDelete, close)
        is Panel.Skills -> SkillsPanel(panel.sourceId, state, onEdit)
    }
    Box(Modifier.padding(top = 12.dp)) { TextAction(stringResource(R.string.sheet_close), close) }
}

@Composable
private fun ChoicePanel(title: String, options: List<Pair<ContentId, String>>, selected: ContentId?, onChoose: (ContentId) -> Unit) {
    SheetTitle(title)
    TagRow(Modifier.padding(vertical = 6.dp)) {
        for ((id, name) in options) ChoiceTag(name, id == selected, { onChoose(id) })
    }
}

@Composable
private fun AbilityPanel(ability: Ability, state: EditorUiState.Ready, onEdit: (BuildEdit) -> Unit) {
    val c = CarnetTheme.colors
    val build = state.build
    val name = stringResource(ability.labelRes)
    val base = build.baseScores[ability] ?: PointBuy.MIN
    SheetTitle(name) {
        Text(state.sheet.scores.getValue(ability).toString(), style = CarnetTheme.type.numericLarge, color = c.gold)
    }
    SubTitle(stringResource(R.string.sheet_ability_base))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StepButton("\u2212", stringResource(R.string.sheet_decrease, name), enabled = base > PointBuy.MIN) { onEdit(BuildEdit.Base(ability, base - 1)) }
        Text(base.toString(), style = CarnetTheme.type.numeric, color = c.text, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 40.dp))
        StepButton("+", stringResource(R.string.sheet_increase, name), enabled = BuildEdits.canIncrease(build, ability)) { onEdit(BuildEdit.Base(ability, base + 1)) }
        Text(stringResource(R.string.sheet_ability_remaining, state.sheet.pointBuyRemaining), style = CarnetTheme.type.bodySmall, color = c.text2)
    }
    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceTag(stringResource(R.string.sheet_major), build.majorBonus == ability, { onEdit(BuildEdit.Major(ability)) })
        ChoiceTag(stringResource(R.string.sheet_minor), build.minorBonus == ability, { onEdit(BuildEdit.Minor(ability)) })
    }
    Text(stringResource(R.string.sheet_bonus_hint), style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(top = 10.dp))
}

/** Bouton de pas : cible de 48 dp, signe gravé dans un cercle. */
@Composable
private fun StepButton(sign: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = CarnetTheme.colors
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(24.dp))
            .background(if (enabled) c.surface else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(48.dp)) { drawCircle(if (enabled) c.goldDeep else c.line, size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx())) }
        Text(sign, style = CarnetTheme.type.title, color = if (enabled) c.gold else c.muted, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
private fun DetailHeader(short: String, total: Int) {
    SheetTitle(stringResource(R.string.detail_title)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(short, style = CarnetTheme.type.labelSmall, color = CarnetTheme.colors.text2, modifier = Modifier.padding(bottom = 6.dp))
            Text(total.toString(), style = CarnetTheme.type.numericLarge, color = CarnetTheme.colors.gold)
        }
    }
}

@Composable
private fun SourceLine(url: String) {
    val title = Regex("title=([^&]+)").find(url)?.groupValues?.get(1)?.replace('_', ' ').orEmpty()
    val revision = Regex("oldid=(\\d+)").find(url)?.groupValues?.get(1).orEmpty()
    Text(stringResource(R.string.detail_source, title, revision), style = CarnetTheme.type.readingItalic.copy(fontSize = CarnetTheme.type.bodySmall.fontSize), color = CarnetTheme.colors.text2, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun HitPointsDetail(state: EditorUiState.Ready) {
    val sheet = state.sheet
    val hidden = stringResource(R.string.hidden_choice)
    DetailHeader(stringResource(R.string.detail_hp_short), sheet.hitPoints.total)
    for (term in sheet.hitPoints.terms) {
        when (term) {
            is Term.FirstLevel -> CalcRow(
                stringResource(R.string.detail_first_level, state.catalog.name(term.classId) ?: hidden),
                stringResource(R.string.detail_first_level_why, term.base, signed(term.constitution)),
                term.value.toString(),
            )
            is Term.LaterLevels -> {
                val name = state.catalog.name(term.classId) ?: hidden
                val last = sheet.classLevels[term.classId] ?: term.count
                val first = last - term.count + 1
                if (term.count == 1) CalcRow(stringResource(R.string.detail_later_one, name, first), stringResource(R.string.detail_later_one_why, term.perLevel, signed(term.constitution)), term.value.toString())
                else CalcRow(stringResource(R.string.detail_later_many, name, first, last), stringResource(R.string.detail_later_many_why, term.count, term.perLevel, signed(term.constitution)), term.value.toString())
            }
            is Term.PerLevelBonus -> CalcRow(state.catalog.name(term.sourceId) ?: hidden, stringResource(R.string.detail_per_level_why, signed(term.perLevel), term.levels), term.value.toString())
            is Term.DifficultyBonus -> CalcRow(stringResource(R.string.detail_difficulty), stringResource(R.string.detail_difficulty_why, term.percent), term.value.toString())
            else -> Unit
        }
    }
    CalcRow(stringResource(R.string.detail_total), "", sheet.hitPoints.total.toString(), total = true)
    SourceLine(RuleSources.HIT_POINTS)
}

@Composable
private fun ArmourDetail(state: EditorUiState.Ready) {
    val sheet = state.sheet
    DetailHeader(stringResource(R.string.detail_ac_short), sheet.armourClass.total)
    for (term in sheet.armourClass.terms) {
        when (term) {
            is Term.Base -> CalcRow(
                stringResource(R.string.detail_ac_base),
                stringResource(
                    when (term.formula) {
                        null -> R.string.detail_ac_base_none
                        UnarmouredFormula.MAGE_ARMOUR -> R.string.detail_ac_base_mage
                        UnarmouredFormula.BARBARIAN -> R.string.detail_ac_base_barbarian
                        UnarmouredFormula.MONK -> R.string.detail_ac_base_monk
                    },
                ),
                term.value.toString(),
            )
            is Term.AbilityModifier -> CalcRow(stringResource(term.ability.labelRes), stringResource(R.string.detail_modifier), signed(term.value))
            else -> Unit
        }
    }
    CalcRow(stringResource(R.string.detail_total), "", sheet.armourClass.total.toString(), total = true)
    Text(stringResource(R.string.unarmoured_note), style = CarnetTheme.type.bodySmall, color = CarnetTheme.colors.text2, modifier = Modifier.padding(top = 8.dp))
    SourceLine(RuleSources.ARMOUR_CLASS)
}

@Composable
private fun SpellDetail(state: EditorUiState.Ready, dc: Boolean) {
    val sheet = state.sheet
    val hidden = stringResource(R.string.hidden_choice)
    val first = sheet.spellcasting.firstOrNull() ?: return
    if (dc) DetailHeader(stringResource(R.string.detail_dc_short), first.saveDc) else SheetTitle(stringResource(R.string.detail_attack_title)) {
        Text(signed(first.attackBonus), style = CarnetTheme.type.numericLarge, color = CarnetTheme.colors.gold)
    }
    for (casting in sheet.spellcasting) {
        if (sheet.spellcasting.size > 1) SubTitle(stringResource(R.string.detail_dc_class, state.catalog.name(casting.classId) ?: hidden, stringResource(casting.ability.labelRes)))
        if (dc) CalcRow(stringResource(R.string.detail_dc_base), "", "8")
        CalcRow(stringResource(casting.ability.labelRes), stringResource(R.string.detail_modifier), signed(casting.modifier))
        CalcRow(stringResource(R.string.detail_proficiency), "", signed(sheet.proficiency))
        CalcRow(stringResource(R.string.detail_total), "", if (dc) casting.saveDc.toString() else signed(casting.attackBonus), total = true)
    }
    SourceLine(RuleSources.PROFICIENCY)
}

@Composable
private fun LevelPanel(index: Int, state: EditorUiState.Ready, onEdit: (BuildEdit) -> Unit) {
    val c = CarnetTheme.colors
    val row = state.rows.getOrNull(index) ?: return
    val classId = row.level.classId.value
    SheetTitle(stringResource(R.string.level_title, index + 1, state.catalog.name(classId) ?: stringResource(R.string.hidden_choice), row.classLevel))
    SubTitle(stringResource(R.string.sheet_level_class))
    TagRow {
        for (cls in state.catalog.classes) ChoiceTag(cls.name.fr, cls.id == row.level.classId, { onEdit(BuildEdit.LevelClass(index, cls.id)) })
    }
    if (row.subclassHere) {
        SubTitle(stringResource(R.string.sheet_level_subclass))
        TagRow {
            for (sub in state.catalog.subclassesOf(classId)) {
                ChoiceTag(sub.name.fr, sub.id == row.level.subclassId, { on -> onEdit(BuildEdit.Subclass(classId, if (on) sub.id else null)) })
            }
        }
    }
    if (row.featHere) {
        SubTitle(stringResource(R.string.sheet_level_feat))
        val chosen = row.level.feat
        // La répartition d'abord : elle reste sous les yeux, au-dessus de la longue liste des dons.
        val increase = chosen?.let { state.rules.feats[it.featId.value]?.abilityIncrease }
        if (chosen != null && increase != null) {
            val left = increase.amount - chosen.abilityIncreases.values.sum()
            Text(
                if (left > 0) stringResource(R.string.sheet_feat_split, left) else stringResource(R.string.sheet_feat_split_done),
                style = CarnetTheme.type.bodySmall, color = if (left > 0) c.vermilion else c.text2, modifier = Modifier.padding(bottom = 6.dp),
            )
            TagRow {
                for (a in Ability.entries.filter { increase.choices.isEmpty() || it in increase.choices }) {
                    val n = chosen.abilityIncreases[a] ?: 0
                    val label = if (n > 0) "${stringResource(a.labelRes)} ${signed(n)}" else stringResource(a.labelRes)
                    ChoiceTag(label, n > 0, {
                        val next = BuildEdits.toggleIncrease(chosen.abilityIncreases, a, increase.amount, increase.choices, increase.splittable)
                        onEdit(BuildEdit.Feat(index, chosen.copy(abilityIncreases = next)))
                    })
                }
            }
            Box(Modifier.height(12.dp))
        }
        TagRow {
            ChoiceTag(stringResource(R.string.sheet_feat_none), chosen == null, { onEdit(BuildEdit.Feat(index, null)) })
            for (feat in state.catalog.feats) {
                ChoiceTag(feat.name.fr, feat.id == chosen?.featId, { on -> onEdit(BuildEdit.Feat(index, if (on) FeatChoice(feat.id) else null)) })
            }
        }
    }
}

/** Choix de compétences de chaque source (classe de départ, multiclasse, race), avec leur avancement. */
@Composable
private fun SkillSources(state: EditorUiState.Ready, open: (Panel) -> Unit) {
    val hidden = stringResource(R.string.hidden_choice)
    for (source in CharacterSheetCalculator.skillSources(state.build, state.rules)) {
        val picked = state.build.skillChoices[source.id].orEmpty()
        ListRow(
            stringResource(R.string.skills_source, state.catalog.name(source.id) ?: hidden, picked.size, source.count),
            { open(Panel.Skills(source.id)) },
            subtitle = skillNames(picked).ifEmpty { stringResource(R.string.skills_none_chosen) },
        )
    }
}

@Composable
private fun skillNames(skills: List<Skill>): String =
    Skill.entries.filter { it in skills }.map { stringResource(it.labelRes) }.joinToString(stringResource(R.string.list_separator))

/** Les 18 compétences, avec leur bonus ; un losange d'or marque celles qui sont maîtrisées. */
@Composable
private fun SkillsGrid(sheet: CharacterSheet) {
    val c = CarnetTheme.colors
    BoxWithConstraints(Modifier.padding(horizontal = CarnetTheme.dimens.gutter, vertical = 8.dp)) {
        val scale = LocalDensity.current.fontScale.coerceAtMost(1.6f)
        val columns = if (maxWidth / 2 >= 160.dp * scale) 2 else 1
        val proficient = stringResource(R.string.skill_proficient)
        val sorted = Skill.entries.map { it to stringResource(it.labelRes) }.sortedBy { Folding.fold(it.second) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (row in sorted.chunked(columns)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for ((skill, label) in row) {
                        val mastered = skill in sheet.skillProficiencies
                        val bonus = signed(sheet.skillBonuses.getValue(skill))
                        Row(
                            Modifier.weight(1f).heightIn(min = 32.dp).clearAndSetSemantics {
                                contentDescription = if (mastered) "$label $bonus, $proficient" else "$label $bonus"
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Canvas(Modifier.size(12.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width / 2, 1f); lineTo(size.width - 1f, size.height / 2)
                                    lineTo(size.width / 2, size.height - 1f); lineTo(1f, size.height / 2); close()
                                }
                                if (mastered) drawPath(path, c.gold) else drawPath(path, c.line, style = Stroke(1.dp.toPx()))
                            }
                            Text(label, style = CarnetTheme.type.bodySmall, color = if (mastered) c.text else c.text2, modifier = Modifier.weight(1f).padding(start = 8.dp))
                            Text(bonus, style = CarnetTheme.type.dataStrong, color = if (mastered) c.gold else c.text2)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SkillsPanel(sourceId: String, state: EditorUiState.Ready, onEdit: (BuildEdit) -> Unit) {
    val c = CarnetTheme.colors
    val source = CharacterSheetCalculator.skillSources(state.build, state.rules).firstOrNull { it.id == sourceId } ?: return
    val picked = state.build.skillChoices[sourceId].orEmpty()
    val fixed = CharacterSheetCalculator.fixedSkills(state.build, state.rules)
    val elsewhere = state.build.skillChoices.filterKeys { it != sourceId }.values.flatten().toSet()
    SheetTitle(stringResource(R.string.skills_source, state.catalog.name(sourceId) ?: stringResource(R.string.hidden_choice), picked.size, source.count))
    Text(stringResource(R.string.skills_hint), style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(bottom = 8.dp))
    TagRow {
        val options = Skill.entries.filter { it in source && it !in fixed && it !in elsewhere }.map { it to stringResource(it.labelRes) }.sortedBy { Folding.fold(it.second) }
        for ((skill, label) in options) ChoiceTag(label, skill in picked, { onEdit(BuildEdit.ToggleSkill(sourceId, skill)) })
    }
    if (fixed.isNotEmpty()) {
        Text(stringResource(R.string.skills_fixed, skillNames(fixed.toList())), style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun SharePanel(state: EditorUiState.Ready, onSend: (String) -> Unit) {
    val c = CarnetTheme.colors
    val code = state.shareCode
    SheetTitle(stringResource(R.string.share_title))
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        QrCode(code, stringResource(R.string.share_qr_a11y, state.build.name))
    }
    SubTitle(stringResource(R.string.share_code_label))
    SelectionContainer {
        Text(code, style = CarnetTheme.type.dataSmall, color = c.text)
    }
    Text(stringResource(R.string.share_hint), style = CarnetTheme.type.bodySmall, color = c.text2, modifier = Modifier.padding(vertical = 10.dp))
    GoldButton(stringResource(R.string.share_send), { onSend(code) })
}

/** QR code dessiné module par module, encre sombre sur fond clair quel que soit le thème (lisible par un appareil photo). */
@Composable
private fun QrCode(text: String, a11y: String) {
    val matrix = remember(text) { QrCodes.encode(text) } ?: return
    Box(
        Modifier.size(232.dp).clip(RoundedCornerShape(CarnetTheme.dimens.radius)).background(QR_PAPER).padding(16.dp)
            .clearAndSetSemantics { contentDescription = a11y },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.minDimension / matrix.width
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (matrix[x, y]) drawRect(QR_INK, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

private val QR_PAPER = Color(0xFFF4ECD8)
private val QR_INK = Color(0xFF1E1710)

@Composable
private fun DeletePanel(name: String, onDelete: () -> Unit, close: () -> Unit) {
    SheetTitle(stringResource(R.string.editor_delete))
    Text(stringResource(R.string.editor_delete_confirm, name), style = CarnetTheme.type.reading, color = CarnetTheme.colors.text, modifier = Modifier.padding(vertical = 8.dp))
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GoldButton(stringResource(R.string.editor_delete_no), close)
        QuietButton(stringResource(R.string.editor_delete_yes), { close(); onDelete() })
    }
}

/** Partage : trois nœuds reliés, tracés comme les autres glyphes gravés. */
@Composable
private fun ShareGlyph(color: Color) {
    Canvas(Modifier.size(24.dp)) {
        val k = size.width / 24f
        val a = Offset(17f * k, 5.5f * k)
        val b = Offset(6.5f * k, 12f * k)
        val d = Offset(17f * k, 18.5f * k)
        drawLine(color, b, a, 1.6f * k, StrokeCap.Round)
        drawLine(color, b, d, 1.6f * k, StrokeCap.Round)
        for (p in listOf(a, b, d)) drawCircle(color, 2.6f * k, p, style = Stroke(1.6f * k))
    }
}
