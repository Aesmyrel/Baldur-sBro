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

    /** Énumération brute avec dés de bonus : chaque face du d20 croisée avec chaque combinaison de faces des dés. */
    private fun bruteForceWithDice(kind: RollKind, modifier: Int, target: Int, mode: RollMode, dice: List<Int>): Double {
        var combos = listOf(0 to 1.0)
        for (n in dice) combos = combos.flatMap { (s, p) -> (1..n).map { s + it to p / n } }
        return combos.sumOf { (b, pb) -> pb * bruteForce(kind, modifier + b, target, mode, 20, false) }
    }

    @Test
    fun `dés de bonus convolués exactement, 1 et 20 naturels gardés`() {
        for (kind in RollKind.entries) for (mode in RollMode.entries) for (dice in listOf(listOf(4), listOf(4, 6), listOf(10))) for (modifier in -2..8) for (target in 8..24) {
            assertThat(D20.successChance(kind, modifier, target, mode, bonusDice = dice))
                .isWithin(1e-12).of(bruteForceWithDice(kind, modifier, target, mode, dice))
        }
        // Un dé de bonus ne sauve pas un 1 naturel en attaque : +0 et 1d4 contre 2 réussit 19 fois sur 20.
        assertThat(D20.successChance(RollKind.ATTACK, 0, 2, bonusDice = listOf(4))).isWithin(1e-12).of(19.0 / 20)
        // Test à +2 contre DD 15 : 8/20 seul ; avec Assistance, moyenne sur le d4 de (8 + b)/20, soit 10,5/20.
        assertThat(D20.successChance(RollKind.ABILITY_CHECK, 2, 15)).isWithin(1e-12).of(8.0 / 20)
        assertThat(D20.successChance(RollKind.ABILITY_CHECK, 2, 15, bonusDice = listOf(BonusDie.GUIDANCE.sides()))).isWithin(1e-12).of(10.5 / 20)
        assertThat(Dice.sumDistribution(listOf(6, 6))[7]).isWithin(1e-12).of(6.0 / 36)
        assertThat(Dice.sumDistribution(emptyList())).containsExactly(0, 1.0)
    }

    @Test
    fun `dés de bonus de BG3, jets concernés et taille`() {
        assertThat(BonusDie.GUIDANCE.kinds).containsExactly(RollKind.ABILITY_CHECK)
        assertThat(BonusDie.BLESS.kinds).containsExactly(RollKind.ATTACK, RollKind.SAVING_THROW)
        assertThat(BonusDie.BARDIC_INSPIRATION.kinds).containsExactly(RollKind.ATTACK, RollKind.ABILITY_CHECK, RollKind.SAVING_THROW)
        assertThat((1..12).map { BonusDie.BARDIC_INSPIRATION.sides(it) }).containsExactly(6, 6, 6, 6, 8, 8, 8, 8, 8, 10, 10, 10).inOrder()
        assertThat(BonusDie.BLESS.sides()).isEqualTo(4)
    }

    @Test
    fun `relances d'Inspiration et seuil de critique`() {
        // 50 % de réussite : une relance donne 75 %, deux 87,5 %.
        assertThat(D20.successChance(RollKind.ABILITY_CHECK, 0, 11, inspirationRerolls = 1)).isWithin(1e-12).of(0.75)
        assertThat(D20.successChance(RollKind.ABILITY_CHECK, 0, 11, inspirationRerolls = 2)).isWithin(1e-12).of(0.875)
        // La relance garde l'avantage : 1 − (1 − p_avantage)².
        val adv = D20.successChance(RollKind.ABILITY_CHECK, 0, 11, RollMode.ADVANTAGE)
        assertThat(D20.successChance(RollKind.ABILITY_CHECK, 0, 11, RollMode.ADVANTAGE, inspirationRerolls = 1)).isWithin(1e-12).of(1 - (1 - adv) * (1 - adv))
        // Coup critique amélioré : 19, puis 18 s'il se cumule ; jamais sous 2.
        assertThat(D20.critThreshold(0)).isEqualTo(20)
        assertThat(D20.critThreshold(1)).isEqualTo(19)
        assertThat(D20.critThreshold(2)).isEqualTo(18)
        assertThat(D20.critThreshold(30)).isEqualTo(2)
        assertThat(D20.criticalChance(critThreshold = D20.critThreshold(1))).isWithin(1e-12).of(2.0 / 20)
    }

    @Test
    fun `attaques d'arme, exemples de la page`() {
        val maul = WeaponTraits()
        val rapier = WeaponTraits(finesse = true)
        val longbow = WeaponTraits(ranged = true)
        val handaxe = WeaponTraits(thrown = true)
        val dagger = WeaponTraits(finesse = true, thrown = true)
        val (str, dex) = 10 to 16
        assertThat(WeaponAttack.attackAbility(maul, AttackMode.MELEE, str, dex)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.STRENGTH)
        assertThat(WeaponAttack.attackAbility(rapier, AttackMode.MELEE, str, dex)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.DEXTERITY)
        assertThat(WeaponAttack.attackAbility(rapier, AttackMode.MELEE, 16, 10)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.STRENGTH)
        assertThat(WeaponAttack.attackAbility(longbow, AttackMode.RANGED, str, dex)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.DEXTERITY)
        assertThat(WeaponAttack.attackAbility(handaxe, AttackMode.THROWN, str, dex)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.STRENGTH)
        // Dague lancée : Force pour l'attaque, la plus haute pour les dégâts.
        assertThat(WeaponAttack.attackAbility(dagger, AttackMode.THROWN, str, dex)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.STRENGTH)
        assertThat(WeaponAttack.damageAbility(dagger, AttackMode.THROWN, str, dex)).isEqualTo(fr.carnetdefaerun.core.model.content.Ability.DEXTERITY)
        // Rapière, Dextérité 16 (+3), maîtrisée à +2 : +5 ; non maîtrisée : +3.
        val scores = fr.carnetdefaerun.core.model.content.Ability.entries.associateWith { 10 } + (fr.carnetdefaerun.core.model.content.Ability.DEXTERITY to 16)
        assertThat(WeaponAttack.attackBonus(rapier, AttackMode.MELEE, scores, proficient = true, proficiency = 2)).isEqualTo(5)
        assertThat(WeaponAttack.attackBonus(rapier, AttackMode.MELEE, scores, proficient = false, proficiency = 2)).isEqualTo(3)
    }
}
