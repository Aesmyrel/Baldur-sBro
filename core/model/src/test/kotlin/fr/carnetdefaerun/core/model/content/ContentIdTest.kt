package fr.carnetdefaerun.core.model.content

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentIdTest {
    @Test
    fun `identifiants valides`() {
        listOf("spell.fireball", "lore.deity.selune", "item.blood-of-lathander", "class.wizard.bladesinging")
            .forEach { assertThat(ContentId(it).value).isEqualTo(it) }
        assertThat(ContentId("lore.deity.selune").kind).isEqualTo("lore")
    }

    @Test
    fun `identifiants invalides`() {
        listOf("", "spell", "Spell.Fireball", "spell.fire ball", "spell..x", "spell.-x", "spell.x-", "sort.boule-de-feu.").forEach {
            assertThrows(IllegalArgumentException::class.java) { ContentId(it) }
        }
    }
}
