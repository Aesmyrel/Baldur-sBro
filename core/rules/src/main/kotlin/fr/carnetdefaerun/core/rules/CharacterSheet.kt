package fr.carnetdefaerun.core.rules

import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.AbilityIncrease
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.model.settings.Difficulty

/** Ce que le moteur doit savoir d'une classe ; tiré de sa fiche (data/game) par [RulesData.from], jamais écrit à la main. */
data class ClassProfile(
    val id: String,
    val hitDie: HitDie,
    val savingThrows: Set<Ability>,
    /** Caractéristique d'incantation, y compris pour les sous-classes qui lancent des sorts (chevalier occulte…). */
    val spellcastingAbility: Ability?,
    val progression: CasterProgression,
    /** Niveau de classe où l'Incantation commence : premier niveau à emplacements de son tableau. */
    val spellcastingFrom: Int = 1,
    /** Magie de pacte : emplacements par niveau de classe (indice 0 : niveau 1), au format du tableau de classe. */
    val pactSlots: List<List<Int>> = emptyList(),
    /** Formule de classe d'armure sans armure accordée au niveau 1 (barbare, moine). */
    val unarmoured: UnarmouredFormula? = null,
    /** Niveaux de classe qui donnent un don. */
    val featLevels: Set<Int> = emptySet(),
    /** Niveau de classe où la sous-classe se choisit. */
    val subclassLevel: Int = 1,
    /** Compétences parmi lesquelles la classe laisse choisir. */
    val skillList: List<Skill> = emptyList(),
    /** Nombre de compétences à choisir comme classe de départ, puis en la prenant en multiclasse. */
    val skillChoices: Int = 0,
    val multiclassSkillChoices: Int = 0,
    /** Sorts mineurs et sorts appris par niveau de classe (indice 0 : niveau 1), d'après le tableau de classe ; null sans colonne. */
    val cantripsKnown: List<Int?> = emptyList(),
    val spellsKnown: List<Int?> = emptyList(),
    /** Lanceur qui prépare ses sorts : clerc, druide, paladin, magicien ([RuleSources.SPELLS], « Prepared spells »). */
    val prepares: Boolean = false,
    /**
     * Niveaux de classe qui touchent aux compétences d'une façon pas encore modélisée : Expertise (roublard, barde),
     * Ennemi juré et Explorateur-né (rôdeur), invocations (occultiste). La fiche le signale au lieu de se taire.
     */
    val unmodelledSkillLevels: Set<Int> = emptySet(),
)

/** Race, sous-race ou historique : compétences maîtrisées d'office, et choix laissé (humain : une compétence au choix). */
data class OriginProfile(
    val id: String,
    val skills: Set<Skill> = emptySet(),
    val skillChoices: Int = 0,
    /** Liste du choix ; vide : toutes les compétences. */
    val skillList: List<Skill> = emptyList(),
    /** Compétences où la maîtrise compte double (gnome des roches : Histoire). */
    val expertise: Set<Skill> = emptySet(),
)

/** Ce que le moteur doit savoir d'une sous-classe. */
data class SubclassProfile(
    val id: String,
    val parentClass: String,
    /** Tiers de lanceur (chevalier occulte, escroc arcanique) ; [CasterProgression.NONE] sinon. */
    val progression: CasterProgression = CasterProgression.NONE,
    val spellcastingFrom: Int = 1,
    /** Résilience draconique : +1 point de vie par niveau d'ensorceleur, et 13 + Dextérité sans armure. */
    val draconicResilience: Boolean = false,
    /** Compétences au choix accordées par la sous-classe (Collège du savoir : 3). */
    val skillChoices: Int = 0,
)

/** Ce que le moteur doit savoir d'un don. */
data class FeatProfile(
    val id: String,
    val abilityIncrease: AbilityIncrease? = null,
    /** Robuste : +2 points de vie par niveau du personnage. */
    val tough: Boolean = false,
    /** Résilient : maîtrise des jets de sauvegarde de la caractéristique augmentée. */
    val resilient: Boolean = false,
    /** Doué : compétences au choix ([RuleSources.SKILLED] : 3). */
    val skillChoices: Int = 0,
    /** Vigilant : bonus d'initiative ([RuleSources.ALERT] : +5). */
    val initiativeBonus: Int = 0,
)

/** Données de jeu dont dépend le calcul, tirées de data/game par [RulesData.from]. */
data class RulesData(
    val classes: Map<String, ClassProfile>,
    val subclasses: Map<String, SubclassProfile> = emptyMap(),
    val feats: Map<String, FeatProfile> = emptyMap(),
    /** Sous-races à Robustesse naine (+1 point de vie par niveau du personnage). */
    val dwarvenToughnessSubraces: Set<String> = emptySet(),
    /** Races, sous-races et historiques, pour leurs compétences. */
    val origins: Map<String, OriginProfile> = emptyMap(),
) {
    companion object
}

/**
 * Effets d'un mode de difficulté sur les personnages du joueur ([RuleSources.DIFFICULTY]) : en Explorateur, toutes
 * les maîtrises augmentent de 2, les membres du groupe reçoivent 100 % de points de vie en plus (pris à la lettre :
 * le maximum double) et la multiclasse est impossible. Équilibré, Tacticien et Honneur ne changent rien côté joueur
 * (ils renforcent les adversaires). Le mode personnalisé dépend de réglages que le carnet ne connaît pas : il est
 * traité comme Équilibré.
 */
data class DifficultyRules(val proficiencyDelta: Int = 0, val hitPointMultiplier: Int = 1, val multiclassing: Boolean = true) {
    companion object {
        val BALANCED = DifficultyRules()
        val EXPLORER = DifficultyRules(proficiencyDelta = 2, hitPointMultiplier = 2, multiclassing = false)

        fun of(difficulty: Difficulty): DifficultyRules = if (difficulty == Difficulty.EXPLORER) EXPLORER else BALANCED
    }
}

/** Un terme du détail d'un calcul : ce que l'écran affiche ligne à ligne sous « Détail du calcul ». */
sealed interface Term {
    val value: Int

    /** Premier niveau du personnage : valeur de départ de sa classe + Constitution. */
    data class FirstLevel(val classId: String, val base: Int, val constitution: Int) : Term {
        override val value get() = base + constitution
    }

    /** [count] niveaux suivants d'une classe : gain fixe + Constitution, chacun. */
    data class LaterLevels(val classId: String, val count: Int, val perLevel: Int, val constitution: Int) : Term {
        override val value get() = count * (perLevel + constitution)
    }

    /** Bonus d'une capacité ou d'un don ([sourceId]) : [perLevel] × [levels]. */
    data class PerLevelBonus(val sourceId: String, val perLevel: Int, val levels: Int) : Term {
        override val value get() = perLevel * levels
    }

    /** Base d'une classe d'armure : 10 ou 13 selon la formule ([formula] nulle : 10 + Dextérité). */
    data class Base(override val value: Int, val formula: UnarmouredFormula? = null) : Term

    /** Modificateur de caractéristique ajouté. */
    data class AbilityModifier(val ability: Ability, override val value: Int) : Term

    /** Bonus du mode de difficulté (Explorateur : +100 % des points de vie). */
    data class DifficultyBonus(override val value: Int, val percent: Int) : Term
}

data class Breakdown(val terms: List<Term>) {
    val total: Int get() = terms.sumOf { it.value }
}

/**
 * Incantation d'une classe du personnage : sa caractéristique, son DD et son bonus d'attaque, et ses sorts au niveau
 * de classe atteint : sorts mineurs et sorts appris d'après le tableau de classe, sorts préparés pour les lanceurs
 * qui préparent (modificateur + niveau de classe, au moins 1). Null quand la classe n'a pas cette colonne.
 */
data class Spellcasting(
    val classId: String,
    val ability: Ability,
    val modifier: Int,
    val saveDc: Int,
    val attackBonus: Int,
    val cantrips: Int? = null,
    val known: Int? = null,
    val prepared: Int? = null,
)

/** Un choix de compétences : [count] parmi [list] (vide : toutes), pour la source [id] (classe ou race). */
data class SkillSource(val id: String, val count: Int, val list: List<Skill>) {
    operator fun contains(skill: Skill) = list.isEmpty() || skill in list
}

/** Emplacements de Magie de pacte : [count] emplacements, tous de niveau [level]. */
data class PactSlots(val count: Int, val level: Int)

/** Ce qui rend un build invalide (le jeu ne le permettrait pas) ou incomplet (un choix reste à faire). */
enum class SheetProblem(val blocking: Boolean) {
    NO_LEVEL(false),
    TOO_MANY_LEVELS(true),
    UNKNOWN_CLASS(true),
    POINT_BUY_OUT_OF_RANGE(true),
    POINT_BUY_OVER_BUDGET(true),
    BONUSES_MISSING(false),
    BONUSES_SAME_ABILITY(true),
    /** Un niveau de don n'a pas de don choisi. */
    FEAT_MISSING(false),
    /** Un don est placé à un niveau de classe qui n'en donne pas, ou n'existe pas. */
    FEAT_NOT_ALLOWED(true),
    /** La répartition des points d'un don ne respecte pas sa fiche (montant, caractéristiques permises). */
    FEAT_INCREASE_INVALID(true),
    /** Une classe a atteint le niveau de sa sous-classe sans qu'elle soit choisie. */
    SUBCLASS_MISSING(false),
    /** Des compétences restent à choisir (classe de départ, multiclasse, race). */
    SKILLS_MISSING(false),
    /** Compétence hors de la liste de sa source, en trop, ou déjà maîtrisée par ailleurs. */
    SKILLS_INVALID(true),
    /** Sous-classe inconnue, d'une autre classe, ou choisie à un autre niveau que le sien. */
    SUBCLASS_INVALID(true),
    /**
     * Plusieurs classes en mode Explorateur : on ne peut plus y prendre de nouvelle classe, mais des niveaux déjà
     * acquis restent ([RuleSources.DIFFICULTY]) ; signalé, pas invalide.
     */
    MULTICLASS_DISABLED(false),
    /** Le build a une capacité qui touche aux compétences sans être encore modélisée : les bonus peuvent être sous-estimés. */
    SKILLS_PARTIAL(false),
}

/** Fiche calculée d'un build. Toutes les valeurs dérivées viennent des règles sourcées de ce module. */
data class CharacterSheet(
    val level: Int,
    /** Niveaux par classe, dans l'ordre où chaque classe a été prise. */
    val classLevels: Map<String, Int>,
    /** Sous-classe choisie pour chaque classe qui en a une. */
    val subclasses: Map<String, String>,
    val scores: Map<Ability, Int>,
    val modifiers: Map<Ability, Int>,
    val pointBuyRemaining: Int,
    val proficiency: Int,
    val hitPoints: Breakdown,
    val armourClass: Breakdown,
    val initiative: Int,
    val savingThrows: Set<Ability>,
    /** Bonus de chaque jet de sauvegarde : modificateur, plus la maîtrise si elle est acquise. */
    val savingThrowBonuses: Map<Ability, Int>,
    /** Compétences maîtrisées (historique, race, sous-race, choix valides). */
    val skillProficiencies: Set<Skill>,
    /** Bonus de chaque compétence : modificateur de sa caractéristique, plus la maîtrise si elle est acquise. */
    val skillBonuses: Map<Skill, Int>,
    val spellcasting: List<Spellcasting>,
    val spellSlots: List<Int>,
    val pactSlots: PactSlots?,
    val problems: Set<SheetProblem>,
)

/**
 * Calcule la fiche d'un build. Règles et sources :
 * - caractéristiques : achat de points et bonus ([PointBuy]), puis augmentations des dons, plafonnées comme leur fiche
 *   l'indique (20 pour toutes, [RuleSources.FEATS]) ;
 * - points de vie : [HitPoints], détaillés par classe, avec Robustesse naine et Résilience draconique
 *   ([RuleSources.HIT_POINTS]) et Robuste, +2 par niveau rétroactivement ([RuleSources.TOUGH]) ;
 * - classe d'armure sans équipement : la meilleure formule ([ArmourClass], [RuleSources.ARMOUR_CLASS]) ;
 * - initiative : modificateur de Dextérité ([RuleSources.ABILITIES]) ;
 * - jets de sauvegarde : ceux de la classe de départ, la multiclasse n'en accordant jamais ([RuleSources.CLASSES]),
 *   plus celui du don Résilient ([RuleSources.FEATS]) ;
 * - incantation, emplacements et Magie de pacte : [SpellSlots], [spellSaveDc], [spellAttackBonus] ;
 * - compétences et sauvegardes : modificateur, plus le bonus de maîtrise si elles sont maîtrisées ; une même maîtrise
 *   venue de deux sources n'ajoute rien ([RuleSources.PROFICIENCY]). Choix de compétences : la classe de départ
 *   donne les siens, chaque classe prise en multiclasse les siens, l'humain une compétence au choix (fiches de classe
 *   et de race, data/game). L'Expertise n'est pas encore modélisée.
 */
object CharacterSheetCalculator {
    fun compute(build: Build, data: RulesData, difficulty: DifficultyRules = DifficultyRules.BALANCED): CharacterSheet {
        val problems = mutableSetOf<SheetProblem>()
        if (build.levels.isEmpty()) problems += SheetProblem.NO_LEVEL
        if (build.levels.size > MAX_CHARACTER_LEVEL) problems += SheetProblem.TOO_MANY_LEVELS
        val taken = build.levels.take(MAX_CHARACTER_LEVEL)
        val known = taken.filter { it.classId.value in data.classes }
        if (known.size < taken.size) problems += SheetProblem.UNKNOWN_CLASS
        val level = known.size

        // Niveau de classe atteint à chaque niveau du personnage, et sous-classes choisies.
        val classLevels = LinkedHashMap<String, Int>()
        val classLevelAt = known.map { l -> (classLevels.getOrDefault(l.classId.value, 0) + 1).also { classLevels[l.classId.value] = it } }
        val chosenSubclasses = LinkedHashMap<String, String>()
        known.forEachIndexed { i, l ->
            val subclassId = l.subclassId?.value ?: return@forEachIndexed
            val classProfile = data.classes.getValue(l.classId.value)
            val sub = data.subclasses[subclassId]
            val valid = sub != null && sub.parentClass == classProfile.id && classLevelAt[i] == classProfile.subclassLevel &&
                l.classId.value !in chosenSubclasses
            if (valid) chosenSubclasses[l.classId.value] = subclassId else problems += SheetProblem.SUBCLASS_INVALID
        }
        if (classLevels.any { (id, n) -> n >= data.classes.getValue(id).subclassLevel && id !in chosenSubclasses }) {
            problems += SheetProblem.SUBCLASS_MISSING
        }
        val subclasses = chosenSubclasses.mapValues { (_, s) -> data.subclasses.getValue(s) }

        // Caractéristiques : achat de points, bonus de création, puis dons dans l'ordre des niveaux.
        val major = build.majorBonus
        val minor = build.minorBonus
        if (major == null || minor == null) problems += SheetProblem.BONUSES_MISSING
        if (major != null && major == minor) problems += SheetProblem.BONUSES_SAME_ABILITY
        if (build.baseScores.values.any { it !in PointBuy.MIN..PointBuy.MAX }) problems += SheetProblem.POINT_BUY_OUT_OF_RANGE
        val base = Ability.entries.associateWith { a -> (build.baseScores[a] ?: PointBuy.MIN).coerceIn(PointBuy.MIN, PointBuy.MAX) }
        if (PointBuy.spent(base) > PointBuy.BUDGET) problems += SheetProblem.POINT_BUY_OVER_BUDGET
        val scores = Ability.entries.associateWith { a ->
            base.getValue(a) + (if (a == major) PointBuy.MAJOR_BONUS else 0) + (if (a == minor && minor != major) PointBuy.MINOR_BONUS else 0)
        }.toMutableMap()
        val feats = mutableListOf<Pair<FeatProfile, Map<Ability, Int>>>()
        known.forEachIndexed { i, l ->
            val classProfile = data.classes.getValue(l.classId.value)
            val featLevel = classLevelAt[i] in classProfile.featLevels
            val choice = l.feat
            when {
                choice == null -> if (featLevel) problems += SheetProblem.FEAT_MISSING
                !featLevel || choice.featId.value !in data.feats -> problems += SheetProblem.FEAT_NOT_ALLOWED
                else -> {
                    val feat = data.feats.getValue(choice.featId.value)
                    if (!increaseIsValid(feat.abilityIncrease, choice.abilityIncreases)) {
                        problems += SheetProblem.FEAT_INCREASE_INVALID
                    } else {
                        val cap = feat.abilityIncrease?.cap ?: 0
                        choice.abilityIncreases.forEach { (a, inc) -> scores[a] = maxOf(scores.getValue(a), minOf(cap, scores.getValue(a) + inc)) }
                        feats += feat to choice.abilityIncreases
                    }
                }
            }
        }
        val modifiers = scores.mapValues { (_, v) -> abilityModifier(v) }
        val con = modifiers.getValue(Ability.CONSTITUTION)

        // Points de vie, détaillés : premier niveau, puis les niveaux suivants de chaque classe, puis les capacités.
        val hpTerms = mutableListOf<Term>()
        known.firstOrNull()?.let { first ->
            val profile = data.classes.getValue(first.classId.value)
            hpTerms += Term.FirstLevel(profile.id, profile.hitDie.atFirstLevel, con)
            val later = known.drop(1).groupingBy { it.classId.value }.eachCount()
            for (classId in classLevels.keys) {
                val count = later[classId] ?: continue
                hpTerms += Term.LaterLevels(classId, count, data.classes.getValue(classId).hitDie.perLevel, con)
            }
        }
        val subrace = build.subrace?.value
        if (subrace != null && subrace in data.dwarvenToughnessSubraces && level > 0) hpTerms += Term.PerLevelBonus(subrace, 1, level)
        subclasses.forEach { (classId, s) -> if (s.draconicResilience) hpTerms += Term.PerLevelBonus(s.id, 1, classLevels.getValue(classId)) }
        feats.map { it.first }.filter { it.tough }.distinctBy { it.id }.forEach { hpTerms += Term.PerLevelBonus(it.id, 2, level) }
        if (difficulty.hitPointMultiplier > 1 && hpTerms.isNotEmpty()) {
            val base = hpTerms.sumOf { it.value }
            hpTerms += Term.DifficultyBonus(base * (difficulty.hitPointMultiplier - 1), 100 * (difficulty.hitPointMultiplier - 1))
        }
        if (!difficulty.multiclassing && classLevels.size > 1) problems += SheetProblem.MULTICLASS_DISABLED

        // Classe d'armure sans équipement : la meilleure formule disponible.
        val formulas = buildSet {
            classLevels.keys.forEach { id -> data.classes.getValue(id).unarmoured?.let(::add) }
            if (subclasses.values.any { it.draconicResilience }) add(UnarmouredFormula.MAGE_ARMOUR)
        }

        // Incantation : chaque classe qui l'a obtenue, par elle-même ou par sa sous-classe.
        val prof = if (level > 0) proficiencyBonus(level) + difficulty.proficiencyDelta else 0
        val casters = classLevels.map { (id, n) ->
            val c = data.classes.getValue(id)
            val sub = subclasses[id]?.takeIf { it.progression != CasterProgression.NONE }
            if (sub != null) CasterLevels(sub.progression, n, sub.spellcastingFrom, sub.id) else CasterLevels(c.progression, n, c.spellcastingFrom, id)
        }
        val spellcasting = classLevels.keys.zip(casters).mapNotNull { (id, caster) ->
            val ability = data.classes.getValue(id).spellcastingAbility
            if (ability == null || caster.progression == CasterProgression.NONE || caster.level < caster.spellcastingFrom) return@mapNotNull null
            val mod = modifiers.getValue(ability)
            val c = data.classes.getValue(id)
            Spellcasting(
                classId = id,
                ability = ability,
                modifier = mod,
                saveDc = spellSaveDc(mod, prof),
                attackBonus = spellAttackBonus(mod, prof),
                cantrips = c.cantripsKnown.getOrNull(caster.level - 1),
                known = c.spellsKnown.getOrNull(caster.level - 1),
                prepared = if (c.prepares) preparedSpells(mod, caster.level) else null,
            )
        }
        val pact = classLevels.entries.firstNotNullOfOrNull { (id, n) ->
            data.classes.getValue(id).pactSlots.getOrNull(n - 1)?.let { row ->
                val top = row.indexOfLast { it > 0 }
                if (top < 0) null else PactSlots(row[top], top + 1)
            }
        }

        val startingSaves = known.firstOrNull()?.let { data.classes.getValue(it.classId.value).savingThrows }.orEmpty()
        val resilientSaves = feats.filter { it.first.resilient }.flatMap { it.second.keys }
        val saves = startingSaves + resilientSaves

        // Compétences : d'office (historique, race, sous-race), puis les choix de chaque source.
        val fixed = fixedSkills(build, data)
        val sources = skillSources(build, data)
        if (classLevels.any { (id, n) -> data.classes.getValue(id).unmodelledSkillLevels.any { it <= n } }) problems += SheetProblem.SKILLS_PARTIAL
        val chosen = mutableSetOf<Skill>()
        for (source in sources) {
            val picks = build.skillChoices[source.id].orEmpty()
            if (picks.size < source.count) problems += SheetProblem.SKILLS_MISSING
            val valid = picks.size <= source.count && picks.distinct().size == picks.size &&
                picks.all { it in source && it !in fixed && it !in chosen }
            if (!valid) problems += SheetProblem.SKILLS_INVALID
            chosen += picks.take(source.count).filter { it in source }
        }
        if (build.skillChoices.keys.any { key -> sources.none { it.id == key } }) problems += SheetProblem.SKILLS_INVALID
        val skillProficiencies = fixed + chosen
        val expertise = listOfNotNull(build.background, build.race, build.subrace).mapNotNull { data.origins[it.value] }.flatMap { it.expertise }.toSet()
        return CharacterSheet(
            level = level,
            classLevels = classLevels,
            subclasses = chosenSubclasses,
            scores = scores,
            modifiers = modifiers,
            pointBuyRemaining = PointBuy.BUDGET - PointBuy.spent(base),
            proficiency = prof,
            hitPoints = Breakdown(hpTerms),
            armourClass = Breakdown(armourTerms(modifiers, formulas)),
            // Initiative : Dextérité ([RuleSources.ABILITIES]), plus Vigilant ([RuleSources.ALERT]).
            initiative = modifiers.getValue(Ability.DEXTERITY) + feats.map { it.first }.distinctBy { it.id }.sumOf { it.initiativeBonus },
            savingThrows = saves,
            savingThrowBonuses = Ability.entries.associateWith { a -> modifiers.getValue(a) + if (a in saves) prof else 0 },
            skillProficiencies = skillProficiencies,
            skillBonuses = Skill.entries.associateWith { sk ->
                // L'Expertise double la maîtrise ; maîtrise et Expertise ne se cumulent pas ([RuleSources.PROFICIENCY]).
                modifiers.getValue(sk.ability) + when {
                    sk in expertise && sk in skillProficiencies -> 2 * prof
                    sk in skillProficiencies -> prof
                    else -> 0
                }
            },
            spellcasting = spellcasting,
            spellSlots = if (level > 0) SpellSlots.forClasses(casters) else emptyList(),
            pactSlots = pact,
            problems = problems,
        )
    }

    /** Compétences maîtrisées d'office : historique, race, sous-race. */
    fun fixedSkills(build: Build, data: RulesData): Set<Skill> =
        listOfNotNull(build.background, build.race, build.subrace).mapNotNull { data.origins[it.value] }.flatMap { it.skills }.toSet()

    /**
     * Choix de compétences que le build doit faire, dans l'ordre : la classe de départ, chaque classe prise ensuite
     * (ses choix de multiclasse, s'il y en a), la race qui en laisse choisir.
     */
    fun skillSources(build: Build, data: RulesData): List<SkillSource> = buildList {
        val classes = build.levels.map { it.classId.value }.distinct().filter { it in data.classes }
        classes.forEachIndexed { i, id ->
            val c = data.classes.getValue(id)
            val count = if (i == 0) c.skillChoices else c.multiclassSkillChoices
            if (count > 0) add(SkillSource(id, count, c.skillList))
        }
        build.race?.value?.let { data.origins[it] }?.takeIf { it.skillChoices > 0 }?.let { add(SkillSource(it.id, it.skillChoices, it.skillList)) }
        // Sous-classe qui laisse choisir (Collège du savoir) et don Doué : des compétences parmi toutes.
        build.levels.mapNotNull { it.subclassId?.value }.distinct().mapNotNull { data.subclasses[it] }.filter { it.skillChoices > 0 }
            .forEach { add(SkillSource(it.id, it.skillChoices, emptyList())) }
        build.levels.mapNotNull { it.feat?.featId?.value }.distinct().mapNotNull { data.feats[it] }.filter { it.skillChoices > 0 }
            .forEach { add(SkillSource(it.id, it.skillChoices, emptyList())) }
    }

    /**
     * Répartition conforme à la fiche du don : le montant entier sur une caractéristique permise, ou, si le don le
     * permet (Amélioration de caractéristique), +1 sur deux caractéristiques différentes. Un don sans augmentation
     * n'en accepte aucune.
     */
    private fun increaseIsValid(increase: AbilityIncrease?, chosen: Map<Ability, Int>): Boolean {
        if (increase == null) return chosen.isEmpty()
        if (chosen.values.any { it <= 0 } || chosen.values.sum() != increase.amount) return false
        if (increase.choices.isNotEmpty() && !increase.choices.containsAll(chosen.keys)) return false
        return chosen.size == 1 || (increase.splittable && chosen.values.all { it == 1 })
    }

    /** Détail de la meilleure formule sans armure ; même choix que [ArmourClass.compute]. */
    private fun armourTerms(modifiers: Map<Ability, Int>, formulas: Set<UnarmouredFormula>): List<Term> {
        val dex = Term.AbilityModifier(Ability.DEXTERITY, modifiers.getValue(Ability.DEXTERITY))
        val candidates = buildList {
            add(listOf(Term.Base(10), dex))
            if (UnarmouredFormula.MAGE_ARMOUR in formulas) add(listOf(Term.Base(13, UnarmouredFormula.MAGE_ARMOUR), dex))
            if (UnarmouredFormula.BARBARIAN in formulas) {
                add(listOf(Term.Base(10, UnarmouredFormula.BARBARIAN), Term.AbilityModifier(Ability.CONSTITUTION, modifiers.getValue(Ability.CONSTITUTION)), dex))
            }
            if (UnarmouredFormula.MONK in formulas) {
                add(listOf(Term.Base(10, UnarmouredFormula.MONK), Term.AbilityModifier(Ability.WISDOM, modifiers.getValue(Ability.WISDOM)), dex))
            }
        }
        // À égalité, la formule la plus simple (la première) l'emporte.
        return candidates.maxBy { terms -> terms.sumOf { it.value } }
    }
}

