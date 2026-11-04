package fr.carnetdefaerun.core.rules

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.model.builds.Build
import fr.carnetdefaerun.core.model.builds.BuildLevel
import fr.carnetdefaerun.core.model.builds.FeatChoice
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.Ability.CHARISMA
import fr.carnetdefaerun.core.model.content.Ability.CONSTITUTION
import fr.carnetdefaerun.core.model.content.Ability.DEXTERITY
import fr.carnetdefaerun.core.model.content.Ability.INTELLIGENCE
import fr.carnetdefaerun.core.model.content.Ability.STRENGTH
import fr.carnetdefaerun.core.model.content.Ability.WISDOM
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test

/**
 * Fiche calculée sur les vraies données de jeu (data/game). Chaque valeur attendue est recalculée à la main d'après
 * les pages sources, puis recoupée avec les fonctions de règles prises une à une.
 */
class CharacterSheetTest {

    private val data: RulesData by lazy {
        val entries = listOf("classes.json", "sous-classes.json", "dons.json", "sous-races.json").flatMap {
            ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), File("../../data/game/$it").readText())
        }
        RulesData.from(entries)
    }

    private fun id(value: String) = ContentId(value)
    private fun level(classId: String, subclass: String? = null, feat: FeatChoice? = null) =
        BuildLevel(id(classId), subclass?.let(::id), feat)
    private fun asi(vararg increases: Pair<Ability, Int>) = FeatChoice(id("feat.ability-improvement"), mapOf(*increases))
    private fun scores(vararg values: Pair<Ability, Int>) = Ability.entries.associateWith { 8 } + values

    @Test
    fun `profils tirés des données, conformes aux tableaux`() {
        val wizard = data.classes.getValue("class.wizard")
        assertThat(wizard.hitDie).isEqualTo(HitDie(6, 4))
        assertThat(wizard.progression).isEqualTo(CasterProgression.FULL)
        assertThat(wizard.featLevels).containsExactly(4, 8, 12)
        assertThat(data.classes.getValue("class.paladin").progression).isEqualTo(CasterProgression.HALF)
        assertThat(data.classes.getValue("class.paladin").spellcastingFrom).isEqualTo(2)
        assertThat(data.classes.getValue("class.warlock").progression).isEqualTo(CasterProgression.PACT)
        assertThat(data.classes.getValue("class.fighter").progression).isEqualTo(CasterProgression.NONE)
        assertThat(data.classes.getValue("class.fighter").featLevels).containsExactly(4, 6, 8, 12)
        assertThat(data.classes.getValue("class.rogue").featLevels).containsExactly(4, 8, 10, 12)
        assertThat(data.classes.getValue("class.barbarian").unarmoured).isEqualTo(UnarmouredFormula.BARBARIAN)
        assertThat(data.classes.getValue("class.monk").unarmoured).isEqualTo(UnarmouredFormula.MONK)
        assertThat(data.subclasses.getValue("subclass.eldritch-knight").progression).isEqualTo(CasterProgression.THIRD)
        assertThat(data.subclasses.getValue("subclass.eldritch-knight").spellcastingFrom).isEqualTo(3)
        assertThat(data.subclasses.getValue("subclass.draconic-bloodline").draconicResilience).isTrue()
        assertThat(data.subclasses.filterValues { it.draconicResilience }.keys).containsExactly("subclass.draconic-bloodline")
        assertThat(data.dwarvenToughnessSubraces).containsExactly("subrace.gold-dwarf")
        assertThat(data.feats.filterValues { it.tough }.keys).containsExactly("feat.tough")
        assertThat(data.feats.getValue("feat.resilient").resilient).isTrue()
    }

    @Test
    fun `guerrier 2 puis magicien 10, détail des points de vie`() {
        val build = Build(
            name = "Mage de bataille",
            baseScores = scores(INTELLIGENCE to 15, CONSTITUTION to 15, DEXTERITY to 14),
            majorBonus = INTELLIGENCE,
            minorBonus = CONSTITUTION,
            levels = List(2) { level("class.fighter") } +
                level("class.wizard") + level("class.wizard", "subclass.evocation-school") +
                level("class.wizard") + level("class.wizard", feat = asi(INTELLIGENCE to 2)) +
                List(3) { level("class.wizard") } + level("class.wizard", feat = asi(INTELLIGENCE to 1, CONSTITUTION to 1)) +
                List(2) { level("class.wizard") },
        )
        val sheet = CharacterSheetCalculator.compute(build, data)
        assertThat(sheet.problems).isEmpty()
        assertThat(sheet.level).isEqualTo(12)
        assertThat(sheet.classLevels).containsExactly("class.fighter", 2, "class.wizard", 10).inOrder()
        assertThat(sheet.pointBuyRemaining).isEqualTo(2) // 9 + 9 + 7 = 25 points dépensés sur 27
        // Intelligence 15 + 2 + 2 + 1 = 20 ; Constitution 15 + 1 + 1 = 17 (+3).
        assertThat(sheet.scores[INTELLIGENCE]).isEqualTo(20)
        assertThat(sheet.scores[CONSTITUTION]).isEqualTo(17)
        // 10 + 3, puis 6 + 3 pour le second niveau de guerrier, puis 10 × (4 + 3) : 92.
        assertThat(sheet.hitPoints.terms).containsExactly(
            Term.FirstLevel("class.fighter", 10, 3),
            Term.LaterLevels("class.fighter", 1, 6, 3),
            Term.LaterLevels("class.wizard", 10, 4, 3),
        ).inOrder()
        assertThat(sheet.hitPoints.total).isEqualTo(92)
        val dice = List(2) { HitDie(10, 6) } + List(10) { HitDie(6, 4) }
        assertThat(sheet.hitPoints.total).isEqualTo(HitPoints.maximum(dice, 3))
        // Sauvegardes du guerrier seulement ; maîtrise +4 ; magicien : DD 8 + 5 + 4, attaque +9 ; emplacements d'un lanceur 10.
        assertThat(sheet.savingThrows).containsExactly(STRENGTH, CONSTITUTION)
        assertThat(sheet.proficiency).isEqualTo(4)
        assertThat(sheet.spellcasting).containsExactly(Spellcasting("class.wizard", INTELLIGENCE, 5, 17, 9))
        assertThat(sheet.spellSlots).isEqualTo(SpellSlots.forLevel(10))
        assertThat(sheet.pactSlots).isNull()
        assertThat(sheet.armourClass.total).isEqualTo(12)
        assertThat(sheet.initiative).isEqualTo(2)
    }

    @Test
    fun `nain d'or guerrier avec Robuste, chevalier occulte`() {
        val build = Build(
            name = "Rempart",
            subrace = id("subrace.gold-dwarf"),
            baseScores = scores(STRENGTH to 15, CONSTITUTION to 15, INTELLIGENCE to 14),
            majorBonus = STRENGTH,
            minorBonus = CONSTITUTION,
            levels = listOf(
                level("class.fighter"), level("class.fighter"), level("class.fighter", "subclass.eldritch-knight"),
                level("class.fighter", feat = FeatChoice(id("feat.tough"))),
            ),
        )
        val sheet = CharacterSheetCalculator.compute(build, data)
        assertThat(sheet.problems).isEmpty()
        // Constitution 16 (+3) : 10 + 3 × 6 + 4 × 3 = 40 ; Robustesse naine +4 ; Robuste +8, rétroactif.
        assertThat(sheet.hitPoints.terms).containsAtLeast(
            Term.PerLevelBonus("subrace.gold-dwarf", 1, 4),
            Term.PerLevelBonus("feat.tough", 2, 4),
        )
        assertThat(sheet.hitPoints.total).isEqualTo(52)
        assertThat(sheet.hitPoints.total).isEqualTo(HitPoints.maximum(List(4) { HitDie(10, 6) }, 3, dwarvenToughness = true) + 2 * 4)
        // Le chevalier occulte lance ses sorts avec l'Intelligence de sa classe (14, +2) ; tiers de lanceur au niveau 4.
        assertThat(sheet.spellcasting).containsExactly(Spellcasting("class.fighter", INTELLIGENCE, 2, 12, 4))
        assertThat(sheet.spellSlots).containsExactly(3)
    }

    @Test
    fun `ensorceleur draconique, points de vie et classe d'armure`() {
        val build = Build(
            name = "Écailles",
            baseScores = scores(CHARISMA to 15, CONSTITUTION to 14, DEXTERITY to 14),
            majorBonus = CHARISMA,
            minorBonus = DEXTERITY,
            levels = listOf(level("class.sorcerer", "subclass.draconic-bloodline")) + List(2) { level("class.sorcerer") } +
                level("class.sorcerer", feat = asi(CHARISMA to 2)) + List(2) { level("class.sorcerer") },
        )
        val sheet = CharacterSheetCalculator.compute(build, data)
        assertThat(sheet.problems).isEmpty()
        // Constitution 14 (+2) : 6 + 2 + 5 × (4 + 2) = 38, plus 6 pour la Résilience draconique.
        assertThat(sheet.hitPoints.total).isEqualTo(44)
        assertThat(sheet.hitPoints.total).isEqualTo(HitPoints.maximum(List(6) { HitDie(6, 4) }, 2, draconicResilienceSorcererLevels = 6))
        // Dextérité 15 (+2) : 13 + 2.
        assertThat(sheet.armourClass.terms).containsExactly(Term.Base(13, UnarmouredFormula.MAGE_ARMOUR), Term.AbilityModifier(DEXTERITY, 2)).inOrder()
        assertThat(sheet.armourClass.total).isEqualTo(ArmourClass.compute(dexterity = 2, unarmoured = setOf(UnarmouredFormula.MAGE_ARMOUR)))
        assertThat(sheet.scores[CHARISMA]).isEqualTo(19)
        assertThat(sheet.spellcasting.single().saveDc).isEqualTo(8 + 4 + 3)
    }

    @Test
    fun `barbare puis moine, meilleure défense sans armure et sauvegardes de départ`() {
        val build = Build(
            name = "Poings",
            baseScores = scores(STRENGTH to 15, CONSTITUTION to 15, DEXTERITY to 14),
            majorBonus = CONSTITUTION,
            minorBonus = STRENGTH,
            levels = listOf(level("class.barbarian"), level("class.monk")),
        )
        val sheet = CharacterSheetCalculator.compute(build, data)
        assertThat(sheet.problems).isEmpty()
        // Constitution 17 (+3), Dextérité 14 (+2), Sagesse 8 (−1) : barbare 15, moine 11.
        assertThat(sheet.armourClass.terms).containsExactly(
            Term.Base(10, UnarmouredFormula.BARBARIAN), Term.AbilityModifier(CONSTITUTION, 3), Term.AbilityModifier(DEXTERITY, 2),
        ).inOrder()
        assertThat(sheet.armourClass.total).isEqualTo(
            ArmourClass.compute(dexterity = 2, constitution = 3, wisdom = -1, unarmoured = setOf(UnarmouredFormula.BARBARIAN, UnarmouredFormula.MONK)),
        )
        assertThat(sheet.savingThrows).containsExactly(STRENGTH, CONSTITUTION)
        assertThat(sheet.hitPoints.total).isEqualTo(12 + 3 + 5 + 3)
        assertThat(sheet.spellcasting).isEmpty()
        assertThat(sheet.spellSlots).isEmpty()
    }

    @Test
    fun `occultiste 5 et ensorceleur 2, Magie de pacte à part`() {
        val build = Build(
            name = "Pacte",
            baseScores = scores(CHARISMA to 15, CONSTITUTION to 15, DEXTERITY to 14),
            majorBonus = CHARISMA,
            minorBonus = CONSTITUTION,
            levels = listOf(level("class.warlock", "subclass.the-fiend")) + List(2) { level("class.warlock") } +
                level("class.warlock", feat = asi(CHARISMA to 2)) + level("class.warlock") +
                level("class.sorcerer", "subclass.storm-sorcery") + level("class.sorcerer"),
        )
        val sheet = CharacterSheetCalculator.compute(build, data)
        assertThat(sheet.problems).isEmpty()
        // Occultiste 5 : deux emplacements de niveau 3 (tableau de classe) ; ensorceleur 2 seul compte pour l'Incantation.
        assertThat(sheet.pactSlots).isEqualTo(PactSlots(2, 3))
        assertThat(sheet.spellSlots).isEqualTo(SpellSlots.forLevel(2))
        assertThat(sheet.spellcasting.map { it.classId }).containsExactly("class.warlock", "class.sorcerer").inOrder()
        assertThat(sheet.spellcasting.map { it.saveDc }.distinct()).containsExactly(8 + 4 + 3)
    }

    @Test
    fun `Résilient donne la sauvegarde choisie, les dons plafonnent à 20`() {
        val build = Build(
            name = "Studieux",
            baseScores = scores(INTELLIGENCE to 15, CONSTITUTION to 15, DEXTERITY to 14),
            majorBonus = INTELLIGENCE,
            minorBonus = CONSTITUTION,
            levels = listOf(level("class.wizard"), level("class.wizard", "subclass.abjuration-school"), level("class.wizard")) +
                level("class.wizard", feat = asi(INTELLIGENCE to 2)) + List(3) { level("class.wizard") } +
                level("class.wizard", feat = asi(INTELLIGENCE to 2)) + List(3) { level("class.wizard") } +
                level("class.wizard", feat = FeatChoice(id("feat.resilient"), mapOf(CONSTITUTION to 1))),
        )
        val sheet = CharacterSheetCalculator.compute(build, data)
        assertThat(sheet.problems).isEmpty()
        assertThat(sheet.scores[INTELLIGENCE]).isEqualTo(20) // 17 + 2 + 2, plafonné à 20
        assertThat(sheet.scores[CONSTITUTION]).isEqualTo(17)
        assertThat(sheet.savingThrows).containsExactly(INTELLIGENCE, WISDOM, CONSTITUTION)
    }

    @Test
    fun `choix invalides ou incomplets signalés`() {
        fun problems(build: Build) = CharacterSheetCalculator.compute(build, data).problems
        val base = Build(name = "Essai", majorBonus = STRENGTH, minorBonus = DEXTERITY, levels = listOf(level("class.fighter")))
        assertThat(problems(base)).isEmpty()
        assertThat(problems(base.copy(levels = emptyList()))).containsExactly(SheetProblem.NO_LEVEL)
        assertThat(problems(base.copy(minorBonus = STRENGTH))).containsExactly(SheetProblem.BONUSES_SAME_ABILITY)
        assertThat(problems(base.copy(majorBonus = null))).containsExactly(SheetProblem.BONUSES_MISSING)
        assertThat(problems(base.copy(baseScores = scores(STRENGTH to 15, DEXTERITY to 15, CONSTITUTION to 15, WISDOM to 9))))
            .containsExactly(SheetProblem.POINT_BUY_OVER_BUDGET)
        assertThat(problems(base.copy(baseScores = scores(STRENGTH to 16)))).containsExactly(SheetProblem.POINT_BUY_OUT_OF_RANGE)
        assertThat(problems(base.copy(levels = listOf(level("class.unknown"))))).contains(SheetProblem.UNKNOWN_CLASS)
        assertThat(problems(base.copy(levels = List(13) { level("class.fighter") }))).contains(SheetProblem.TOO_MANY_LEVELS)
        // Don au niveau 2 de guerrier : interdit ; niveau 3 sans sous-classe : à compléter.
        assertThat(problems(base.copy(levels = listOf(level("class.fighter"), level("class.fighter", feat = asi(STRENGTH to 2))))))
            .containsExactly(SheetProblem.FEAT_NOT_ALLOWED)
        assertThat(problems(base.copy(levels = List(3) { level("class.fighter") }))).containsExactly(SheetProblem.SUBCLASS_MISSING)
        // Sous-classe d'une autre classe, ou au mauvais niveau.
        assertThat(problems(base.copy(levels = listOf(level("class.fighter", "subclass.eldritch-knight"))))).contains(SheetProblem.SUBCLASS_INVALID)
        assertThat(problems(base.copy(levels = listOf(level("class.fighter"), level("class.fighter"), level("class.fighter", "subclass.the-fiend")))))
            .contains(SheetProblem.SUBCLASS_INVALID)
        // Niveau 4 sans don ; Athlète en Constitution ; amélioration +2 répartie en +1 et +1 : permise.
        val fighter3 = listOf(level("class.fighter"), level("class.fighter"), level("class.fighter", "subclass.champion"))
        assertThat(problems(base.copy(levels = fighter3 + level("class.fighter")))).containsExactly(SheetProblem.FEAT_MISSING)
        val athlete = FeatChoice(id("feat.athlete"), mapOf(CONSTITUTION to 1))
        assertThat(problems(base.copy(levels = fighter3 + level("class.fighter", feat = athlete)))).containsExactly(SheetProblem.FEAT_INCREASE_INVALID)
        assertThat(problems(base.copy(levels = fighter3 + level("class.fighter", feat = asi(STRENGTH to 1, DEXTERITY to 1))))).isEmpty()
        assertThat(problems(base.copy(levels = fighter3 + level("class.fighter", feat = asi(STRENGTH to 1))))).containsExactly(SheetProblem.FEAT_INCREASE_INVALID)
        assertThat(problems(base.copy(levels = fighter3 + level("class.fighter", feat = FeatChoice(id("feat.tough"), mapOf(STRENGTH to 1))))))
            .containsExactly(SheetProblem.FEAT_INCREASE_INVALID)
    }

    @Test
    fun `chaque classe seule, de 1 à 12, recoupée avec les fonctions de règles`() {
        for ((classId, profile) in data.classes) {
            for (n in 1..12) {
                val build = Build(name = classId, majorBonus = CONSTITUTION, minorBonus = DEXTERITY, levels = List(n) { level(classId) })
                val sheet = CharacterSheetCalculator.compute(build, data)
                val con = abilityModifier(8 + 2)
                assertWithMessage("$classId $n").that(sheet.hitPoints.total).isEqualTo(HitPoints.maximum(List(n) { profile.hitDie }, con))
                assertWithMessage("$classId $n").that(sheet.proficiency).isEqualTo(proficiencyBonus(n))
                assertWithMessage("$classId $n").that(sheet.savingThrows).isEqualTo(profile.savingThrows)
                if (profile.progression != CasterProgression.PACT) {
                    val expected = SpellSlots.forClasses(listOf(CasterLevels(profile.progression, n, profile.spellcastingFrom, classId)))
                    assertWithMessage("$classId $n").that(sheet.spellSlots).isEqualTo(expected)
                }
            }
        }
    }
}
