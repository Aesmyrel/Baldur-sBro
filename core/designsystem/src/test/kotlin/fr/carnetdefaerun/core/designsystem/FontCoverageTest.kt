package fr.carnetdefaerun.core.designsystem

import com.google.common.truth.Truth.assertWithMessage
import java.awt.Font
import java.awt.font.FontRenderContext
import java.io.File
import java.nio.ByteBuffer
import org.junit.Test

/**
 * Chaque police embarquée couvre la typographie française (espaces fines insécables, apostrophe,
 * guillemets), garde la notice de son autrice (OFL §2) et ne porte aucun nom de police réservé (OFL §3).
 */
class FontCoverageTest {
    private val fonts = File("src/main/res/font").listFiles { f -> f.extension == "ttf" }.orEmpty().sortedBy { it.name }

    /** Autrice ou auteurs cités par la licence de chaque famille (`assets/licences/polices`). */
    private val authors = listOf(
        "alegreya_sans" to "The Alegreya Sans Project Authors",
        "eb_garamond" to "The EB Garamond Project Authors",
        "titres" to "Ana Sanfelippo",
    )

    @Test
    fun `les caractères de la typographie française sont dessinés par nos polices`() {
        assertWithMessage("polices trouvées").that(fonts).isNotEmpty()
        for (file in fonts) {
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            for (ch in listOf(' ', ' ', '’', '«', '»', 'œ', 'É', '…')) {
                assertWithMessage("%s : U+%s", file.name, "%04X".format(ch.code)).that(font.canDisplay(ch)).isTrue()
            }
        }
    }

    @Test
    fun `l'espace fine insécable est plus étroite qu'une espace-mot`() {
        val frc = FontRenderContext(null, false, true)
        for (file in fonts) {
            val font = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(1000f)
            fun advance(text: String) = font.createGlyphVector(frc, text).getGlyphMetrics(0).advance
            assertWithMessage(file.name).that(advance(" ")).isLessThan(advance(" ") * 0.7f)
            assertWithMessage(file.name).that(advance(" ")).isGreaterThan(0f)
        }
    }

    @Test
    fun `la notice de copyright d'origine est conservée`() {
        for (file in fonts) {
            val names = nameTable(file.readBytes())
            val author = authors.first { file.name.startsWith(it.first) }.second
            assertWithMessage("%s : notice", file.name).that(names[0]).contains(author)
            // La déclaration du nom réservé fait partie de la notice : elle reste, même si la police est renommée.
            if (file.name.startsWith("titres")) assertWithMessage("%s : nom réservé déclaré", file.name).that(names[0]).contains("Almendra")
            names[13]?.let { assertWithMessage("%s : licence", file.name).that(it).contains("SIL Open Font License") }
            names[14]?.let { assertWithMessage("%s : adresse de la licence", file.name).that(it).containsMatch("scripts\\.sil\\.org/OFL|openfontlicense\\.org") }
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

    /** Table `name` d'une police TrueType : premier enregistrement Unicode ou Windows de chaque identifiant. */
    private fun nameTable(bytes: ByteArray): Map<Int, String> {
        val buf = ByteBuffer.wrap(bytes)
        val tables = buf.getShort(4).toInt()
        val offset = (0 until tables).map { 12 + it * 16 }.first { String(bytes, it, 4, Charsets.US_ASCII) == "name" }.let { buf.getInt(it + 8) }
        val count = buf.getShort(offset + 2).toInt() and 0xFFFF
        val strings = offset + (buf.getShort(offset + 4).toInt() and 0xFFFF)
        val result = mutableMapOf<Int, String>()
        for (i in 0 until count) {
            val r = offset + 6 + i * 12
            val platform = buf.getShort(r).toInt()
            val id = buf.getShort(r + 6).toInt() and 0xFFFF
            val length = buf.getShort(r + 8).toInt() and 0xFFFF
            val start = strings + (buf.getShort(r + 10).toInt() and 0xFFFF)
            if (platform == 0 || platform == 3) result.putIfAbsent(id, String(bytes, start, length, Charsets.UTF_16BE))
        }
        return result
    }
}
