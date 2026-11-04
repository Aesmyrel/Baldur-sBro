package fr.carnetdefaerun.core.rules

import fr.carnetdefaerun.core.model.content.Ability

/**
 * Modificateur d'une valeur de caractéristique : (valeur − 10) / 2 arrondi vers le bas, sans dépasser +10.
 * Source : [RuleSources.ABILITIES], section « Ability score modifiers ».
 */
fun abilityModifier(score: Int): Int = Math.floorDiv(score - 10, 2).coerceAtMost(10)

/**
 * Achat de points à la création du personnage. Source : [RuleSources.CHARACTER_CREATION], section « Ability scores » :
 * 27 points ; chaque valeur entre 8 et 15 avant bonus ; chaque point jusqu'à 13 coûte 1, 14 et 15 coûtent 2 chacun ;
 * puis un +2 et un +1. La page précise que la valeur de départ peut atteindre 17 au plus : les deux bonus vont donc
 * à deux caractéristiques différentes (15 + 2), jamais à la même.
 */
object PointBuy {
    const val BUDGET = 27
    const val MIN = 8
    const val MAX = 15
    const val MAJOR_BONUS = 2
    const val MINOR_BONUS = 1

    /** Coût d'une valeur, depuis 8. */
    fun cost(score: Int): Int {
        require(score in MIN..MAX) { "Valeur hors de l'achat de points : $score" }
        return (minOf(score, 13) - MIN) + 2 * maxOf(0, score - 13)
    }

    fun spent(base: Map<Ability, Int>): Int = Ability.entries.sumOf { cost(base[it] ?: MIN) }

    fun remaining(base: Map<Ability, Int>): Int = BUDGET - spent(base)

    /** Ce qui rend une répartition invalide. */
    enum class Problem { OUT_OF_RANGE, OVER_BUDGET, SAME_ABILITY_BONUSES }

    fun problems(base: Map<Ability, Int>, major: Ability, minor: Ability): Set<Problem> = buildSet {
        if (base.values.any { it !in MIN..MAX }) add(Problem.OUT_OF_RANGE)
        else if (spent(base) > BUDGET) add(Problem.OVER_BUDGET)
        if (major == minor) add(Problem.SAME_ABILITY_BONUSES)
    }

    /** Valeurs de départ, bonus compris. */
    fun finalScores(base: Map<Ability, Int>, major: Ability, minor: Ability): Map<Ability, Int> =
        Ability.entries.associateWith { a ->
            (base[a] ?: MIN) + (if (a == major) MAJOR_BONUS else 0) + (if (a == minor) MINOR_BONUS else 0)
        }
}
