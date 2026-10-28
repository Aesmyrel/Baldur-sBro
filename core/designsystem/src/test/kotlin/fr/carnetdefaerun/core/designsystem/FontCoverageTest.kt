package fr.carnetdefaerun.core.designsystem

import com.google.common.truth.Truth.assertWithMessage
import java.awt.Font
import java.io.File
import org.junit.Test

/**
 * Chaque police embarquée couvre la typographie française (espaces fines insécables, apostrophe,
 * guillemets), et aucune version modifiée ne porte un nom de police réservé (OFL §3).
 */
class FontCoverageTest {
    private val fonts = File("src/main/res/font").listFiles { f -> f.extension == "ttf" }.orEmpty().sortedBy { it.name }

    @Test
    fun `les caractères de la typographie française sont dessinés par nos polices`() {
        assertWithMessage("polices trouvées").that(fonts).isNotEmpty()
        for (file in fonts) {
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            for (ch in listOf(' ', ' ', '’', '«', '»', 'œ', 'É')) {
                assertWithMessage("%s : U+%s", file.name, "%04X".format(ch.code)).that(font.canDisplay(ch)).isTrue()
            }
        }
    }

    @Test
    fun `aucune police modifiée ne garde un nom réservé`() {
        for (file in fonts) {
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            assertWithMessage(file.name).that(font.getFamily(java.util.Locale.ROOT)).doesNotContain("Almendra")
            assertWithMessage(file.name).that(font.fontName).doesNotContain("Almendra")
        }
    }
}
