package fr.carnetdefaerun.pipeline

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.Ability
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.ClassFacts
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.Resource
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test

/**
 * Instantané de référence des classes générées (`data/game/classes.json`), comparé à la main aux pages de bg3.wiki :
 * Wizard (révision 439634), Warlock (424760), Ranger (422145), Cleric (439581), Barbarian (439532), Classes (417100,
 * maîtrises de multiclasse). Si une page change, la génération doit être relue avant de mettre ce test à jour.
 */
class ClassDataTest {
    private val classes: Map<String, Entry> by lazy {
        val file = File("../../data/game/classes.json").takeIf { it.exists() } ?: File("data/game/classes.json")
        ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), file.readText()).associateBy { it.id.value }
    }

    private fun facts(id: String) = classes.getValue(id).facts as ClassFacts

    @Test
    fun `les douze classes ont douze niveaux et un bonus de maîtrise de +2 à +4`() {
        assertThat(classes.keys).hasSize(12)
        classes.values.forEach { e ->
            val f = e.facts as ClassFacts
            assertThat(f.levels.map { it.level }).containsExactlyElementsIn(1..12).inOrder()
            assertThat(f.levels.map { it.proficiencyBonus }).containsExactly(2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4).inOrder()
            assertThat(f.savingThrows).hasSize(2)
            assertThat(e.unknown).isEmpty()
        }
    }

    @Test
    fun `magicien, page Wizard`() {
        val w = facts("class.wizard")
        assertThat(w.hitPointsAtFirstLevel to w.hitPointsPerLevel).isEqualTo(6 to 4)
        assertThat(w.savingThrows).containsExactly(Ability.INTELLIGENCE, Ability.WISDOM)
        assertThat(w.spellcastingAbility).isEqualTo(Ability.INTELLIGENCE)
        assertThat(w.subclassLevel).isEqualTo(2)
        assertThat(w.levels.filter { it.feat }.map { it.level }).containsExactly(4, 8, 12).inOrder()
        assertThat(w.levels.last().spellSlots).containsExactly(4, 3, 3, 3, 2, 1).inOrder()
        assertThat(w.levels.first().cantripsKnown).isEqualTo(3)
        assertThat(w.levels.last().resources.single().resource).isEqualTo(Resource.ARCANE_RECOVERY)
        assertThat(w.multiclassProficiencies.armour).isEmpty()
    }

    @Test
    fun `occultiste, page Warlock, magie de pacte`() {
        val w = facts("class.warlock")
        assertThat(w.pactMagic).isTrue()
        assertThat(w.subclassLevel).isEqualTo(1)
        assertThat(w.levels.first().spellSlots).containsExactly(1)
        assertThat(w.levels.last().spellSlots).containsExactly(0, 0, 0, 0, 3).inOrder()
    }

    @Test
    fun `rôdeur, page Ranger, quatre emplacements de niveau 3 au niveau 12 (écart voulu par le jeu)`() {
        assertThat(facts("class.ranger").levels.last().spellSlots).containsExactly(4, 3, 4).inOrder()
    }

    @Test
    fun `clerc, page Cleric, sous-classe au niveau 1, lue dans la colonne Subclass`() {
        val c = facts("class.cleric")
        assertThat(c.subclassLevel).isEqualTo(1)
        assertThat(c.hitPointsAtFirstLevel to c.hitPointsPerLevel).isEqualTo(8 to 5)
        assertThat(c.spellcastingAbility).isEqualTo(Ability.WISDOM)
        assertThat(c.multiclassProficiencies.armour).containsExactly(ArmourProficiency.LIGHT, ArmourProficiency.MEDIUM, ArmourProficiency.SHIELD).inOrder()
    }

    @Test
    fun `barbare, page Barbarian, rage et dégâts de rage`() {
        val b = facts("class.barbarian")
        assertThat(b.hitPointsAtFirstLevel to b.hitPointsPerLevel).isEqualTo(12 to 7)
        assertThat(b.levels.first().resources.single().resource).isEqualTo(Resource.RAGE)
        assertThat(b.levels.first().values.map { it.key }).contains("rage_damage")
        assertThat(b.multiclassProficiencies.martialWeapons).isTrue()
    }
}
