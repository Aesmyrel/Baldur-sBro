package fr.carnetdefaerun.feature.tools

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.rules.BonusDie
import fr.carnetdefaerun.core.rules.RollKind
import fr.carnetdefaerun.core.rules.RollMode
import org.junit.Test

class ProbabilityTest {
    @Test
    fun `ce qu'il faut au dé, selon le type de jet`() {
        assertThat(ProbabilityInput(RollKind.ABILITY_CHECK, modifier = 3, target = 15).result().needed).isEqualTo(Needed.Face(12))
        assertThat(ProbabilityInput(RollKind.ABILITY_CHECK, modifier = 10, target = 5).result().needed).isEqualTo(Needed.AllButOne)
        assertThat(ProbabilityInput(RollKind.ABILITY_CHECK, modifier = 0, target = 25).result().needed).isEqualTo(Needed.OnlyNatural(20))
        // Attaque contre une CA hors de portée : seul le seuil de critique touche.
        assertThat(ProbabilityInput(RollKind.ATTACK, modifier = 0, target = 30, improvedCriticals = 1).result().needed).isEqualTo(Needed.OnlyNatural(19))
        // Sauvegarde ordinaire : ni 1 ni 20 naturels.
        assertThat(ProbabilityInput(RollKind.SAVING_THROW, modifier = 10, target = 5).result().needed).isEqualTo(Needed.Always)
        assertThat(ProbabilityInput(RollKind.SAVING_THROW, modifier = 0, target = 25).result().needed).isEqualTo(Needed.Never)
        assertThat(ProbabilityInput(RollKind.SAVING_THROW, modifier = 0, target = 25).result().success).isEqualTo(0.0)
        assertThat(ProbabilityInput(RollKind.SAVING_THROW, modifier = 0, target = 25, autoSaves = true).result().success).isWithin(1e-12).of(1.0 / 20)
    }

    @Test
    fun `dés de bonus filtrés par type de jet, critique pour les attaques seulement`() {
        val check = ProbabilityInput(RollKind.ABILITY_CHECK, bonusDice = setOf(BonusDie.GUIDANCE, BonusDie.BLESS))
        assertThat(check.appliedDice).containsExactly(BonusDie.GUIDANCE)
        assertThat(check.result().critical).isNull()
        val attack = ProbabilityInput(RollKind.ATTACK, modifier = 5, target = 15, mode = RollMode.ADVANTAGE, bonusDice = setOf(BonusDie.BLESS, BonusDie.GUIDANCE))
        assertThat(attack.appliedDice).containsExactly(BonusDie.BLESS)
        // Avantage : 1 − (19/20)² de critique.
        assertThat(attack.result().critical).isWithin(1e-12).of(1 - (19.0 / 20) * (19.0 / 20))
        // Test à +3 contre DD 15 : 9/20 = 45 % ; avec Assistance, (9 + 2,5)/20 = 57,5 %.
        assertThat(ProbabilityInput(RollKind.ABILITY_CHECK, 3, 15).result().success).isWithin(1e-12).of(0.45)
        assertThat(ProbabilityInput(RollKind.ABILITY_CHECK, 3, 15, bonusDice = setOf(BonusDie.GUIDANCE)).result().success).isWithin(1e-12).of(0.575)
    }

    @Test
    fun `pourcentages à la française`() {
        assertThat(percent(0.575)).isEqualTo("58")
        assertThat(percent(0.05)).isEqualTo("5,0")
        assertThat(percent(0.9975)).isEqualTo("99,8")
        assertThat(percent(0.0)).isEqualTo("0")
        assertThat(percent(1.0)).isEqualTo("100")
        assertThat(percent(0.99999)).isEqualTo("> 99,9")
        assertThat(percent(0.000001)).isEqualTo("< 0,1")
    }
}
