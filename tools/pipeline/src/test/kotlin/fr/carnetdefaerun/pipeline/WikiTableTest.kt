package fr.carnetdefaerun.pipeline

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.pipeline.wiki.WikiTables
import org.junit.Test

class WikiTableTest {
    // Forme des tableaux de progression de bg3.wiki : deux lignes d'en-tête, fusions verticales, modèles à barres.
    private val text = """
        Avant.
        {| class="wikitable" style="text-align: center;"
        |+ Légende
        |-
        ! colspan="3" | Progression
        ! colspan="2" | [[Spells#Spell Slots|Emplacements]]
        |-
        ! Level
        ! Proficiency Bonus
        ! Features
        ! 1st !! 2nd
        |-
        ! [[#Level 1|1st]]
        | rowspan="2" | +2
        | style="text-align: left;" | {{SAI|Arcane Recovery}}, {{SmIconLink|Icon.webp|Wizard#Level 1|Choose a subclass}}
        | 2
        | -
        |-
        ! [[#Level 2|2nd]]
        | {{SAI|Feats|Feat}}
        | 3 || -
        |-
        ! [[#Level 3|3rd]]
        | +3
        | -
        | rowspan="2" | 4
        | 2
        |-
        ! 4th
        | +3
        | texte sur
        deux lignes
        | 3
        |}
        Après.
    """.trimIndent()

    @Test
    fun `les fusions de cellules sont dépliées colonne par colonne`() {
        val table = WikiTables.parse(text).single()
        assertThat(table.caption).isEqualTo("Légende")
        val grid = table.grid()
        assertThat(grid.map { it.size }).containsExactly(5, 5, 5, 5, 5, 5).inOrder()
        assertThat(grid[1].map { it.text }).containsExactly("Level", "Proficiency Bonus", "Features", "1st", "2nd").inOrder()
        assertThat(grid[3].map { it.text }).containsExactly("[[#Level 2|2nd]]", "+2", "{{SAI|Feats|Feat}}", "3", "-").inOrder()
        assertThat(grid[5].map { it.text }).containsExactly("4th", "+3", "texte sur\ndeux lignes", "4", "3").inOrder()
    }

    @Test
    fun `les attributs sont séparés du contenu, jamais les barres d'un modèle ou d'un lien`() {
        val cell = WikiTables.parse(text).single().grid()[2][2]
        assertThat(cell.text).isEqualTo("{{SAI|Arcane Recovery}}, {{SmIconLink|Icon.webp|Wizard#Level 1|Choose a subclass}}")
        assertThat(WikiTables.parse(text).single().grid()[2][1].rowspan).isEqualTo(2)
        assertThat(WikiTables.parse(text).single().grid()[0][0].colspan).isEqualTo(3)
    }

    @Test
    fun `un tableau imbriqué reste dans le texte de sa cellule`() {
        val nested = "{|\n|-\n| a\n{|\n|-\n| b\n|}\n| c\n|}"
        val table = WikiTables.parse(nested).single()
        assertThat(table.grid().single().map { it.text.lines().first() }).containsExactly("a", "c").inOrder()
    }
}
