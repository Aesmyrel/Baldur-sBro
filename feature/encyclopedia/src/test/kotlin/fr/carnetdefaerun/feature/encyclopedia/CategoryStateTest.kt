package fr.carnetdefaerun.feature.encyclopedia

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.data.content.ContentRepository
import fr.carnetdefaerun.core.model.content.EntryType
import fr.carnetdefaerun.core.model.content.SpellSchool
import fr.carnetdefaerun.core.model.spoiler.Act
import fr.carnetdefaerun.core.model.spoiler.SpoilerProfile
import fr.carnetdefaerun.core.testing.GameData
import fr.carnetdefaerun.core.testing.InMemoryContentStore
import org.junit.Test

/** Liste d'une catégorie : filtre par nom, par sous-type, groupes par parent. */
class CategoryStateTest {
    private val repository = ContentRepository(InMemoryContentStore(GameData.entries, GameData.references))
    private val profile = SpoilerProfile(Act.ACT_1)
    private fun list(type: EntryType) = repository.list(type, profile)

    @Test
    fun `filtre par nom français ou anglais, accents ignorés`() {
        val spells = list(EntryType.SPELL)
        assertThat(categoryState(EntryType.SPELL, spells, null, "boule de feu").groups.single().entries.map { it.id.value }).contains("spell.fireball")
        assertThat(categoryState(EntryType.SPELL, spells, null, "FIREBALL").groups.single().entries.map { it.id.value }).contains("spell.fireball")
        assertThat(categoryState(EntryType.SPELL, spells, null, "benediction").groups.single().entries.map { it.id.value }).contains("spell.bless")
        assertThat(categoryState(EntryType.SPELL, spells, null, "zzqx").groups).isEmpty()
    }

    @Test
    fun `sous-types dans l'ordre du modèle, et filtre par sous-type`() {
        val state = categoryState(EntryType.SPELL, list(EntryType.SPELL), null)
        assertThat(state.subtypes).isInOrder(compareBy<String> { s -> SpellSchool.entries.indexOfFirst { it.name == s } })
        val evocation = categoryState(EntryType.SPELL, list(EntryType.SPELL), null, subtype = SpellSchool.EVOCATION.name)
        assertThat(evocation.groups.single().entries.all { it.subtype == SpellSchool.EVOCATION.name }).isTrue()
    }

    @Test
    fun `sous-classes groupées par classe, sans étiquettes de filtre`() {
        val state = categoryState(EntryType.SUBCLASS, list(EntryType.SUBCLASS), list(EntryType.CLASS))
        assertThat(state.subtypes).isEmpty()
        assertThat(state.groups.map { it.title }).containsNoDuplicates()
        assertThat(state.groups.first { it.title == "Barbare" }.entries.all { it.subtype == "class.barbarian" }).isTrue()
        assertThat(state.groups.sumOf { it.entries.size }).isEqualTo(list(EntryType.SUBCLASS).visible.size)
    }
}
