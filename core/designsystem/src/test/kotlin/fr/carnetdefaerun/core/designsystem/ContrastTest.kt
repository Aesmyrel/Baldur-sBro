package fr.carnetdefaerun.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertWithMessage
import fr.carnetdefaerun.core.designsystem.theme.CarnetColors
import fr.carnetdefaerun.core.designsystem.theme.CarnetPalettes
import fr.carnetdefaerun.core.designsystem.theme.GoldLeaf
import org.junit.Test

/**
 * Contrastes des jetons (charte : texte ≥ 4,5:1, contours et grands titres ≥ 3:1), mesurés sur chaque fond
 * de chaque thème. Un nouveau jeton porteur de texte s'ajoute ici.
 */
class ContrastTest {
    private val palettes = mapOf("encre" to CarnetPalettes.Ink, "oled" to CarnetPalettes.Oled, "parchemin" to CarnetPalettes.Parchment)

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun CarnetColors.grounds() = mapOf("bg" to bg, "surface" to surface, "raised" to raised, "navBg" to navBg)

    private fun CarnetColors.texts() = mapOf(
        "text" to text, "text2" to text2, "muted" to muted, "gold" to gold, "lapis" to lapis, "vermilion" to vermilion,
        "verdigris" to verdigris, "tyrian" to tyrian, "orpiment" to orpiment, "sepia" to sepia,
    )

    @Test
    fun `les jetons de texte atteignent 4,5 sur tous les fonds`() {
        for ((theme, c) in palettes) for ((t, fg) in c.texts()) for ((g, ground) in c.grounds()) {
            assertWithMessage("%s : %s sur %s", theme, t, g).that(ratio(fg, ground)).isAtLeast(4.5)
        }
    }

    @Test
    fun `l'or d'une option choisie reste lisible sur son lavis`() {
        for ((theme, c) in palettes) {
            assertWithMessage(theme).that(ratio(c.gold, c.selectedTint.compositeOver(c.surface))).isAtLeast(4.5)
            assertWithMessage(theme).that(ratio(c.gold, c.selectedTint.compositeOver(c.bg))).isAtLeast(4.5)
        }
    }

    @Test
    fun `les contours en or profond atteignent 3 sur la page`() {
        for ((theme, c) in palettes) for (ground in listOf(c.bg, c.surface)) {
            assertWithMessage(theme).that(ratio(c.goldDeep, ground)).isAtLeast(3.0)
        }
    }

    @Test
    fun `le texte sur la feuille d'or atteint 4,5 sur chaque teinte du dégradé de texte`() {
        for ((theme, c) in palettes) for (tint in listOf(GoldLeaf.highlight, GoldLeaf.mid, GoldLeaf.warm)) {
            assertWithMessage(theme).that(ratio(c.onGold, tint)).isAtLeast(4.5)
        }
    }
}
