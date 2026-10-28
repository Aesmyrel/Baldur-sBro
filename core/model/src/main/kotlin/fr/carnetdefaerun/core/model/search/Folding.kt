package fr.carnetdefaerun.core.model.search

import java.text.Normalizer

/**
 * Forme de comparaison d'un texte pour le tri et la recherche : minuscules, sans accents,
 * ligatures dépliées (œ → oe, æ → ae), apostrophes et ponctuation retirées, espaces simples.
 * « Lae’zel » et « laezel », « Œil » et « oeil » ont la même forme.
 */
object Folding {
    private val marks = Regex("\\p{M}+")
    private val apostrophes = Regex("['’‘`]")
    private val separators = Regex("[^a-z0-9]+")

    fun fold(text: String): String {
        val expanded = text.replace("œ", "oe").replace("Œ", "oe").replace("æ", "ae").replace("Æ", "ae").replace("ß", "ss")
        return Normalizer.normalize(expanded, Normalizer.Form.NFD)
            .replace(marks, "")
            .lowercase()
            .replace(apostrophes, "")
            .replace(separators, " ")
            .trim()
    }
}
