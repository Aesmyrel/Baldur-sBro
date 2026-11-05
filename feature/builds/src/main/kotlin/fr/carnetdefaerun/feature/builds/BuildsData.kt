package fr.carnetdefaerun.feature.builds

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import fr.carnetdefaerun.core.data.content.ContentGateway
import fr.carnetdefaerun.core.designsystem.illustration.ClassEmblem
import fr.carnetdefaerun.core.designsystem.theme.CarnetColors
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.rules.CharacterSheet
import fr.carnetdefaerun.core.rules.RulesData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Noms visibles de toutes les options d'un build, mis à jour avec le verrou et les noms corrigés. */
fun ContentGateway.catalog(): Flow<Catalog> = combine(
    listOf(EntryType.CLASS, EntryType.SUBCLASS, EntryType.FEAT, EntryType.RACE, EntryType.SUBRACE, EntryType.BACKGROUND).map(::list),
) { lists -> Catalog.of(lists[0], lists[1], lists[2], lists[3], lists[4], lists[5]) }

/** Une édition demandée par l'écran ; le ViewModel l'applique au build courant. */
sealed interface BuildEdit {
    data class Rename(val name: String) : BuildEdit
    data class Base(val ability: Ability, val value: Int) : BuildEdit
    data class Major(val ability: Ability) : BuildEdit
    data class Minor(val ability: Ability) : BuildEdit
    data class AddLevel(val classId: ContentId) : BuildEdit
    data object RemoveLastLevel : BuildEdit
    data class LevelClass(val index: Int, val classId: ContentId) : BuildEdit
    data class Subclass(val classId: String, val subclassId: ContentId?) : BuildEdit
    data class Feat(val index: Int, val feat: FeatChoice?) : BuildEdit
    data class Race(val id: ContentId) : BuildEdit
    data class Subrace(val id: ContentId?) : BuildEdit
    data class Background(val id: ContentId?) : BuildEdit
    data class ToggleSkill(val sourceId: String, val skill: Skill) : BuildEdit

    fun applyTo(build: Build, rules: RulesData, catalog: Catalog): Build = when (this) {
        is Rename -> BuildEdits.rename(build, name)
        is Base -> BuildEdits.setBase(build, ability, value)
        is Major -> BuildEdits.setMajor(build, ability)
        is Minor -> BuildEdits.setMinor(build, ability)
        is AddLevel -> BuildEdits.addLevel(build, classId, rules)
        RemoveLastLevel -> BuildEdits.removeLastLevel(build, rules)
        is LevelClass -> BuildEdits.setLevelClass(build, index, classId, rules)
        is Subclass -> BuildEdits.setSubclass(build, classId, subclassId, rules)
        is Feat -> BuildEdits.setFeat(build, index, feat, rules)
        is Race -> BuildEdits.setRace(build, id, catalog, rules)
        is Subrace -> BuildEdits.cleanSkills(build.copy(subrace = id), rules)
        is Background -> BuildEdits.cleanSkills(build.copy(background = id), rules)
        is ToggleSkill -> BuildEdits.toggleSkill(build, sourceId, skill, rules)
    }
}

/** Un niveau du build vu par l'écran : la classe, son niveau de classe, ce qui s'y choisit. */
data class LevelRow(
    val index: Int,
    val level: BuildLevel,
    val classLevel: Int,
    val subclassHere: Boolean,
    val featHere: Boolean,
)

fun levelRows(build: Build, rules: RulesData): List<LevelRow> {
    val count = mutableMapOf<String, Int>()
    return build.levels.mapIndexed { i, l ->
        val c = l.classId.value
        val n = (count[c] ?: 0) + 1
        count[c] = n
        val profile = rules.classes[c]
        LevelRow(i, l, n, profile?.subclassLevel == n, profile?.featLevels?.contains(n) == true)
    }
}

/** Emblème de la classe ; null pour une classe inconnue. */
fun emblemOf(classId: String): ClassEmblem? = when (classId) {
    "class.barbarian" -> ClassEmblem.BARBARE
    "class.bard" -> ClassEmblem.BARDE
    "class.cleric" -> ClassEmblem.CLERC
    "class.druid" -> ClassEmblem.DRUIDE
    "class.fighter" -> ClassEmblem.GUERRIER
    "class.monk" -> ClassEmblem.MOINE
    "class.paladin" -> ClassEmblem.PALADIN
    "class.ranger" -> ClassEmblem.RODEUR
    "class.rogue" -> ClassEmblem.ROUBLARD
    "class.sorcerer" -> ClassEmblem.ENSORCELEUR
    "class.warlock" -> ClassEmblem.OCCULTISTE
    "class.wizard" -> ClassEmblem.MAGICIEN
    else -> null
}

/** Pigment d'enluminure par famille de classe : martiales vermillon, arcanes lapis, pacte et chant pourpre, etc. */
fun pigmentOf(classId: String, c: CarnetColors): Color = when (classId) {
    "class.barbarian", "class.fighter" -> c.vermilion
    "class.wizard", "class.sorcerer" -> c.lapis
    "class.warlock", "class.bard" -> c.tyrian
    "class.druid", "class.ranger" -> c.verdigris
    "class.cleric", "class.paladin" -> c.orpiment
    else -> c.gold
}

/** Nombre signé avec le vrai signe moins. */
fun signed(value: Int): String = if (value >= 0) "+$value" else "−${-value}"

/** Chiffres romains des niveaux de sort (1 à 9). */
fun roman(n: Int): String = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX").getOrElse(n - 1) { n.toString() }

/** Résumé d'un build : origine · niveau · classes (sous-classe de la classe principale). */
@Composable
fun summaryLine(build: Build, sheet: CharacterSheet, catalog: Catalog): String {
    val hidden = stringResource(R.string.hidden_choice)
    val parts = mutableListOf<String>()
    (build.subrace ?: build.race)?.let { parts += catalog.name(it) ?: hidden }
    parts += if (sheet.level == 0) stringResource(R.string.summary_no_level) else stringResource(R.string.summary_level, sheet.level)
    if (sheet.classLevels.isNotEmpty()) {
        val main = sheet.classLevels.maxByOrNull { it.value }?.key
        val separator = stringResource(R.string.summary_classes_separator)
        val classes = sheet.classLevels.entries.map { (id, n) ->
            val label = stringResource(R.string.summary_class_level, catalog.name(id) ?: hidden, n)
            val sub = sheet.subclasses[id]?.takeIf { id == main }
            if (sub != null) stringResource(R.string.summary_subclass, label, catalog.name(sub) ?: hidden) else label
        }
        parts += classes.joinToString(separator)
    }
    return parts.joinToString(stringResource(R.string.summary_separator))
}
