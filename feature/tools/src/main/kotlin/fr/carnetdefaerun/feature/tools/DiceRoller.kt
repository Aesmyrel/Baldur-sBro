package fr.carnetdefaerun.feature.tools

import kotlin.random.Random

/** Faces des dés proposés ; le d100 est un dé de pourcentage, tiré de 1 à 100. */
val DIE_SIDES = listOf(4, 6, 8, 10, 12, 20, 100)

/** Dés à lancer : nombre de dés par nombre de faces, plus un modificateur. */
data class DicePool(val counts: Map<Int, Int> = emptyMap(), val modifier: Int = 0) {
    val dice: List<Int> get() = DIE_SIDES.flatMap { sides -> List(counts[sides] ?: 0) { sides } }
    val isEmpty: Boolean get() = dice.isEmpty()

    fun add(sides: Int): DicePool = if (dice.size >= MAX_DICE) this else copy(counts = counts + (sides to (counts[sides] ?: 0) + 1))
    fun remove(sides: Int): DicePool {
        val n = (counts[sides] ?: 0) - 1
        return copy(counts = if (n <= 0) counts - sides else counts + (sides to n))
    }

    /** Minimum, maximum et moyenne exacte de la somme. */
    val minimum: Int get() = dice.size + modifier
    val maximum: Int get() = dice.sum() + modifier
    /** Moyenne exacte : celle d'un dé à s faces vaut (s + 1) / 2, et les moyennes s'additionnent. */
    val mean: Double get() = dice.sumOf { (it + 1) / 2.0 } + modifier

    val isFull: Boolean get() = dice.size >= MAX_DICE

    companion object {
        const val MAX_DICE = 12
    }
}

/** Un lancer : la face obtenue pour chaque dé, dans l'ordre du réservoir, et le total. */
data class DiceRoll(val pool: DicePool, val faces: List<Int>) {
    val total: Int get() = faces.sum() + pool.modifier
}

fun DicePool.roll(random: Random = Random.Default): DiceRoll = DiceRoll(this, dice.map { random.nextInt(1, it + 1) })
