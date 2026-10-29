package fr.carnetdefaerun.pipeline

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.content.ArmourProficiency
import fr.carnetdefaerun.core.model.content.ContentFormat
import fr.carnetdefaerun.core.model.content.ContentId
import fr.carnetdefaerun.core.model.content.Entry
import fr.carnetdefaerun.core.model.content.GrantKind
import fr.carnetdefaerun.core.model.content.Resource
import fr.carnetdefaerun.core.model.content.SubclassFacts
import java.io.File
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Test

/**
 * Instantané de référence des sous-classes générées (`data/game/sous-classes.json`), comparé à la main aux pages
 * de bg3.wiki : War Domain (révision 424747), Circle of the Land (429837), Battle Master (416338) et Manoeuvres
 * (437844), Draconic Bloodline (417913) et DraconicAncestryTable (439058), Way of the Four Elements (424800),
 * The Fiend (424003), Arcane Trickster (439623), Eldritch Knight (439631), Abjuration School (415738).
 */
class SubclassDataTest {
    private val subclasses: Map<String, Entry> by lazy {
        val file = File("../../data/game/sous-classes.json").takeIf { it.exists() } ?: File("data/game/sous-classes.json")
        ContentFormat.strict.decodeFromString(ListSerializer(Entry.serializer()), file.readText()).associateBy { it.id.value }
    }

    private fun facts(id: String) = subclasses.getValue(id).facts as SubclassFacts

    @Test
    fun `cinquante-huit sous-classes, chacune rattachée à une classe`() {
        assertThat(subclasses).hasSize(58)
        assertThat(subclasses.values.map { (it.facts as SubclassFacts).parentClass.value }.toSet()).hasSize(12)
        // Deux « Wild Magic » : identifiants distingués par la classe parente.
        assertThat(subclasses.keys).containsAtLeast("subclass.wild-magic-barbarian", "subclass.wild-magic-sorcerer", "subclass.giant")
    }

    @Test
    fun `domaine de la guerre, maîtrises, sorts de domaine et charges`() {
        val w = facts("subclass.war-domain")
        assertThat(w.parentClass).isEqualTo(ContentId("class.cleric"))
        assertThat(w.proficiencies.armour).containsExactly(ArmourProficiency.HEAVY)
        assertThat(w.proficiencies.martialWeapons).isTrue()
        val first = w.levels.first { it.level == 1 }
        assertThat(first.spells.map { it.spell.value }).containsExactly("spell.divine-favour", "spell.shield-of-faith")
        assertThat(first.spells.map { it.kind }.toSet()).containsExactly(GrantKind.ALWAYS_PREPARED)
        assertThat(first.resources.single().resource).isEqualTo(Resource.WAR_PRIEST)
    }

    @Test
    fun `cercle de la terre, un terrain au choix, deux sorts par terrain`() {
        val levels = facts("subclass.circle-of-the-land").levels
        for (level in listOf(3, 5, 7, 9)) {
            val choice = levels.first { it.level == level }.choices.single()
            assertThat(choice.options).hasSize(8)
            choice.options.forEach { assertThat(it.grants).hasSize(2) }
        }
        assertThat(levels.first { it.level == 2 }.choices.single().options).hasSize(6) // sort mineur au choix
    }

    @Test
    fun `maître de guerre, trois manœuvres puis deux de plus, lues dans la section des manœuvres`() {
        val levels = facts("subclass.battle-master").levels
        assertThat(levels.map { l -> l.choices.single().count }).containsExactly(3, 2, 2).inOrder()
        assertThat(levels.first().choices.single().options).hasSize(14)
        assertThat(levels.first().resources.single().resource).isEqualTo(Resource.SUPERIORITY_DIE)
    }

    @Test
    fun `lignée draconique, dix ascendances, voie des quatre éléments, trois disciplines`() {
        assertThat(facts("subclass.draconic-bloodline").levels.first().choices.single().options).hasSize(10)
        val disciplines = facts("subclass.way-of-the-four-elements").levels.first().choices.single()
        assertThat(disciplines.count).isEqualTo(3)
        assertThat(disciplines.options).hasSize(11)
    }

    @Test
    fun `fiélon, liste étendue (sorts ajoutés à la liste, pas toujours préparés)`() {
        val first = facts("subclass.the-fiend").levels.first()
        assertThat(first.spells.map { it.spell.value }).containsExactly("spell.burning-hands", "spell.command")
        assertThat(first.spells.map { it.kind }.toSet()).containsExactly(GrantKind.EXPANDED_LIST)
    }

    @Test
    fun `tiers de lanceur, pages divergentes au niveau 10, emplacements laissés vides et signalés`() {
        for (id in listOf("subclass.arcane-trickster", "subclass.eldritch-knight")) {
            val f = facts(id)
            assertThat(f.levels.first { it.level == 7 }.spellSlots).containsExactly(4, 2).inOrder()
            assertThat(f.levels.first { it.level == 10 }.spellSlots).isEmpty()
            assertThat(subclasses.getValue(id).unknown).contains("spell_slots")
        }
    }

    @Test
    fun `école d'abjuration, capacité sans fiche gardée par son nom`() {
        val levels = facts("subclass.abjuration-school").levels
        assertThat(levels.map { it.level }).containsExactly(2, 6, 10).inOrder()
        assertThat(levels.first().features.map { it.name.en }).containsExactly("Abjuration Savant", "Arcane Ward")
    }
}
