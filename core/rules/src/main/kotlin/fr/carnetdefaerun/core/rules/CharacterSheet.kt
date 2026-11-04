package fr.carnetdefaerun.core.rules

import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.AbilityIncrease

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
)

/** Ce que le moteur doit savoir d'un don. */
data class FeatProfile(
    val id: String,
    val abilityIncrease: AbilityIncrease? = null,
    /** Robuste : +2 points de vie par niveau du personnage. */
    val tough: Boolean = false,
    /** Résilient : maîtrise des jets de sauvegarde de la caractéristique augmentée. */
    val resilient: Boolean = false,
)

/** Données de jeu dont dépend le calcul, tirées de data/game par [RulesData.from]. */
data class RulesData(
    val classes: Map<String, ClassProfile>,
    val subclasses: Map<String, SubclassProfile> = emptyMap(),
    val feats: Map<String, FeatProfile> = emptyMap(),
    /** Sous-races à Robustesse naine (+1 point de vie par niveau du personnage). */
    val dwarvenToughnessSubraces: Set<String> = emptySet(),
) {
    companion object
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
}

data class Breakdown(val terms: List<Term>) {
    val total: Int get() = terms.sumOf { it.value }
}

/** Incantation d'une classe du personnage : sa caractéristique, son DD et son bonus d'attaque. */
data class Spellcasting(val classId: String, val ability: Ability, val modifier: Int, val saveDc: Int, val attackBonus: Int)

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
    /** Sous-classe inconnue, d'une autre classe, ou choisie à un autre niveau que le sien. */
    SUBCLASS_INVALID(true),
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
 * - incantation, emplacements et Magie de pacte : [SpellSlots], [spellSaveDc], [spellAttackBonus].
 */
object CharacterSheetCalculator {
    fun compute(build: Build, data: RulesData): CharacterSheet {
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

        // Classe d'armure sans équipement : la meilleure formule disponible.
        val formulas = buildSet {
            classLevels.keys.forEach { id -> data.classes.getValue(id).unarmoured?.let(::add) }
            if (subclasses.values.any { it.draconicResilience }) add(UnarmouredFormula.MAGE_ARMOUR)
        }

        // Incantation : chaque classe qui l'a obtenue, par elle-même ou par sa sous-classe.
        val prof = if (level > 0) proficiencyBonus(level) else 0
        val casters = classLevels.map { (id, n) ->
            val c = data.classes.getValue(id)
            val sub = subclasses[id]?.takeIf { it.progression != CasterProgression.NONE }
            if (sub != null) CasterLevels(sub.progression, n, sub.spellcastingFrom, sub.id) else CasterLevels(c.progression, n, c.spellcastingFrom, id)
        }
        val spellcasting = classLevels.keys.zip(casters).mapNotNull { (id, caster) ->
            val ability = data.classes.getValue(id).spellcastingAbility
            if (ability == null || caster.progression == CasterProgression.NONE || caster.level < caster.spellcastingFrom) return@mapNotNull null
            val mod = modifiers.getValue(ability)
            Spellcasting(id, ability, mod, spellSaveDc(mod, prof), spellAttackBonus(mod, prof))
        }
        val pact = classLevels.entries.firstNotNullOfOrNull { (id, n) ->
            data.classes.getValue(id).pactSlots.getOrNull(n - 1)?.let { row ->
                val top = row.indexOfLast { it > 0 }
                if (top < 0) null else PactSlots(row[top], top + 1)
            }
        }

        val startingSaves = known.firstOrNull()?.let { data.classes.getValue(it.classId.value).savingThrows }.orEmpty()
        val resilientSaves = feats.filter { it.first.resilient }.flatMap { it.second.keys }
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
            initiative = modifiers.getValue(Ability.DEXTERITY),
            savingThrows = startingSaves + resilientSaves,
            spellcasting = spellcasting,
            spellSlots = if (level > 0) SpellSlots.forClasses(casters) else emptyList(),
            pactSlots = pact,
            problems = problems,
        )
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

