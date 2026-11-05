package fr.carnetdefaerun.feature.builds

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.rules.CharacterSheetCalculator
import org.junit.Test

/** Éditions de l'atelier : chaque choix reste au niveau où le jeu le place. */
class BuildEditingTest {
    private val rules = BuildFixtures.rules
    private val catalog by lazy { BuildFixtures.catalog() }
    private fun id(value: String) = ContentId(value)

    @Test
    fun `le build de la maquette donne les valeurs de la maquette`() {
        val sheet = CharacterSheetCalculator.compute(BuildFixtures.battleMage, rules)
        assertThat(sheet.problems).isEmpty()
        assertThat(sheet.hitPoints.total).isEqualTo(92)
        assertThat(sheet.scores).containsExactlyEntriesIn(
            mapOf(Ability.STRENGTH to 8, Ability.DEXTERITY to 15, Ability.CONSTITUTION to 16, Ability.INTELLIGENCE to 20, Ability.WISDOM to 10, Ability.CHARISMA to 8),
        )
        assertThat(sheet.spellcasting.single().saveDc).isEqualTo(17)
        assertThat(sheet.spellcasting.single().attackBonus).isEqualTo(9)
        assertThat(sheet.pointBuyRemaining).isEqualTo(0)
        assertThat(CharacterSheetCalculator.compute(BuildFixtures.pact, rules).problems).isEmpty()
    }

    @Test
    fun `changer la classe d'un niveau replace sous-classe et dons`() {
        // Le premier niveau passe de guerrier à magicien : la sous-classe d'évocation recule d'un niveau.
        val edited = BuildEdits.setLevelClass(BuildFixtures.battleMage, 0, id("class.wizard"), rules)
        val subclassAt = edited.levels.indexOfFirst { it.subclassId != null }
        assertThat(subclassAt).isEqualTo(2) // troisième niveau : magicien 2
        assertThat(edited.levels[subclassAt].subclassId).isEqualTo(id("subclass.evocation-school"))
        // Les deux dons du magicien suivent ses niveaux 4 et 8.
        assertThat(edited.levels.mapIndexedNotNull { i, l -> if (l.feat != null) i else null }).containsExactly(4, 8).inOrder()
        assertThat(CharacterSheetCalculator.compute(edited, rules).problems).isEmpty()
    }

    @Test
    fun `retirer des niveaux abandonne les choix sans place, les rajouter les redemande`() {
        var build = BuildFixtures.battleMage
        repeat(9) { build = BuildEdits.removeLastLevel(build, rules) }
        assertThat(build.levels).hasSize(3)
        assertThat(build.levels.mapNotNull { it.feat }).isEmpty()
        assertThat(build.levels.all { it.subclassId == null }).isTrue()
        build = BuildEdits.addLevel(build, id("class.wizard"), rules)
        assertThat(CharacterSheetCalculator.compute(build, rules).problems).contains(fr.carnetdefaerun.core.rules.SheetProblem.SUBCLASS_MISSING)
        build = BuildEdits.setSubclass(build, "class.wizard", id("subclass.abjuration-school"), rules)
        assertThat(build.levels[3].subclassId).isEqualTo(id("subclass.abjuration-school"))
        // Pas plus de 12 niveaux.
        repeat(20) { build = BuildEdits.addLevel(build, id("class.wizard"), rules) }
        assertThat(build.levels).hasSize(12)
    }

    @Test
    fun `une sous-classe d'une autre classe est refusée`() {
        val build = Build(name = "x", levels = List(3) { BuildLevel(id("class.fighter")) })
        assertThat(BuildEdits.setSubclass(build, "class.fighter", id("subclass.the-fiend"), rules).levels.all { it.subclassId == null }).isTrue()
    }

    @Test
    fun `bonus +2 et +1, achat de points borné`() {
        val build = Build(name = "x", majorBonus = Ability.STRENGTH, minorBonus = Ability.DEXTERITY)
        assertThat(BuildEdits.setMajor(build, Ability.DEXTERITY).let { it.majorBonus to it.minorBonus }).isEqualTo(Ability.DEXTERITY to Ability.STRENGTH)
        assertThat(BuildEdits.setMinor(build, Ability.STRENGTH).let { it.majorBonus to it.minorBonus }).isEqualTo(Ability.DEXTERITY to Ability.STRENGTH)
        var b = build
        for (a in listOf(Ability.STRENGTH, Ability.DEXTERITY, Ability.CONSTITUTION)) repeat(7) { if (BuildEdits.canIncrease(b, a)) b = BuildEdits.setBase(b, a, b.baseScores.getValue(a) + 1) }
        assertThat(b.baseScores.filterValues { it > 8 }).containsExactly(Ability.STRENGTH, 15, Ability.DEXTERITY, 15, Ability.CONSTITUTION, 15)
        assertThat(BuildEdits.canIncrease(b, Ability.WISDOM)).isFalse() // budget épuisé
        assertThat(BuildEdits.setBase(b, Ability.STRENGTH, 99).baseScores[Ability.STRENGTH]).isEqualTo(15)
    }

    @Test
    fun `répartition des dons`() {
        // Amélioration : +1 et +1, ou +2 au même endroit.
        var asi = emptyMap<Ability, Int>()
        asi = BuildEdits.toggleIncrease(asi, Ability.STRENGTH, 2, emptyList(), splittable = true)
        asi = BuildEdits.toggleIncrease(asi, Ability.STRENGTH, 2, emptyList(), splittable = true)
        assertThat(asi).containsExactly(Ability.STRENGTH, 2)
        assertThat(BuildEdits.toggleIncrease(asi, Ability.DEXTERITY, 2, emptyList(), true)).isEqualTo(asi) // plus rien à placer
        asi = BuildEdits.toggleIncrease(asi, Ability.STRENGTH, 2, emptyList(), true)
        assertThat(asi).isEmpty()
        // Athlète : +1 en Force ou en Dextérité seulement, le point se déplace.
        val choices = listOf(Ability.STRENGTH, Ability.DEXTERITY)
        var athlete = BuildEdits.toggleIncrease(emptyMap(), Ability.STRENGTH, 1, choices, false)
        athlete = BuildEdits.toggleIncrease(athlete, Ability.DEXTERITY, 1, choices, false)
        assertThat(athlete).containsExactly(Ability.DEXTERITY, 1)
        assertThat(BuildEdits.toggleIncrease(athlete, Ability.WISDOM, 1, choices, false)).isEqualTo(athlete)
        val build = BuildEdits.setFeat(BuildFixtures.battleMage, 5, FeatChoice(id("feat.athlete"), athlete), rules)
        assertThat(CharacterSheetCalculator.compute(build, rules).scores[Ability.DEXTERITY]).isEqualTo(16)
    }

    @Test
    fun `changer de race retire une sous-race qui ne lui appartient pas`() {
        val build = BuildFixtures.battleMage
        assertThat(BuildEdits.setRace(build, id("race.dwarf"), catalog).subrace).isNull()
        assertThat(BuildEdits.setRace(build, id("race.elf"), catalog).subrace).isEqualTo(id("subrace.high-elf"))
    }
}
