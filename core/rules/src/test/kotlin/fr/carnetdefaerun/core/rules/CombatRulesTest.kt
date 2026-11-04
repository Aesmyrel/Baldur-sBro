package fr.carnetdefaerun.core.rules

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import org.junit.Test

/** Classe d'armure et jets de d20 ; chaque probabilité est recalculée par énumération brute, indépendante du moteur. */
class CombatRulesTest {

    @Test
    fun `classe d'armure, armures et formules sans armure`() {
        // Armure légère : base + Dextérité entière ; intermédiaire : +2 au plus (+3 avec le don) ; lourde : sans Dextérité.
        assertThat(ArmourClass.compute(dexterity = 4, armour = WornArmour(12, ArmourProficiency.LIGHT))).isEqualTo(16)
        assertThat(ArmourClass.compute(dexterity = 4, armour = WornArmour(14, ArmourProficiency.MEDIUM))).isEqualTo(16)
        assertThat(ArmourClass.compute(dexterity = 4, armour = WornArmour(14, ArmourProficiency.MEDIUM), mediumArmourMaster = true)).isEqualTo(17)
        assertThat(ArmourClass.compute(dexterity = 4, armour = WornArmour(14, ArmourProficiency.MEDIUM, fullDexterity = true))).isEqualTo(18)
        assertThat(ArmourClass.compute(dexterity = 3, armour = WornArmour(18, ArmourProficiency.HEAVY), shieldBonus = 2)).isEqualTo(20)
        // Sans armure : 10 + Dextérité, ou la meilleure formule disponible.
        assertThat(ArmourClass.compute(dexterity = 2)).isEqualTo(12)
        assertThat(ArmourClass.compute(dexterity = 2, unarmoured = setOf(UnarmouredFormula.MAGE_ARMOUR))).isEqualTo(15)
        assertThat(ArmourClass.compute(dexterity = 2, constitution = 3, unarmoured = setOf(UnarmouredFormula.BARBARIAN), shieldBonus = 2)).isEqualTo(17)
        assertThat(ArmourClass.compute(dexterity = 3, wisdom = 3, unarmoured = setOf(UnarmouredFormula.MONK))).isEqualTo(16)
        // Le moine perd sa formule avec un bouclier.
        assertThat(ArmourClass.compute(dexterity = 3, wisdom = 3, unarmoured = setOf(UnarmouredFormula.MONK), shieldBonus = 2)).isEqualTo(15)
        assertThat(ArmourClass.compute(dexterity = 1, otherBonuses = 1)).isEqualTo(12)
    }

    /** Énumération brute : toutes les paires de dés, la relance du halfelin comprise. */
    private fun bruteForce(kind: RollKind, modifier: Int, target: Int, mode: RollMode, crit: Int, luck: Boolean, autoSaves: Boolean = false): Double {
        fun outcomes(): List<Pair<Int, Double>> =
            if (!luck) (1..20).map { it to 1.0 / 20 }
            else (1..20).flatMap { a -> if (a != 1) listOf(a to 1.0 / 20) else (1..20).map { b -> b to 1.0 / 400 } }
        val dice = when (mode) {
            RollMode.NORMAL -> outcomes()
            else -> outcomes().flatMap { (a, pa) -> outcomes().map { (b, pb) -> (if (mode == RollMode.ADVANTAGE) maxOf(a, b) else minOf(a, b)) to pa * pb } }
        }
        val natural = kind != RollKind.SAVING_THROW || autoSaves
        return dice.filter { (f, _) ->
            when {
                natural && f == 1 -> false
                kind == RollKind.ATTACK && f >= crit -> true
                natural && f == 20 -> true
                else -> f + modifier >= target
            }
        }.sumOf { it.second }
    }

    @Test
    fun `probabilités exactes, comparées à l'énumération brute`() {
        for (kind in RollKind.entries) for (mode in RollMode.entries) for (luck in listOf(false, true)) for (modifier in -3..12) for (target in 5..25) {
            val crit = if (kind == RollKind.ATTACK) 19 else 20
            assertThat(D20.successChance(kind, modifier, target, mode, crit, luck))
                .isWithin(1e-12).of(bruteForce(kind, modifier, target, mode, crit, luck))
        }
    }

    @Test
    fun `1 et 20 naturels selon le jet`() {
        // Attaque : même avec +30, un 1 rate ; même avec −10 contre 30, un 20 touche.
        assertThat(D20.successChance(RollKind.ATTACK, 30, 10)).isWithin(1e-12).of(19.0 / 20)
        assertThat(D20.successChance(RollKind.ATTACK, -10, 30)).isWithin(1e-12).of(1.0 / 20)
        // Sauvegarde : ni réussite ni échec automatiques…
        assertThat(D20.successChance(RollKind.SAVING_THROW, 30, 10)).isWithin(1e-12).of(1.0)
        assertThat(D20.successChance(RollKind.SAVING_THROW, -10, 30)).isWithin(1e-12).of(0.0)
        // … sauf pour la concentration ou en dialogue.
        assertThat(D20.successChance(RollKind.SAVING_THROW, -10, 30, autoSaves = true)).isWithin(1e-12).of(1.0 / 20)
    }

    @Test
    fun `avantage, critiques et valeur passive`() {
        assertThat(RollMode.of(advantage = true, disadvantage = true)).isEqualTo(RollMode.NORMAL)
        assertThat(D20.criticalChance()).isWithin(1e-12).of(1.0 / 20)
        assertThat(D20.criticalChance(critThreshold = 19)).isWithin(1e-12).of(2.0 / 20)
        // Avantage : 1 − (19/20)², désavantage : (1/20)².
        assertThat(D20.criticalChance(RollMode.ADVANTAGE)).isWithin(1e-12).of(1 - (19.0 / 20) * (19.0 / 20))
        assertThat(D20.criticalChance(RollMode.DISADVANTAGE)).isWithin(1e-12).of(1.0 / 400)
        assertThat(D20.passive(4)).isEqualTo(14)
        assertThat(D20.passive(4, RollMode.ADVANTAGE)).isEqualTo(19)
        assertThat(D20.passive(4, RollMode.DISADVANTAGE)).isEqualTo(9)
    }
}
