package fr.carnetdefaerun.feature.tools

import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.rules.BonusDie
import fr.carnetdefaerun.core.rules.RollKind
import fr.carnetdefaerun.core.rules.RollMode
import org.junit.Test

/**
 * Critère de la phase 6 : les probabilités de l'outil égalent une énumération exhaustive, écrite ici sans rien
 * reprendre du moteur. Tout l'espace d'entrée de l'outil est parcouru (types de jet, modes, Chance du halfelin,
 * sauvegardes de concentration, seuils de critique, combinaisons de dés de bonus et niveaux de barde, bonus, cibles,
 * relances), et chaque message « il faut N » est confronté aux faces qui réussissent vraiment.
 */
class ProbabilityExhaustiveTest {
    /** Faces d'un d20, la Chance du halfelin relançant un 1 une fois. */
    private fun die(luck: Boolean): List<Pair<Int, Double>> =
        if (!luck) (1..20).map { it to 0.05 } else (2..20).map { it to 0.05 } + (1..20).map { it to 0.05 * 0.05 }

    /** Face retenue selon le mode : un dé, le meilleur ou le moins bon de deux. */
    private fun kept(mode: RollMode, luck: Boolean): Map<Int, Double> {
        val out = HashMap<Int, Double>()
        if (mode == RollMode.NORMAL) die(luck).forEach { (f, p) -> out.merge(f, p, Double::plus) }
        else for ((a, pa) in die(luck)) for ((b, pb) in die(luck)) out.merge(if (mode == RollMode.ADVANTAGE) maxOf(a, b) else minOf(a, b), pa * pb, Double::plus)
        return out
    }

    /** Toutes les sommes de dés de bonus, par énumération de chaque combinaison de faces. */
    private fun bonusSums(sides: List<Int>): List<Pair<Int, Double>> {
        var combos = listOf(0 to 1.0)
        for (n in sides) combos = combos.flatMap { (s, p) -> (1..n).map { s + it to p / n } }
        return combos
    }

    private fun succeeds(kind: RollKind, face: Int, total: Int, target: Int, crit: Int, autoSaves: Boolean): Boolean {
        val natural = kind != RollKind.SAVING_THROW || autoSaves
        return when {
            natural && face == 1 -> false
            kind == RollKind.ATTACK && face >= crit -> true
            natural && face == 20 -> true
            else -> total >= target
        }
    }

    @Test
    fun `tout l'espace d'entrée de l'outil, contre l'énumération exhaustive`() {
        val keptCache = HashMap<Pair<RollMode, Boolean>, Map<Int, Double>>()
        var cases = 0
        for (kind in RollKind.entries) for (mode in RollMode.entries) for (luck in listOf(false, true)) {
            val faces = keptCache.getOrPut(mode to luck) { kept(mode, luck) }
            val autos = if (kind == RollKind.SAVING_THROW) listOf(false, true) else listOf(false)
            val crits = if (kind == RollKind.ATTACK) 0..3 else 0..0
            val usable = BonusDie.entries.filter { kind in it.kinds }
            val subsets = (0 until (1 shl usable.size)).map { mask -> usable.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet() }
            for (autoSaves in autos) for (improved in crits) for (dice in subsets) {
                val bardLevels = if (BonusDie.BARDIC_INSPIRATION in dice) listOf(1, 5, 10) else listOf(1)
                for (bard in bardLevels) {
                    val sums = bonusSums(dice.map { it.sides(bard) })
                    for (modifier in -3..12) for (target in 4..26) {
                        val crit = 20 - improved
                        var p = 0.0
                        for ((face, pf) in faces) for ((b, pb) in sums) if (succeeds(kind, face, face + modifier + b, target, crit, autoSaves)) p += pf * pb
                        for (rerolls in listOf(0, 2)) {
                            // Relances en séquences explicites : réussir au k-ième essai après k − 1 échecs.
                            var expected = 0.0
                            var failAll = 1.0
                            repeat(rerolls + 1) { expected += failAll * p; failAll *= 1 - p }
                            val input = ProbabilityInput(kind, modifier, target, mode, dice, bard, luck, improved, autoSaves, rerolls)
                            assertWithMessage("$input").that(input.result().success).isWithin(1e-12).of(expected)
                            cases++
                        }
                        // « Il faut N » : comparé aux faces qui réussissent vraiment avec le d20 seul.
                        if (mode == RollMode.NORMAL && !luck && dice.isEmpty()) {
                            val winning = (1..20).filter { succeeds(kind, it, it + modifier, target, crit, autoSaves) }.toSet()
                            val needed = ProbabilityInput(kind, modifier, target, mode, emptySet(), 1, false, improved, autoSaves).result().needed
                            val claimed = when (needed) {
                                is Needed.Face -> (needed.face..20).toSet()
                                Needed.AllButOne -> (2..20).toSet()
                                is Needed.OnlyNatural -> (needed.face..20).toSet()
                                Needed.Always -> (1..20).toSet()
                                Needed.Never -> emptySet()
                            }
                            assertWithMessage("$kind $modifier contre $target, critique $crit, auto $autoSaves : $needed").that(claimed).isEqualTo(winning)
                        }
                    }
                }
            }
        }
        assertWithMessage("cas parcourus").that(cases).isGreaterThan(100_000)
    }
}
