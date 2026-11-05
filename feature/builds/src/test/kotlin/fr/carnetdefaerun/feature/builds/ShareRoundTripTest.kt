package fr.carnetdefaerun.feature.builds

import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.model.builds.BuildCode
import org.junit.Test

/**
 * Critère de fin de la phase 4 : un build exporté puis importé revient identique. Chemin complet de l'image :
 * code de partage → QR code (tel que l'écran le dessine) → pixels → lecture ZXing → build.
 */
class ShareRoundTripTest {
    /** Rend la matrice comme l'écran : modules sombres sur fond clair, marge claire autour, [scale] pixels par module. */
    private fun render(text: String, scale: Int = 6, quiet: Int = 4): Triple<Int, Int, IntArray> {
        val matrix = QrCodes.encode(text)!!
        val side = (matrix.width + 2 * quiet) * scale
        val pixels = IntArray(side * side) { PAPER }
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (!matrix[x, y]) continue
            for (dy in 0 until scale) for (dx in 0 until scale) pixels[((y + quiet) * scale + dy) * side + (x + quiet) * scale + dx] = INK
        }
        return Triple(side, side, pixels)
    }

    @Test
    fun `aller-retour par le QR code, build identique`() {
        for (build in listOf(BuildFixtures.battleMage, BuildFixtures.pact)) {
            val (w, h, pixels) = render(BuildCode.encode(build))
            val read = QrCodes.decode(w, h, pixels)
            assertThat(read).isEqualTo(BuildCode.encode(build))
            assertThat(BuildCode.decode(read!!)).isEqualTo(build)
        }
    }

    @Test
    fun `une image sans QR code ne donne rien`() {
        assertThat(QrCodes.decode(64, 64, IntArray(64 * 64) { PAPER })).isNull()
    }

    private companion object {
        const val PAPER = 0xFFF4ECD8.toInt()
        const val INK = 0xFF1E1710.toInt()
    }
}
