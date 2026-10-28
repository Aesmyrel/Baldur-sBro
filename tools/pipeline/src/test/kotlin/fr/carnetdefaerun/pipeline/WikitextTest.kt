package fr.carnetdefaerun.pipeline

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.pipeline.wiki.Wikitext
import org.junit.Test

class WikitextTest {
    private val page = """
        {{hatnote|Voir aussi {{SAI|Autre}}.}}
        {{Feature page
        | type = spell
        | level = 3
        | classes = Sorcerer, Wizard
        | note = un [[Lien|libellé]] et {{DamageText|8d6|Fire}} <!-- commentaire | piège -->
        | positionnel
        }}
    """.trimIndent()

    @Test
    fun `le modèle principal est trouvé, quels que soient espaces et casse`() {
        val t = Wikitext.pageTemplate(page, "FeaturePage")!!
        assertThat(t["type"]).isEqualTo("spell")
        assertThat(t["level"]).isEqualTo("3")
        assertThat(t["classes"]).isEqualTo("Sorcerer, Wizard")
        assertThat(t.arg(0)).isEqualTo("positionnel")
    }

    @Test
    fun `les barres des liens, modèles imbriqués et commentaires ne coupent pas les paramètres`() {
        val note = Wikitext.pageTemplate(page, "Feature page")!!["note"]!!
        assertThat(note).isEqualTo("un [[Lien|libellé]] et {{DamageText|8d6|Fire}}")
        assertThat(Wikitext.plain(note)).isEqualTo("un libellé et 8d6")
    }

    @Test
    fun `liens et texte brut`() {
        assertThat(Wikitext.links("[[Defiled Temple]] et [[Underdark#Accès|l'Outreterre]]").map { it.target })
            .containsExactly("Defiled Temple", "Underdark").inOrder()
        assertThat(Wikitext.plain("Sold by {{CharLink|Grat|Grat the Trader}}")).isEqualTo("Sold by Grat the Trader")
        assertThat(Wikitext.plain("'''Gras''' et ''italique''")).isEqualTo("Gras et italique")
    }
}
