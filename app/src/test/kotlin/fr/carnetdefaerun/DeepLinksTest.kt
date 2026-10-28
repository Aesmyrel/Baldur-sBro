package fr.carnetdefaerun

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.navigation.Destination
import fr.carnetdefaerun.navigation.DeepLinks
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeepLinksTest {
    @Test
    fun `liens reconnus`() {
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://fiche/spell.fireball"))).isEqualTo(Destination.GameEntry("spell.fireball"))
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://codex/lore.people.githyanki#s2")))
            .isEqualTo(Destination.LoreEntry("lore.people.githyanki", "s2"))
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://recherche?q=boule%20de%20feu"))).isEqualTo(Destination.Search("boule de feu"))
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://build/42"))).isEqualTo(Destination.BuildEditor(42))
    }

    @Test
    fun `liens refusés`() {
        assertThat(DeepLinks.parse(Uri.parse("https://example.com/fiche/spell.fireball"))).isNull()
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://fiche/Pas Un Id"))).isNull()
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://inconnu"))).isNull()
        assertThat(DeepLinks.parse(null)).isNull()
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://build/-3"))).isNull()
    }

    @Test
    fun `valeurs hostiles neutralisées`() {
        // Une ancre n'est qu'un numéro de section : tout autre texte est écarté.
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://codex/lore.people.githyanki#titre-secret")))
            .isEqualTo(Destination.LoreEntry("lore.people.githyanki", null))
        val long = "a".repeat(5_000)
        assertThat(DeepLinks.parse(Uri.parse("carnetdefaerun://recherche?q=$long"))).isEqualTo(Destination.Search("a".repeat(200)))
    }
}
