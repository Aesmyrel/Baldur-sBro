package fr.carnetdefaerun.feature.builds

import fr.carnetdefaerun.core.data.content.EntryList
import fr.carnetdefaerun.core.data.content.EntrySummary
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildCode
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import fr.carnetdefaerun.core.rules.MAX_CHARACTER_LEVEL
import fr.carnetdefaerun.core.rules.PointBuy
import fr.carnetdefaerun.core.rules.RulesData

/**
 * Noms visibles des options d'un build, tels que la porte de contenu les donne (verrou appliqué, noms corrigés).
 * Une option absente d'ici est masquée ou inconnue : l'écran la montre comme un « choix voilé ».
 */
data class Catalog(
    val classes: List<EntrySummary> = emptyList(),
    val subclasses: List<EntrySummary> = emptyList(),
    val feats: List<EntrySummary> = emptyList(),
    val races: List<EntrySummary> = emptyList(),
    val subraces: List<EntrySummary> = emptyList(),
    val backgrounds: List<EntrySummary> = emptyList(),
) {
    private val byId: Map<String, EntrySummary> = (classes + subclasses + feats + races + subraces + backgrounds).associateBy { it.id.value }

    fun name(id: ContentId?): String? = id?.let { byId[it.value]?.name?.fr }
    fun name(id: String): String? = byId[id]?.name?.fr

    /** Sous-classes d'une classe, sous-races d'une race : le sous-type de la fiche est l'identifiant du parent. */
    fun subclassesOf(classId: String) = subclasses.filter { it.subtype == classId }
    fun subracesOf(raceId: String) = subraces.filter { it.subtype == raceId }

    companion object {
        fun of(classes: EntryList, subclasses: EntryList, feats: EntryList, races: EntryList, subraces: EntryList, backgrounds: EntryList) =
            Catalog(classes.visible, subclasses.visible, feats.visible, races.visible, subraces.visible, backgrounds.visible)
    }
}

/**
 * Éditions d'un build, en fonctions pures. Après chaque changement de la suite des niveaux, [normalize] replace
 * les choix au bon niveau : la sous-classe d'une classe au niveau de classe où elle se choisit, les dons d'une classe
 * sur ses niveaux de don, dans l'ordre. Un choix qui n'a plus de place (classe retirée) est abandonné.
 */
object BuildEdits {
    fun normalize(build: Build, rules: RulesData): Build =
        cleanSkills(build.copy(levels = place(build.levels.take(MAX_CHARACTER_LEVEL).map { it.classId }, build.levels, rules)), rules)

    /**
     * Pose sur la suite de classes [classes] les choix lus dans [source] : la première sous-classe valide de chaque
     * classe, et ses dons dans l'ordre où ils avaient été pris. [source] peut être l'ancienne suite (changement de
     * classe d'un niveau) : un don ou une sous-classe portés par le niveau changé restent ainsi à leur classe.
     */
    private fun place(classes: List<ContentId>, source: List<BuildLevel>, rules: RulesData): List<BuildLevel> {
        val subclass = mutableMapOf<String, ContentId>()
        val feats = mutableMapOf<String, ArrayDeque<FeatChoice>>()
        for (l in source) {
            val c = l.classId.value
            l.subclassId?.let { s -> if (c !in subclass && rules.subclasses[s.value]?.parentClass == c) subclass[c] = s }
            l.feat?.let { f -> feats.getOrPut(c) { ArrayDeque() }.addLast(f) }
        }
        val count = mutableMapOf<String, Int>()
        return classes.mapIndexed { i, id ->
            val c = id.value
            val profile = rules.classes[c] ?: return@mapIndexed source.getOrNull(i)?.takeIf { it.classId == id } ?: BuildLevel(id)
            val n = (count[c] ?: 0) + 1
            count[c] = n
            BuildLevel(
                classId = id,
                subclassId = if (n == profile.subclassLevel) subclass[c] else null,
                feat = if (n in profile.featLevels) feats[c]?.removeFirstOrNull() else null,
            )
        }
    }

    /**
     * Choix de compétences gardés seulement pour les sources encore présentes, dans leur liste, sans doublon ni
     * compétence déjà maîtrisée d'office, et pas au-delà du nombre permis.
     */
    fun cleanSkills(build: Build, rules: RulesData): Build {
        val fixed = CharacterSheetCalculator.fixedSkills(build, rules)
        val taken = fixed.toMutableSet()
        val kept = CharacterSheetCalculator.skillSources(build, rules).associate { source ->
            val picks = build.skillChoices[source.id].orEmpty().filter { it in source && it !in taken }.distinct().take(source.count)
            taken += picks
            source.id to picks
        }.filterValues { it.isNotEmpty() }
        return build.copy(skillChoices = kept)
    }

    /** Toucher une compétence d'un choix : elle entre s'il reste de la place, sort si elle y était. */
    fun toggleSkill(build: Build, sourceId: String, skill: Skill, rules: RulesData): Build {
        val source = CharacterSheetCalculator.skillSources(build, rules).firstOrNull { it.id == sourceId } ?: return build
        val current = build.skillChoices[sourceId].orEmpty()
        val next = when {
            skill in current -> current - skill
            current.size < source.count -> current + skill
            else -> return build
        }
        return cleanSkills(build.copy(skillChoices = build.skillChoices + (sourceId to next)), rules)
    }

    fun addLevel(build: Build, classId: ContentId, rules: RulesData): Build =
        if (build.levels.size >= MAX_CHARACTER_LEVEL) build else normalize(build.copy(levels = build.levels + BuildLevel(classId)), rules)

    fun removeLastLevel(build: Build, rules: RulesData): Build = normalize(build.copy(levels = build.levels.dropLast(1)), rules)

    fun setLevelClass(build: Build, index: Int, classId: ContentId, rules: RulesData): Build {
        val current = build.levels.getOrNull(index) ?: return build
        if (current.classId == classId) return build
        val classes = build.levels.map { it.classId }.toMutableList().also { it[index] = classId }
        return cleanSkills(build.copy(levels = place(classes.take(MAX_CHARACTER_LEVEL), build.levels, rules)), rules)
    }

    /** Sous-classe d'une classe du build : placée au niveau où elle se choisit. */
    fun setSubclass(build: Build, classId: String, subclassId: ContentId?, rules: RulesData): Build {
        val levels = build.levels.map { l -> if (l.classId.value == classId) l.copy(subclassId = null) else l }.toMutableList()
        if (subclassId != null) {
            val at = levels.indexOfFirst { it.classId.value == classId }
            if (at >= 0) levels[at] = levels[at].copy(subclassId = subclassId)
        }
        return normalize(build.copy(levels = levels), rules)
    }

    fun setFeat(build: Build, index: Int, feat: FeatChoice?, rules: RulesData): Build {
        val current = build.levels.getOrNull(index) ?: return build
        val levels = build.levels.toMutableList().also { it[index] = current.copy(feat = feat) }
        return normalize(build.copy(levels = levels), rules)
    }

    fun setBase(build: Build, ability: Ability, value: Int): Build =
        build.copy(baseScores = build.baseScores + (ability to value.coerceIn(PointBuy.MIN, PointBuy.MAX)))

    /** Peut-on monter [ability] d'un cran sans dépasser 15 ni le budget ? */
    fun canIncrease(build: Build, ability: Ability): Boolean {
        val current = build.baseScores[ability] ?: PointBuy.MIN
        return current < PointBuy.MAX && PointBuy.spent(build.baseScores + (ability to current + 1)) <= PointBuy.BUDGET
    }

    /** Bonus +2 : s'il tombe sur la caractéristique du +1, celui-ci passe à l'ancienne du +2. */
    fun setMajor(build: Build, ability: Ability): Build =
        build.copy(majorBonus = ability, minorBonus = if (build.minorBonus == ability) build.majorBonus else build.minorBonus)

    fun setMinor(build: Build, ability: Ability): Build =
        build.copy(minorBonus = ability, majorBonus = if (build.majorBonus == ability) build.minorBonus else build.majorBonus)

    fun setRace(build: Build, race: ContentId, catalog: Catalog, rules: RulesData): Build {
        val keepSubrace = build.subrace?.let { s -> catalog.subracesOf(race.value).any { it.id == s } } == true
        return cleanSkills(build.copy(race = race, subrace = if (keepSubrace) build.subrace else null), rules)
    }

    fun rename(build: Build, name: String): Build = build.copy(name = BuildCode.cleanName(name))

    /**
     * Répartition d'un don après un toucher sur [ability] : +1 de plus tant qu'il reste des points à placer et que
     * la fiche le permet, sinon retour à 0. Un don à une seule caractéristique déplace son point.
     */
    fun toggleIncrease(current: Map<Ability, Int>, ability: Ability, amount: Int, choices: List<Ability>, splittable: Boolean): Map<Ability, Int> {
        if (choices.isNotEmpty() && ability !in choices) return current
        if (!splittable) return if (current[ability] == amount) emptyMap() else mapOf(ability to amount)
        val placed = current.values.sum()
        val mine = current[ability] ?: 0
        return when {
            placed < amount -> current + (ability to mine + 1)
            mine > 0 -> current - ability
            else -> current
        }
    }
}
