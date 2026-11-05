package fr.carnetdefaerun.feature.builds

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.Skill
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import org.junit.Test

class PartyCoverageTest {
    private val rules = BuildFixtures.rules
    private val mage = CharacterSheetCalculator.compute(BuildFixtures.battleMage, rules)
    private val pact = CharacterSheetCalculator.compute(BuildFixtures.pact, rules)

    @Test
    fun `meilleur membre par jet, maîtrise signalée`() {
        val coverage = partyCoverage(listOf(null, mage, null, pact))
        // Intelligence : le magicien (Intelligence 20, +5, sans maîtrise de la sauvegarde : il a commencé guerrier).
        assertThat(coverage.saves[Ability.INTELLIGENCE]).isEqualTo(Best(1, 5, proficient = false))
        // Constitution : le magicien (+3, maîtrisée par le guerrier de départ : +7) contre l'occultiste (+2).
        // Charisme : l'occultiste maîtrise la sauvegarde (Charisme 19, +4, maîtrise +3).
        assertThat(coverage.saves[Ability.CHARISMA]).isEqualTo(Best(3, 7, proficient = true, anyProficient = true))
        // Arcanes : les deux maîtrisent ; le plus haut bonus l'emporte (Intelligence +5 et maîtrise +4).
        assertThat(coverage.skills[Skill.ARCANA]).isEqualTo(Best(1, 9, proficient = true, anyProficient = true))
        // Discrétion : personne ne la maîtrise, le meilleur bonus reste montré.
        assertThat(coverage.skills[Skill.STEALTH]?.proficient).isFalse()
    }

    @Test
    fun `groupe vide`() {
        val coverage = partyCoverage(listOf(null, null, null, null))
        assertThat(coverage.saves.values.all { it == null }).isTrue()
        assertThat(coverage.skills.values.all { it == null }).isTrue()
    }

    @Test
    fun `un membre non maîtrisé peut avoir le meilleur bonus sans cacher la maîtrise d'un autre`() {
        // Cas construit : le magicien garde son bonus d'Arcanes (+9) mais sans la maîtrise ; l'occultiste la maîtrise
        // (Intelligence 8, −1, maîtrise +3 : +2). Le meilleur bonus reste au magicien, et la maîtrise existe dans le groupe.
        val strongButUntrained = mage.copy(skillProficiencies = mage.skillProficiencies - Skill.ARCANA)
        val coverage = partyCoverage(listOf(strongButUntrained, pact))
        val best = coverage.skills.getValue(Skill.ARCANA)!!
        assertThat(best.slot).isEqualTo(0)
        assertThat(best.proficient).isFalse()
        assertThat(best.anyProficient).isTrue()
    }
}
