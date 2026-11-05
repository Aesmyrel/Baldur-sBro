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
        // Charisme : l'occultiste maîtrise la sauvegarde (Charisme 19, +4, maîtrise +3).
        assertThat(coverage.saves[Ability.CHARISMA]).isEqualTo(Best(3, 7, proficient = true))
        // Arcanes : les deux maîtrisent ; le plus haut bonus l'emporte (Intelligence +5 et maîtrise +4).
        assertThat(coverage.skills[Skill.ARCANA]).isEqualTo(Best(1, 9, proficient = true))
        // Discrétion : personne ne la maîtrise, le meilleur bonus reste montré.
        assertThat(coverage.skills[Skill.STEALTH]?.proficient).isFalse()
    }

    @Test
    fun `groupe vide`() {
        val coverage = partyCoverage(listOf(null, null, null, null))
        assertThat(coverage.saves.values.all { it == null }).isTrue()
        assertThat(coverage.skills.values.all { it == null }).isTrue()
    }
}
