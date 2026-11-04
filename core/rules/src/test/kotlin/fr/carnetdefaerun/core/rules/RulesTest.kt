package fr.carnetdefaerun.core.rules

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ClassFacts
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.SubclassFacts
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test

/** Règles de BG3, vérifiées sur les exemples chiffrés des pages sources et recoupées avec les tableaux des classes. */
class RulesTest {

    @Test
    fun `modificateur de caractéristique, arrondi vers le bas et plafonné à +10`() {
        assertThat(abilityModifier(10)).isEqualTo(0)
        assertThat(abilityModifier(11)).isEqualTo(0)
        assertThat(abilityModifier(9)).isEqualTo(-1)
        assertThat(abilityModifier(8)).isEqualTo(-1)
        assertThat(abilityModifier(7)).isEqualTo(-2)
        assertThat(abilityModifier(15)).isEqualTo(2)
        assertThat(abilityModifier(17)).isEqualTo(3)
        assertThat(abilityModifier(30)).isEqualTo(10)
        assertThat(abilityModifier(40)).isEqualTo(10)
    }

    @Test
    fun `achat de points, coûts, budget et bonus`() {
        assertThat(PointBuy.cost(8)).isEqualTo(0)
        assertThat(PointBuy.cost(13)).isEqualTo(5)
        assertThat(PointBuy.cost(14)).isEqualTo(7)
        assertThat(PointBuy.cost(15)).isEqualTo(9)
        val base = mapOf(Ability.STRENGTH to 15, Ability.DEXTERITY to 15, Ability.CONSTITUTION to 15)
        assertThat(PointBuy.spent(base)).isEqualTo(27)
        assertThat(PointBuy.remaining(base)).isEqualTo(0)
        assertThat(PointBuy.problems(base, Ability.STRENGTH, Ability.DEXTERITY)).isEmpty()
        val final = PointBuy.finalScores(base, Ability.STRENGTH, Ability.DEXTERITY)
        assertThat(final[Ability.STRENGTH]).isEqualTo(17) // la valeur de départ la plus haute que permet la page
        assertThat(final[Ability.DEXTERITY]).isEqualTo(16)
        assertThat(final[Ability.WISDOM]).isEqualTo(8)
        assertThat(PointBuy.problems(base + (Ability.WISDOM to 9), Ability.STRENGTH, Ability.DEXTERITY)).containsExactly(PointBuy.Problem.OVER_BUDGET)
        assertThat(PointBuy.problems(mapOf(Ability.STRENGTH to 16), Ability.STRENGTH, Ability.DEXTERITY)).containsExactly(PointBuy.Problem.OUT_OF_RANGE)
        assertThat(PointBuy.problems(emptyMap(), Ability.STRENGTH, Ability.STRENGTH)).containsExactly(PointBuy.Problem.SAME_ABILITY_BONUSES)
    }

    @Test
    fun `bonus de maîtrise et DD des sorts`() {
        assertThat((1..12).map(::proficiencyBonus)).containsExactly(2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4).inOrder()
        assertThat(spellSaveDc(spellcastingModifier = 3, proficiency = 2)).isEqualTo(13)
        assertThat(spellAttackBonus(spellcastingModifier = 3, proficiency = 2)).isEqualTo(5)
        assertThat(preparedSpells(spellcastingModifier = -1, classLevel = 1)).isEqualTo(1)
        assertThat(preparedSpells(spellcastingModifier = 3, classLevel = 4)).isEqualTo(7)
    }

    @Test
    fun `points de vie, exemples de la page`() {
        val fighter = HitDie(10, 6)
        val wizard = HitDie(6, 4)
        // Guerrier, Constitution 15 (+2) : 12, puis 20, puis 28.
        assertThat(HitPoints.maximum(List(1) { fighter }, 2)).isEqualTo(12)
        assertThat(HitPoints.maximum(List(2) { fighter }, 2)).isEqualTo(20)
        assertThat(HitPoints.maximum(List(3) { fighter }, 2)).isEqualTo(28)
        // Au niveau 4, la Constitution passe à 17 (+3) : rétroactif, 40.
        assertThat(HitPoints.maximum(List(4) { fighter }, 3)).isEqualTo(40)
        // Magicien, Constitution 8 (−1) : 5, puis 8, puis 11, puis 14.
        assertThat((1..4).map { n -> HitPoints.maximum(List(n) { wizard }, -1) }).containsExactly(5, 8, 11, 14).inOrder()
        // Capacités passives : +1 par niveau, +1 par niveau d'ensorceleur.
        assertThat(HitPoints.maximum(List(3) { fighter }, 2, dwarvenToughness = true)).isEqualTo(31)
        assertThat(HitPoints.maximum(List(3) { HitDie(6, 4) }, 0, draconicResilienceSorcererLevels = 3)).isEqualTo(17)
    }

    @Test
    fun `niveau d'incantateur effectif en multiclasse`() {
        // Exemple de la page : chevalier occulte 5 et escroc arcanique 5 donnent 5/3 + 5/3 arrondi au-dessous, soit 3.
        val ekAt = listOf(CasterLevels(CasterProgression.THIRD, 5, 3), CasterLevels(CasterProgression.THIRD, 5, 3))
        assertThat(SpellSlots.effectiveSpellcasterLevel(ekAt)).isEqualTo(3)
        // Les niveaux d'occultiste ne comptent pas.
        val warlockWizard = listOf(CasterLevels(CasterProgression.PACT, 5), CasterLevels(CasterProgression.FULL, 3))
        assertThat(SpellSlots.effectiveSpellcasterLevel(warlockWizard)).isEqualTo(3)
        // Paladin 3 + ensorceleur 3 : 1,5 + 3, arrondi au-dessous.
        val palSorc = listOf(CasterLevels(CasterProgression.HALF, 3, 2), CasterLevels(CasterProgression.FULL, 3))
        assertThat(SpellSlots.effectiveSpellcasterLevel(palSorc)).isEqualTo(4)
        assertThat(SpellSlots.forClasses(palSorc)).containsExactly(4, 3).inOrder()
        // Un paladin de niveau 1 n'a pas encore l'Incantation.
        assertThat(SpellSlots.effectiveSpellcasterLevel(listOf(CasterLevels(CasterProgression.HALF, 1, 2)))).isEqualTo(0)
        assertThat(SpellSlots.forLevel(0)).isEmpty()
    }

    @Test
    fun `rôdeur de niveau 12, emplacement de niveau 3 supplémentaire`() {
        assertThat(SpellSlots.forClasses(listOf(CasterLevels(CasterProgression.HALF, 12, 2, "class.ranger")))).containsExactly(4, 3, 4).inOrder()
        assertThat(SpellSlots.forClasses(listOf(CasterLevels(CasterProgression.HALF, 12, 2, "class.paladin")))).containsExactly(4, 3, 3).inOrder()
        // En multiclasse, la table générale s'applique.
        val rangerWizard = listOf(CasterLevels(CasterProgression.HALF, 11, 2, "class.ranger"), CasterLevels(CasterProgression.FULL, 1, 1, "class.wizard"))
        assertThat(SpellSlots.forClasses(rangerWizard)).isEqualTo(SpellSlots.forLevel(6))
    }

    // --- Recoupement avec les tableaux de progression extraits des pages de classe (data/game) ---

    private val entries: List<Entry> by lazy {
        listOf("classes.json", "sous-classes.json").flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), File("../../data/game/$it").readText())
        }
    }

    @Test
    fun `chaque classe d'incantation seule retrouve les emplacements de son tableau`() {
        val progressions = mapOf(
            "class.bard" to CasterProgression.FULL, "class.cleric" to CasterProgression.FULL, "class.druid" to CasterProgression.FULL,
            "class.sorcerer" to CasterProgression.FULL, "class.wizard" to CasterProgression.FULL,
            "class.paladin" to CasterProgression.HALF, "class.ranger" to CasterProgression.HALF,
        )
        for ((id, progression) in progressions) {
            val facts = entries.first { it.id.value == id }.facts as ClassFacts
            val from = facts.levels.first { it.spellSlots.isNotEmpty() }.level
            for (level in facts.levels) {
                val expected = level.spellSlots
                val actual = SpellSlots.forClasses(listOf(CasterLevels(progression, level.level, from, id)))
                assertWithMessage("$id niveau ${level.level}").that(actual).isEqualTo(expected)
            }
        }
    }

    @Test
    fun `chevalier occulte et escroc arcanique retrouvent les emplacements renseignés`() {
        for (id in listOf("subclass.eldritch-knight", "subclass.arcane-trickster")) {
            val facts = entries.first { it.id.value == id }.facts as SubclassFacts
            // Les niveaux sans emplacements renseignés sont ceux où les pages divergent (D-067) : ils ne sont pas testés.
            for (level in facts.levels.filter { it.spellSlots.isNotEmpty() }) {
                val actual = SpellSlots.forClasses(listOf(CasterLevels(CasterProgression.THIRD, level.level, 3)))
                assertWithMessage("$id niveau ${level.level}").that(actual).isEqualTo(level.spellSlots)
            }
        }
    }

    @Test
    fun `bonus de maîtrise et points de vie recoupés avec les tableaux des classes`() {
        for (entry in entries.filter { it.facts is ClassFacts }) {
            val facts = entry.facts as ClassFacts
            facts.levels.forEach { l -> assertWithMessage("${entry.id} niveau ${l.level}").that(proficiencyBonus(l.level)).isEqualTo(l.proficiencyBonus) }
            val die = HitDie(facts.hitPointsAtFirstLevel, facts.hitPointsPerLevel)
            assertWithMessage("${entry.id}").that(HitPoints.maximum(List(12) { die }, 0)).isEqualTo(facts.hitPointsAtFirstLevel + 11 * facts.hitPointsPerLevel)
        }
    }
}
