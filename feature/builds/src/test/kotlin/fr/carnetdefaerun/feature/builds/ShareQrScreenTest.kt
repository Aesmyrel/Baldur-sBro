package fr.carnetdefaerun.feature.builds

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import fr.carnetdefaerun.core.model.builds.BuildCode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Le QR code tel que l'écran de partage le dessine (taille, marge, couleurs de chaque thème) se relit et rend le build
 * identique : la chaîne complète du critère de fin de phase, sans rendu refait à part.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class ShareQrScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `le QR code affiché se relit, dans chaque thème`() {
        val catalog = BuildFixtures.catalog()
        var mode by mutableStateOf(ThemeMode.SOMBRE)
        val state = editorState(1L, BuildFixtures.battleMage, BuildFixtures.rules, catalog, listOf(1L))
        compose.setContent { CarnetTheme(mode = mode, reducedMotion = true) { BuildEditorScreen(state, onEdit = {}, onBack = {}, initialPanel = Panel.Share) } }
        for (theme in listOf(ThemeMode.SOMBRE, ThemeMode.NOIR_OLED, ThemeMode.PARCHEMIN)) {
            mode = theme
            compose.waitForIdle()
            val image = compose.onNodeWithContentDescription("QR code du build", substring = true).captureToImage().toPixelMap()
            val pixels = IntArray(image.width * image.height) { i -> image[i % image.width, i / image.width].let { c ->
                val a = (c.alpha * 255).toInt(); val r = (c.red * 255).toInt(); val g = (c.green * 255).toInt(); val b = (c.blue * 255).toInt()
                (a shl 24) or (r shl 16) or (g shl 8) or b
            } }
            val read = QrCodes.decode(image.width, image.height, pixels)
            assertThat(read).isEqualTo(BuildCode.encode(BuildFixtures.battleMage))
            assertThat(BuildCode.decode(read!!)).isEqualTo(BuildFixtures.battleMage)
        }
    }
}
