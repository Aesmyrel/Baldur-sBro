package fr.carnetdefaerun.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.carnetdefaerun.core.designsystem.theme.CarnetTheme
import fr.carnetdefaerun.core.designsystem.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Captures du catalogue : thèmes sombre, noir OLED et parchemin, police système à 200 %, largeur de pliable.
 * Enregistrer : ./gradlew :core:designsystem:recordRoborazziDebug ; vérifier : verifyRoborazziDebug.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi")
class DesignSystemScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, mode: ThemeMode = ThemeMode.SOMBRE, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                CarnetTheme(mode = mode, reducedMotion = true) { content() }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun accueil_sombre() = shot("accueil_sombre") { CatalogHome() }
    @Test fun accueil_oled() = shot("accueil_oled", ThemeMode.NOIR_OLED) { CatalogHome() }
    @Test fun accueil_parchemin() = shot("accueil_parchemin", ThemeMode.PARCHEMIN) { CatalogHome() }
    @Test fun lecture_sombre() = shot("lecture_sombre") { CatalogReading() }
    @Test fun lecture_parchemin() = shot("lecture_parchemin", ThemeMode.PARCHEMIN) { CatalogReading() }
    @Test fun lecture_police200() = shot("lecture_police200", fontScale = 2f) { CatalogReading() }
    @Test fun builds_sombre() = shot("builds_sombre") { CatalogBuild() }
    @Test fun builds_parchemin() = shot("builds_parchemin", ThemeMode.PARCHEMIN) { CatalogBuild() }
    @Test fun controles_sombre() = shot("controles_sombre") { CatalogControls() }
    @Test fun controles_police200() = shot("controles_police200", fontScale = 2f) { CatalogControls() }

    @Config(qualifiers = "w412dp-h1200dp-xxhdpi")
    @Test fun vignettes_sombre() = shot("vignettes_sombre") { CatalogVignettes() }

    @Config(qualifiers = "w412dp-h1200dp-xxhdpi")
    @Test fun vignettes_parchemin() = shot("vignettes_parchemin", ThemeMode.PARCHEMIN) { CatalogVignettes() }

    @Config(qualifiers = "w800dp-h840dp-xhdpi")
    @Test fun lecture_pliable() = shot("lecture_pliable") { CatalogReading() }
}
