package fr.carnetdefaerun.core.rules

import fr.carnetdefaerun.core.model.content.ArmourProficiency

/** Armure de corps portée : sa classe d'armure de base, son poids et, pour quelques armures, la Dextérité entière. */
data class WornArmour(val baseClass: Int, val weight: ArmourProficiency, val fullDexterity: Boolean = false)

/** Formules d'un personnage sans armure (« Other formulas »). */
enum class UnarmouredFormula {
    /** Armure du mage ou Résilience draconique : 13 + Dextérité + bouclier. */
    MAGE_ARMOUR,

    /** Défense sans armure du barbare : 10 + Constitution + Dextérité + bouclier. */
    BARBARIAN,

    /** Défense sans armure du moine : 10 + Sagesse + Dextérité, sans bouclier. */
    MONK,
}

/**
 * Classe d'armure. Source : [RuleSources.ARMOUR_CLASS], sections « Formulas » et « Other formulas » :
 * - avec une armure : sa base + Dextérité, plafonnée à +2 en armure intermédiaire (+3 avec le don Maître des armures
 *   intermédiaires), ignorée en armure lourde ; quelques armures rares donnent la Dextérité entière (« Exotic Material ») ;
 * - sans armure : 10 + Dextérité, ou la plus haute des formules alternatives dont le personnage dispose ;
 *   celle du moine cesse de fonctionner avec un bouclier ;
 * - bouclier et autres bonus s'ajoutent.
 */
object ArmourClass {
    fun compute(
        dexterity: Int,
        armour: WornArmour? = null,
        shieldBonus: Int = 0,
        constitution: Int = 0,
        wisdom: Int = 0,
        unarmoured: Set<UnarmouredFormula> = emptySet(),
        mediumArmourMaster: Boolean = false,
        otherBonuses: Int = 0,
    ): Int {
        val body = if (armour != null) {
            val dex = when {
                armour.fullDexterity -> dexterity
                armour.weight == ArmourProficiency.HEAVY -> 0
                armour.weight == ArmourProficiency.MEDIUM -> minOf(dexterity, if (mediumArmourMaster) 3 else 2)
                else -> dexterity
            }
            armour.baseClass + dex + shieldBonus
        } else {
            val formulas = buildList {
                add(10 + dexterity + shieldBonus)
                if (UnarmouredFormula.MAGE_ARMOUR in unarmoured) add(13 + dexterity + shieldBonus)
                if (UnarmouredFormula.BARBARIAN in unarmoured) add(10 + constitution + dexterity + shieldBonus)
                if (UnarmouredFormula.MONK in unarmoured && shieldBonus == 0) add(10 + wisdom + dexterity)
            }
            formulas.max()
        }
        return body + otherBonuses
    }
}

/** Sorte de jet de d20 : elle décide des 1 et des 20 naturels. */
enum class RollKind { ATTACK, ABILITY_CHECK, SAVING_THROW }

/** Avantage et désavantage ne se cumulent pas et s'annulent. */
enum class RollMode {
    NORMAL, ADVANTAGE, DISADVANTAGE;

    companion object {
        fun of(advantage: Boolean, disadvantage: Boolean): RollMode = when {
            advantage == disadvantage -> NORMAL
            advantage -> ADVANTAGE
            else -> DISADVANTAGE
        }
    }
}

/**
 * Probabilités exactes d'un jet de d20 : chaque face est énumérée, rien n'est simulé.
 * Source : [RuleSources.DICE_ROLLS], sections « Natural 1s and 20s », « Advantage and disadvantage »,
 * « Critical hits » et « Critical misses ».
 * - Jet d'attaque et test de caractéristique : un 1 naturel échoue toujours, un 20 naturel réussit toujours.
 * - Jet de sauvegarde : ni l'un ni l'autre, sauf pour la concentration ou en dialogue ([autoSaves]).
 * - Attaque : un résultat naturel au moins égal au seuil de critique est un coup critique, qui touche toujours.
 * - Avantage : le meilleur de deux dés ; désavantage : le moins bon.
 * - Chance du halfelin : un 1 naturel est relancé. La page ne précise pas le cas d'un jet avec avantage ; on applique
 *   la relance à chaque dé, comme dans les règles de D&D (interprétation signalée, D-085).
 */
object D20 {
    /** Probabilité de chaque face (indices 1 à 20) d'un dé, puis du résultat retenu selon le mode. */
    fun faceProbabilities(mode: RollMode = RollMode.NORMAL, halflingLuck: Boolean = false): DoubleArray {
        val die = DoubleArray(21)
        for (f in 1..20) die[f] = 1.0 / 20
        if (halflingLuck) {
            // Le 1 est relancé une fois : il ne reste qu'avec probabilité 1/20 × 1/20.
            for (f in 1..20) die[f] = (if (f == 1) 0.0 else 1.0 / 20) + (1.0 / 20) * (1.0 / 20)
        }
        if (mode == RollMode.NORMAL) return die
        val kept = DoubleArray(21)
        for (a in 1..20) for (b in 1..20) {
            val face = if (mode == RollMode.ADVANTAGE) maxOf(a, b) else minOf(a, b)
            kept[face] += die[a] * die[b]
        }
        return kept
    }

    /** Probabilité de réussir un jet de [modifier] contre [target] (DD ou classe d'armure). */
    fun successChance(
        kind: RollKind,
        modifier: Int,
        target: Int,
        mode: RollMode = RollMode.NORMAL,
        critThreshold: Int = 20,
        halflingLuck: Boolean = false,
        autoSaves: Boolean = false,
    ): Double {
        val faces = faceProbabilities(mode, halflingLuck)
        val natural = kind != RollKind.SAVING_THROW || autoSaves
        var p = 0.0
        for (f in 1..20) {
            val success = when {
                natural && f == 1 -> false
                kind == RollKind.ATTACK && f >= critThreshold -> true
                natural && f == 20 -> true
                else -> f + modifier >= target
            }
            if (success) p += faces[f]
        }
        return p
    }

    /** Probabilité d'un coup critique (résultat naturel au moins égal au seuil). */
    fun criticalChance(mode: RollMode = RollMode.NORMAL, critThreshold: Int = 20, halflingLuck: Boolean = false): Double {
        val faces = faceProbabilities(mode, halflingLuck)
        return (critThreshold.coerceIn(2, 20)..20).sumOf { faces[it] }
    }

    /** Valeur passive d'une compétence : 10 + bonus, ±5 avec avantage ou désavantage. Source : [RuleSources.ADVANTAGE]. */
    fun passive(modifier: Int, mode: RollMode = RollMode.NORMAL): Int = 10 + modifier + when (mode) {
        RollMode.NORMAL -> 0
        RollMode.ADVANTAGE -> 5
        RollMode.DISADVANTAGE -> -5
    }
}
